/*******************************************************************************
 * Copyright (c) 2006 Mountainminds GmbH & Co. KG
 * This software is provided under the terms of the Eclipse Public License v1.0
 * See http://www.eclipse.org/legal/epl-v10.html.
 *
 * $Id: JavaCoverageLoader.java 199 2006-12-18 14:49:40Z mtnminds $
 ******************************************************************************/
package com.mountainminds.eclemma.internal.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.osgi.util.NLS;

import com.mountainminds.eclemma.core.ICoverageSession;
import com.mountainminds.eclemma.core.ISessionListener;
import com.mountainminds.eclemma.core.ISessionManager;
import com.mountainminds.eclemma.core.analysis.ICoverageFilter;
import com.mountainminds.eclemma.core.analysis.IJavaCoverageListener;
import com.mountainminds.eclemma.core.analysis.IJavaModelCoverage;
import com.mountainminds.eclemma.internal.core.analysis.SessionAnalyzer;

/**
 * Internal utility class that loads the coverage data asynchronously, holds the
 * current {@link IJavaModelCoverage} object and sends out events in case of
 * changed coverage information.
 * 
 * @author  Marc R. Hoffmann
 * @version $Revision: 199 $
 */
public class JavaCoverageLoader {

  private final ISessionManager sessionManager;

  private ICoverageSession activeSession;

  private IJavaModelCoverage coverage;

  private final List listeners = new ArrayList();
  
  /**
   * List containing all active {@link ICoverageFilter} instances. The list is
   * never null but is normally empty.
   */
  private final List coverageFilters = new ArrayList();

  private ISessionListener sessionListener = new ISessionListener() {

    public void sessionActivated(ICoverageSession session) {
      activeSession = session;
      // TODO Looks like this has no effect
      Platform.getJobManager().cancel(LOADJOB);
      if (session == null) {
        coverage = null;
        fireCoverageChanged();
      } else {
        coverage = IJavaModelCoverage.LOADING;
        fireCoverageChanged();
        new LoadSessionJob(activeSession).schedule();
      }
    }

    public void sessionAdded(ICoverageSession addedSession) {
    }

    public void sessionRemoved(ICoverageSession removedSession) {
    }

  };

  private static final Object LOADJOB = new Object();
  
  private class LoadSessionJob extends Job {

    private final ICoverageSession session;

    public LoadSessionJob(ICoverageSession session) {
      super(NLS.bind(CoreMessages.AnalyzingCoverageSession_task, session.getDescription()));
      this.session = session;
    }

    protected IStatus run(IProgressMonitor monitor) {
      
      // Before we process the session we must reset all filters
      synchronized (coverageFilters) {
        Iterator coverageFiltersIter = coverageFilters.iterator();
        while (coverageFiltersIter.hasNext())
        {
          ((ICoverageFilter)coverageFiltersIter.next()).resetFilter();
        }
      }
      
      IJavaModelCoverage c;
      try {
        c = new SessionAnalyzer().processSession(session, monitor);
      } catch (CoreException e) {
        return e.getStatus();
      }
      coverage = monitor.isCanceled() ? null : c;
      fireCoverageChanged();
      return Status.OK_STATUS;
    }

    public boolean belongsTo(Object family) {
      return family == LOADJOB;
    }

  };

  public JavaCoverageLoader(ISessionManager sessionManager) {
    this.sessionManager = sessionManager;
    sessionManager.addSessionListener(sessionListener);
  }

  public void addJavaCoverageListener(IJavaCoverageListener l) {
    if (l == null)
      throw new NullPointerException();
    if (!listeners.contains(l)) {
      listeners.add(l);
    }
  }

  public void removeJavaCoverageListener(IJavaCoverageListener l) {
    listeners.remove(l);
  }
  
  /**
   * @param filter Filter to add to list of active filters.
   */
  public void addCoverageFilter(ICoverageFilter filter) {
    if (filter == null)
      throw new NullPointerException();
    synchronized (coverageFilters) {
      if (!coverageFilters.contains(filter)) {
        coverageFilters.add(filter);  
      }    
    }
  }

  /**
   * @param filter Filter to remove from list of active filters.
   */
  public void removeCoverageFilter(ICoverageFilter filter) {
    if (filter == null)
      throw new NullPointerException();
    synchronized (coverageFilters) {
      coverageFilters.remove(filter); 
    }    
  }
  
  /**
   * @return If some filters are active
   */
  public boolean coverageFiltersActive() {
    return (coverageFilters.size() > 0);
  }
  
  /**
   * @param element Element to test.
   * @param defaultRet The value to return if no filters are present.
   * @return True if the element should be excluded from the coverage view.
   */
  public boolean isElementFiltered(IJavaElement element, boolean defaultRet) {
    
    boolean isElementFiltered = defaultRet;
    
    synchronized (coverageFilters) {    
      // Consult all the load helpers      
      if ((coverageFilters.size() > 0) &&
          (element != null))
      {
        isElementFiltered = false;
        Iterator iter = coverageFilters.iterator();
        while (iter.hasNext())
        {          
          ICoverageFilter loadHelper = (ICoverageFilter)iter.next();
          try
          {
            isElementFiltered |= loadHelper.isElementFiltered(element);
          }
          catch (Throwable th)
          {
            IStatus status = new Status(IStatus.ERROR, EclEmmaCorePlugin.ID,
                IStatus.ERROR,
                "Uncaught exception when invoking Coverage Filer: " +  //$NON-NLS-1$
                loadHelper.getName(), th);
            EclEmmaCorePlugin.getInstance().getLog().log(status);
          }
        }
      }
    }
    return isElementFiltered;
  }

  public int[] getFilteredLines(int[] lines, IJavaElement element, boolean defaultRet) {
    int[] filteredLines = lines;
    
    synchronized (coverageFilters) {    
      // Consult all the load helpers      
      if ((coverageFilters.size() > 0) &&
          (element != null))
      {
        Iterator iter = coverageFilters.iterator();
        while (iter.hasNext())
        {          
          ICoverageFilter loadHelper = (ICoverageFilter)iter.next();
          try
          {
            filteredLines = loadHelper.getFilteredLines(filteredLines, element);
          }
          catch (Throwable th)
          {
            IStatus status = new Status(IStatus.ERROR, EclEmmaCorePlugin.ID,
                IStatus.ERROR,
                "Uncaught exception when invoking Coverage Filer: " +  //$NON-NLS-1$
                loadHelper.getName(), th);
            EclEmmaCorePlugin.getInstance().getLog().log(status);
          }
        }
      }
    }
    return filteredLines;
  }

  protected void fireCoverageChanged() {
    // avoid concurrent modification issues
    Iterator i = new ArrayList(listeners).iterator();
    while (i.hasNext()) {
      ((IJavaCoverageListener) i.next()).coverageChanged();
    }
  }

  public IJavaModelCoverage getJavaModelCoverage() {
    return coverage;
  }

  public void dispose() {
    sessionManager.removeSessionListener(sessionListener);
  }

}
