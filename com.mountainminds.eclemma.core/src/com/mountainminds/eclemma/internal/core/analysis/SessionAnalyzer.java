/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: SessionAnalyzer.java 473 2008-05-28 20:51:30Z mtnminds $
 ******************************************************************************/
package com.mountainminds.eclemma.internal.core.analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;

import com.mountainminds.eclemma.core.EclEmmaStatus;
import com.mountainminds.eclemma.core.ICoverageSession;
import com.mountainminds.eclemma.core.IInstrumentation;
import com.mountainminds.eclemma.core.analysis.IJavaModelCoverage;
import com.mountainminds.eclemma.internal.core.CoreMessages;
import com.mountainminds.eclemma.internal.core.DebugOptions;
import com.mountainminds.eclemma.internal.core.EclEmmaCorePlugin;
import com.mountainminds.eclemma.internal.core.DebugOptions.ITracer;
import com.mountainminds.eclemma.internal.core.analysis.TypeCoverage.UnboundMethodCoverage;
import com.vladium.emma.data.ClassDescriptor;
import com.vladium.emma.data.DataFactory;
import com.vladium.emma.data.ICoverageData;
import com.vladium.emma.data.IMergeable;
import com.vladium.emma.data.IMetaData;
import com.vladium.emma.data.MethodDescriptor;
import com.vladium.emma.data.ICoverageData.DataHolder;

/**
 * @author Marc R. Hoffmann
 * @version $Revision: 473 $
 */
public class SessionAnalyzer {

  private static final ITracer TRACER = DebugOptions.ANALYSISTRACER;
  private static final ITracer PERFORMANCE = DebugOptions.PERFORMANCETRACER;

  private JavaModelCoverage modelcoverage;

  public IJavaModelCoverage processSession(ICoverageSession session, IProgressMonitor monitor)
      throws CoreException {
    PERFORMANCE.startTimer();
    PERFORMANCE.startMemoryUsage();
    modelcoverage = new JavaModelCoverage();
    IPath[] coveragefiles = session.getCoverageDataFiles();
    IInstrumentation[] instrumentations = session.getInstrumentations();
    monitor.beginTask(NLS.bind(CoreMessages.AnalyzingCoverageSession_task, session.getDescription()),
        coveragefiles.length + instrumentations.length);
    ICoverageData coveragedata = null;
    for (int i = 0; i < coveragefiles.length && !monitor.isCanceled(); i++) {
      coveragedata = processCoveragedataFile(coveragedata, coveragefiles[i]);
      monitor.worked(1);
    }
    for (int i = 0; i < instrumentations.length && !monitor.isCanceled(); i++) {
      processMetadataFile(coveragedata, instrumentations[i],
          new SubProgressMonitor(monitor, 1));
    }
    monitor.done();
    PERFORMANCE.stopTimer("loading " + session.getDescription()); //$NON-NLS-1$
    PERFORMANCE.stopMemoryUsage("loading " + session.getDescription()); //$NON-NLS-1$
    return modelcoverage;
  }

  private ICoverageData processCoveragedataFile(ICoverageData coveragedata,
      IPath path) throws CoreException {
    try {
      File f = path.toFile();
      if (f.exists()) {
        IMergeable data = DataFactory.load(f)[DataFactory.TYPE_COVERAGEDATA];
        if (coveragedata == null) {
          coveragedata = (ICoverageData) data;
        } else {
          coveragedata = (ICoverageData) coveragedata.merge(data);
        }
      }
      return coveragedata;
    } catch (IOException e) {
      throw new CoreException(EclEmmaStatus.COVERAGEDATA_FILE_READ_ERROR
          .getStatus(path, e));
    }
  }

  private void processMetadataFile(ICoverageData coveragedata, IInstrumentation instrumentation,
      IProgressMonitor monitor) throws CoreException {
    IPath metadatafile = instrumentation.getMetaDataFile();
    File f = metadatafile.toFile();
    if (f.exists()) {
      IMetaData metadata;
      try {
        metadata = (IMetaData) DataFactory.load(f)[DataFactory.TYPE_METADATA];
      } catch (IOException e) {
        throw new CoreException(EclEmmaStatus.METADATA_FILE_READ_ERROR.getStatus(
            metadatafile, e));
      }
      if (metadata == null) {
        throw new CoreException(EclEmmaStatus.FILE_CONTAINS_NO_METADATA.getStatus(
            metadatafile));
      }
      IPackageFragmentRoot[] roots = instrumentation.getClassFiles().getPackageFragmentRoots();
      TypeTraverser jep = new TypeTraverser(roots);
      jep.process(new TypeVisitor(metadata, coveragedata), monitor);
    }
  }
  
  private class TypeVisitor implements ITypeVisitor {

    private final ICoverageData coveragedata;
    private final Map descriptors;

    TypeVisitor(IMetaData metadata, ICoverageData coveragedata) {
      this.coveragedata = coveragedata;
      this.descriptors = new HashMap();
      for (Iterator i = metadata.iterator(); i.hasNext();) {
        ClassDescriptor cd = (ClassDescriptor) i.next();
        descriptors.put(cd.getClassVMName(), cd);
      }
    }

