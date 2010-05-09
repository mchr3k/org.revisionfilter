package org.revisionfilter.eclemma;
import org.eclipse.jdt.core.IJavaElement;
import org.revisionfilter.utils.console.RevisionFilterConsoleFactory;
import org.revisionfilter.utils.rcs.CachedRevisionSystem;
import org.revisionfilter.utils.rcs.CachedRevisionSystem.RevisionSystem;
import org.revisionfilter.utils.rcs.svn.SVNRevisionSystem;

import com.mountainminds.eclemma.core.analysis.ICoverageFilter;

/**
 * @author mchr
 */
public class SvnMethodCoverageFilter implements ICoverageFilter
{
  private CachedRevisionSystem revisionChecker = null;

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
    return lines;
  }

}
