package org.revisionfilter.eclemma;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;

/**
 * @author mchr
 */
public class SvnLineCoverageFilter extends SvnMethodCoverageFilter
{
  @Override
  public String getName()
  {
    return "SVN Filter (Lines)";
  }

  @Override
  public int[] getFilteredLines(int[] lines, IJavaElement element) {
    int[] ret = lines;
    if (revisionChecker != null)
    {      
      IResource elementResource = element.getResource();
      if ((elementResource != null) && (elementResource instanceof IFile))
      {
        Set<Integer> dirtyLines = lineChecker.getDirtyLines((IFile)elementResource);
        
        // Set excluded lines to -1
        for (int ii = 0; ii < lines.length; ii++)
        {
          if (!dirtyLines.contains(Integer.valueOf(lines[ii])))
          {
            lines[ii] = -1;
          }
        }
      }
    }
    return ret;
  }

}
