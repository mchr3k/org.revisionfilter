package gb.svnfilter.findbugs;

import gb.svnutils.SvnDiffManager;
import gb.svnutils.SvnUtilsConsoleFactory;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import de.tobject.findbugs.reporter.IMarkerFilter;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;

/**
 * Filter out markers which are present in clean code.
 */
public class SvnMarkerFilter implements IMarkerFilter {

  private SvnDiffManager diffManager = new SvnDiffManager();

  public String getFilterID()
  {
    return SvnMarkerFilter.class.getCanonicalName();
  }

  public String getName()
  {
    return "SVN Filter";
  }

  public boolean isFiltered(IProject project, BugInstance bugInstance) {

    List<? extends BugAnnotation> annotations = bugInstance.getAnnotations();

    IType element = null;
    for (BugAnnotation annot : annotations)
    {
      if (annot instanceof ClassAnnotation)
      {
        ClassAnnotation classAnnot = (ClassAnnotation)annot;
        String className = classAnnot.getClassName();
        if ((className != null) &&
            (className.indexOf("$") > -1))
        {
          // This line makes anonymous inner classes imprecise. Any changes to
          // the java file which contains an anonymous inner class will cause
          // all bugs in these inner classes to be included.
          className = className.substring(0, className.indexOf("$"));
        }
        IJavaProject jproject = JavaCore.create(project);
        try {
          element = jproject.findType(className);
          if (element != null) {
            // Bug is filtered if the class is clean
            if (!diffManager.isSVNDirty(element)) {
              return true;
            }
          }
        } catch (JavaModelException e) {
          // Throw away the exception
        }
      }
      else if ((annot instanceof MethodAnnotation) &&
        (element != null))
      {
        MethodAnnotation methodAnnot = (MethodAnnotation)annot;
        try {
          MethodLocator locator = new MethodLocator(element);
          IMethod method = locator.findMethod(methodAnnot.getMethodName(), methodAnnot.getMethodSignature());
          if (method != null) {
            // Bug is filtered if the method is clean
            if (!diffManager.isSVNDirty(method)) {
              return true;
            }
          }
        } catch (JavaModelException e) {
          // Throw away the exception
        }
      }
      else if ((annot instanceof SourceLineAnnotation) &&
          (element != null))
      {
        SourceLineAnnotation lineAnnot = (SourceLineAnnotation)annot;
        int startLine = lineAnnot.getStartLine();
        int endLine = lineAnnot.getEndLine();
        int numLines = startLine - endLine + 1;
        int[] lines = new int[numLines];
        for (int ii = 0; ii < lines.length; ii++)
        {
          lines[ii] = startLine + ii;
        }
        lines = diffManager.filterSVNCleanLines(lines,element);

        // Check for unfiltered lines
        for (int ii = 0; ii < lines.length; ii++)
        {
          // Unfiltered line - one of the bugged lines is dirty - don't filter
          // this bug annotation!
          if (lines[ii] != -1)
          {
            return false;
          }
        }

        // All lines were filtered - filter this bug annotation
        return true;
      }
    }
    return false;
  }

  public void resetFilter() {
    diffManager = new SvnDiffManager();
    SvnUtilsConsoleFactory.outputLine("FindBugs Marker filter reset");
  }

}