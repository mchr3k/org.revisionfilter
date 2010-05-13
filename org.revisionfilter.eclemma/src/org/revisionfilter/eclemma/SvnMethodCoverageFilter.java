package org.revisionfilter.eclemma;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.revisionfilter.utils.console.RevisionFilterConsoleFactory;
import org.revisionfilter.utils.rcs.CachedLineChangeSystem;
import org.revisionfilter.utils.rcs.impl.LineOffsetEngine.OffsetLineMapping;
import org.revisionfilter.utils.rcs.svn.SVNRevisionSystem;

import com.mountainminds.eclemma.core.analysis.ICoverageFilter;

/**
 * @author mchr
 */
public class SvnMethodCoverageFilter implements ICoverageFilter
{
  CachedLineChangeSystem lineChecker = null;
  SVNRevisionSystem revisionChecker = null;

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
    RevisionFilterConsoleFactory.outputLine("EclEmma " + getName() + " filter reset");
    revisionChecker = new SVNRevisionSystem();
    lineChecker = new CachedLineChangeSystem(revisionChecker);
  }

  @Override
  public boolean isElementFiltered(IJavaElement element)
  {
    // Assume all elements are filtered
    boolean filterElement = true;
    if (revisionChecker != null)
    {
      filterElement = !revisionChecker.isDirty(element.getResource(), 
                                                SVNRevisionSystem.DIRTY_ADDED | 
                                                SVNRevisionSystem.DIRTY_UNVERSIONED);
      if (!filterElement && (element instanceof ISourceReference))
      {
        // Assume all source elements are filtered
        filterElement = true;
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
            Set<Integer> dirtyLines = lineChecker.getDirtyLines(elementFile);
            List<OffsetLineMapping> lineOffsets = lineChecker.getLineOffsets(elementFile);
            int sourceLineStart = getSourceLine(sourceRange.getOffset(), lineOffsets);
            int sourceLineEnd = getSourceLine(sourceRange.getOffset() + sourceRange.getLength(), lineOffsets);
            for (Integer dirtyLine : dirtyLines)
            {
              if ((sourceLineStart <= dirtyLine.intValue()) &&
                  (dirtyLine.intValue() <= sourceLineEnd))
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
          RevisionFilterConsoleFactory.outputLine(ex.toString());
          ex.printStackTrace();
        }
      }
    }
    return filterElement;
  }

  private int getSourceLine(int offset, List<OffsetLineMapping> lineOffsets)
  {
    int latestLine = 1;
    for (OffsetLineMapping mapping : lineOffsets)
    {
      latestLine = mapping.lineNo;
      if (mapping.offset > offset)
      {
        return (mapping.lineNo - 1);
      }
    }
    return latestLine;
  }

  @Override
  public int[] getFilteredLines(int[] lines, IJavaElement element) {
    return lines;
  }

}
