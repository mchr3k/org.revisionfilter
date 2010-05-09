package org.revisionfilter.eclemma;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.revisionfilter.utils.console.RevisionFilterConsoleFactory;
import org.revisionfilter.utils.rcs.CachedRevisionSystem;
import org.revisionfilter.utils.rcs.CachedRevisionSystem.RevisionSystem;
import org.revisionfilter.utils.rcs.svn.SVNRevisionSystem;

import com.mountainminds.eclemma.core.analysis.ICoverageFilter;

/**
 * @author mchr
 */
public class SvnLineCoverageFilter implements ICoverageFilter
{
  private CachedRevisionSystem revisionChecker = null;

  @Override
  public String getName()
  {
    return "SVN Filter (Lines)";
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
    RevisionFilterConsoleFactory.outputLine("EclEmma Line filter reset");
    revisionChecker = new CachedRevisionSystem(RevisionSystem.SVN);
  }

  @Override
  public boolean isElementFiltered(IJavaElement element)
  {
    boolean ret = false;
    if (revisionChecker != null)
    {
      ret = !revisionChecker.isDirty(element.getResource(), 
                                 SVNRevisionSystem.DIRTY_ADDED | 
                                 SVNRevisionSystem.DIRTY_UNVERSIONED);
    }
    return ret;
  }

  @Override
  public int[] getFilteredLines(int[] lines, IJavaElement element) {
    int[] ret = lines;
    if (revisionChecker != null)
    {      
      Set<Integer> dirtyLines = revisionChecker.getDirtyLines((IFile)element.getResource());
      
      // Set excluded lines to -1
      for (int ii = 0; ii < lines.length; ii++)
      {
        if (!dirtyLines.contains(Integer.valueOf(lines[ii])))
        {
          lines[ii] = -1;
        }
      }      
    }
    return ret;
  }

}
