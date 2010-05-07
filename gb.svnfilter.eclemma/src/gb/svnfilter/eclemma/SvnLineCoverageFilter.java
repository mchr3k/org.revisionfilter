package gb.svnfilter.eclemma;
import gb.svnutils.SvnUtilsConsoleFactory;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.revisionfilter.utils.RevisionChecker;
import org.revisionfilter.utils.rcs.svn.SVNRevisionSystem;

import com.mountainminds.eclemma.core.analysis.ICoverageFilter;

/**
 * @author mchr
 */
public class SvnLineCoverageFilter implements ICoverageFilter
{
  private RevisionChecker revisionChecker = null;

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
    SvnUtilsConsoleFactory.outputLine("EclEmma Line filter reset");
    revisionChecker = new RevisionChecker();
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
      
      // Work out how many coverage lines are dirty
      int numDirtyLines = 0;
      for (int lineNo : lines)
      {
        if (dirtyLines.contains(Integer.valueOf(lineNo)))
        {
          numDirtyLines++;
        }
      }
      
      // Generate a new coverage array without the clean lines
      int index = 0;
      ret = new int[numDirtyLines];
      for (int lineNo : lines)
      {
        if (dirtyLines.contains(Integer.valueOf(lineNo)))
        {
          ret[index] = lineNo;
          index++;
        }
      }
    }
    return ret;
  }

}
