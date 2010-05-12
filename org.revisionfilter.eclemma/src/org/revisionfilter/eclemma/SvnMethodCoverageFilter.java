package org.revisionfilter.eclemma;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.revisionfilter.utils.console.RevisionFilterConsoleFactory;
import org.revisionfilter.utils.rcs.CachedRevisionSystem;
import org.revisionfilter.utils.rcs.svn.SVNRevisionSystem;

import com.mountainminds.eclemma.core.analysis.ICoverageFilter;

/**
 * @author mchr
 */
public class SvnMethodCoverageFilter implements ICoverageFilter
{
  CachedRevisionSystem revisionChecker = null;

  @Override
  public String getName()
  {
    return "SVN Filter (Methods)";
  }

  @Override
  public int[] disabledModes()
  {
    return new int[] {INSTRUCTIONS, BLOCKS};
  }

  @Override
  public int preferredMode()
  {
    return LINES;
  }

  @Override
  public void resetFilter()
  {
    RevisionFilterConsoleFactory.outputLine("EclEmma Method filter reset");
    revisionChecker = new CachedRevisionSystem(new SVNRevisionSystem());
  }

  @Override
  public boolean isElementFiltered(IJavaElement element)
  {
    boolean filterElement = true;
    if (revisionChecker != null)
    {
      filterElement = !revisionChecker.isDirty(element.getResource(), 
                                                SVNRevisionSystem.DIRTY_ADDED | 
                                                SVNRevisionSystem.DIRTY_UNVERSIONED);
      if (!filterElement && (element instanceof ISourceReference))
      {
        ISourceReference sourceRef = (ISourceReference)element;
        IResource elementResource = element.getResource();
        try
        {
          if ((sourceRef != null) &&
              (sourceRef.getSourceRange() != null) &&
              (elementResource != null) && 
              (elementResource instanceof IFile))
          {
            ISourceRange sourceRange = sourceRef.getSourceRange();
            IFile elementFile = (IFile)elementResource;
            Set<Integer> dirtyLines = revisionChecker.getDirtyLines(elementFile);          
            for (Integer dirtyLine : dirtyLines)
            {
              if ((sourceRange.getOffset() <= dirtyLine.intValue()) &&
                  (dirtyLine.intValue() <= (sourceRange.getOffset() + sourceRange.getOffset())))
              {
                filterElement = false;
                break;
              }
            }
          }
        }
        catch (JavaModelException ex)
        {
          // Throw away
        }
      }
    }
    return filterElement;
  }

  @Override
  public int[] getFilteredLines(int[] lines, IJavaElement element) {
    return lines;
  }

}