    public void visit(IType type, String vmname) {
      ClassDescriptor descriptor = (ClassDescriptor) descriptors.remove(vmname);
      if (descriptor != null) {
        DataHolder data = coveragedata == null ? null : coveragedata.getCoverage(descriptor);
        if (data != null && data.m_stamp != descriptor.getStamp()) {
          TRACER.trace("Invalid meta data signature for {0}.", descriptor.getClassVMName()); //$NON-NLS-1$
        } else {
          // This call recurses up the hierarchy from the current Type up
          // to the package root setting up coverage objects
          TypeCoverage typecoverage = (TypeCoverage) getCoverage(type, descriptor.hasCompleteLineNumberInfo());
          
          // Check whether there are any active filters
          boolean activeFilters = EclEmmaCorePlugin.getInstance().getJavaCoverageLoader().coverageFiltersActive();
          
          // This adds all the child objects and sets up their associated
          // coverage objects
          IResource resource = type.getResource();
          typecoverage.addType(data != null);
          MethodDescriptor[] methods = descriptor.getMethods();                             
          boolean[][] covered = data == null ? null : data.m_coverage;
          
          if (!activeFilters)
          {
            // No active filters - revert to legacy lazy binding
            UnboundMethodCoverage[] ubcoverage = new UnboundMethodCoverage[methods.length];
            for (int i = 0; i < methods.length; i++) {
              JavaElementCoverage coverage = new JavaElementCoverage(typecoverage, methods[i].hasLineNumberInfo(), resource);
              processMethodCoverage(coverage, methods[i], covered == null ? null : covered[i], resource);
              ubcoverage[i] = new UnboundMethodCoverage(methods[i].getName(), methods[i].getDescriptor(), coverage);
            }
            typecoverage.setUnboundMethods(ubcoverage);
          }
          else
          {
            
            // Check whether any filters are interested in this Type          
            boolean typeFiltered = EclEmmaCorePlugin.getInstance().getJavaCoverageLoader().isElementFiltered(type, true);
            
            if (!typeFiltered)
            {
              try {
                MethodMatcher matcher = new MethodMatcher(type, 
                                                          covered,
                                                          methods);
                while(matcher.methodMatched())
                {
                  // Get selected objects
                  IJavaElement element = matcher.getMatchedElement();
                  MethodDescriptor mdescriptor = matcher.getMatchedDescriptor();
                  boolean[] mcoverage = matcher.getMatchedCoverageData();
                  
                  boolean methodFiltered = EclEmmaCorePlugin.getInstance().getJavaCoverageLoader().isElementFiltered(element, false);
                  
                  if (!methodFiltered)
                  {
                    // Setup coverage data
                    JavaElementCoverage elementCoverage = getCoverage(element,
                                                  mdescriptor.hasLineNumberInfo());
                    processMethodCoverage(elementCoverage, mdescriptor,
                                          mcoverage, resource);
                  }
                }              
              } catch (JavaModelException e) {
                IStatus status = new Status(IStatus.ERROR, EclEmmaCorePlugin.ID,
                           IStatus.ERROR,
                           "Exception examining methods within type: " + type,e); //$NON-NLS-1$
                EclEmmaCorePlugin.getInstance().getLog().log(status);
              } 
            }
          }
        }
      }
    }

    public void done() {
      // dump what's left
      for (Iterator i = descriptors.keySet().iterator(); i.hasNext();) {
        TRACER.trace("Instrumented type {0} has not been processed.", i.next()); //$NON-NLS-1$
      }
    }

  }
  
  private boolean isMethodCovered(boolean[] blocks) {
    for (int i = 0; blocks != null && i < blocks.length; i++) {
      if (blocks[i]) return true;
    }
    return false;
  }

  private void processMethodCoverage(JavaElementCoverage coverage,
      MethodDescriptor descriptor, boolean[] covered, IResource resource) {    
    coverage.addMethod(isMethodCovered(covered));
    IJavaElement element = coverage.getJavaElement(coverage);
    int[] blocksizes = descriptor.getBlockSizes();
    if (blocksizes != null) {
      int blockcount = blocksizes.length;
      int[][] blocklines = descriptor.getBlockMap();
      for (int i = 0; i < blockcount; i++) {
        
        // Apply any active line filters
        int[] filteredLines = EclEmmaCorePlugin.getInstance().getJavaCoverageLoader().getFilteredLines(blocklines == null ? null : blocklines[i], element, false);
        
        // Load the block
        coverage.addBlock(blocksizes[i], filteredLines, covered == null ? false : covered[i]);
      }
    }
  }

  private JavaElementCoverage getCoverage(IJavaElement element, boolean haslines) {
    if (element == null)
      return null;
    JavaElementCoverage c = (JavaElementCoverage) modelcoverage.getCoverageFor(element);
    if (c == null) {
      switch (element.getElementType()) {
      case IJavaElement.JAVA_MODEL:
        c = modelcoverage;
        break;
      case IJavaElement.JAVA_PROJECT:
      case IJavaElement.PACKAGE_FRAGMENT_ROOT:
      case IJavaElement.PACKAGE_FRAGMENT:
        c = new JavaElementCoverage(getCoverage(element.getParent(), false), false, element.getResource());
        break;
      case IJavaElement.TYPE:
        c = new TypeCoverage(getCoverage(element.getParent(), haslines), haslines, element.getResource());
        break;
      default:
        c = new JavaElementCoverage(getCoverage(element.getParent(), haslines), haslines, element.getResource());
        break;
    }
      modelcoverage.put(element, c);
    }
    return c;
  }

}
