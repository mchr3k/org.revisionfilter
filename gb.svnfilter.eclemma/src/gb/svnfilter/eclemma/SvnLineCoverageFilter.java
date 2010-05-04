package gb.svnfilter.eclemma;
import gb.svnutils.SvnDiffManager;
import gb.svnutils.SvnUtilsConsoleFactory;

import org.eclipse.jdt.core.IJavaElement;

import com.mountainminds.eclemma.core.analysis.ICoverageFilter;

/**
 * @author mchr
 */
public class SvnLineCoverageFilter implements ICoverageFilter
{
  /** SVN Diff manager **/
  private SvnDiffManager diffManager = null;

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
    diffManager = new SvnDiffManager();
  }

  @Override
  public boolean isElementFiltered(IJavaElement element)
  {
    boolean ret = false;
    if (diffManager != null)
    {
      ret = !diffManager.isSVNDirty(element);
    }
    return ret;
  }

  @Override
  public int[] getFilteredLines(int[] lines, IJavaElement element) {
    int[] ret = lines;
    if (diffManager != null)
    {
      ret = diffManager.filterSVNCleanLines(lines, element);
    }
    return ret;
  }

}
