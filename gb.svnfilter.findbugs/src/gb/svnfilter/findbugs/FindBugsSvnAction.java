package gb.svnfilter.findbugs;

import gb.svnutils.SvnDiffManager;
import gb.svnutils.SvnUtilsConsoleFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;

import de.tobject.findbugs.actions.FindBugsAction;
import de.tobject.findbugs.actions.IFindBugsActionHelper;

/**
 * Findbugs action that finds bugs only for files which have changed according
 * to SVN.
 */
public class FindBugsSvnAction extends FindBugsAction
{
  /**
   * cTor
   */
  public FindBugsSvnAction()
  {
    setActionHelper(new FindBugsActionSvnHelper());
  }

  private class FindBugsActionSvnHelper implements IFindBugsActionHelper
  {
    @Override
    public Iterator<?> transformSelection(IStructuredSelection xiSSelection) {
      SvnDiffManager diffManager = new SvnDiffManager();
      List<IJavaElement> selectedElements = new ArrayList<IJavaElement>();
      for (Iterator<?> iter = xiSSelection.iterator(); iter.hasNext();)
      {
        Object objElement = iter.next();

        // Only consider allowing Java Projects
        if (objElement instanceof IProject)
        {
          IProject project = (IProject)objElement;
          try
          {
            boolean javaNature = project.isNatureEnabled(JavaCore.NATURE_ID);
            if (javaNature)
            {
              IJavaProject javaProject = JavaCore.create(project);
              SvnUtilsConsoleFactory.outputLine("Finding changed files in project: " + project.getName());
              addChangedElements(javaProject, selectedElements, diffManager);
              SvnUtilsConsoleFactory.outputLine("Found changed files in project: " + project.getName());
            }
          } catch (CoreException e)
          {
            // Throw away
          }
        }
      }

      return selectedElements.iterator();
    }

    private void addChangedElements(IJavaElement javaElement,
        List<IJavaElement> selectedElements, SvnDiffManager diffManager)
    {
      // Consider adding Compilation Units
      if (javaElement.getElementType() == IJavaElement.COMPILATION_UNIT)
      {
        if (diffManager.isSVNDirty(javaElement))
        {
          selectedElements.add(javaElement);
        }
      }
      else
      {
        // Recurse into all other elements
        if (javaElement instanceof IParent)
        {
          IParent javaElementParent = (IParent)javaElement;
          try
          {
            for (IJavaElement child : javaElementParent.getChildren())
            {
              addChangedElements(child, selectedElements, diffManager);
            }
          } catch (JavaModelException e)
          {
            // Throw away the exception
          }
        }
      }
    }

    public IProject getAsyncProject(IStructuredSelection xiSSelection) {
      return getProject(xiSSelection);
    }

    public String getAsyncName(IStructuredSelection xiSSelection) {
      String lRet = "Finding SVN dirty files";
      IProject project = getProject(xiSSelection);

      if (project != null)
      {
        lRet += " in: " + project.getName();
      }

      return lRet;
    }


    public String[] getRequiredFilters() {
      return new String[] {SvnMarkerFilter.class.getCanonicalName()};
    }
  }
}
