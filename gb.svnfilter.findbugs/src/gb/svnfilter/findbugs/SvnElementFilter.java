package gb.svnfilter.findbugs;

import gb.svnutils.SvnDiffManager;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * SVN Element filter
 */
public class SvnElementFilter extends ViewerFilter {

  @Override
  public boolean select(Viewer xiViewer,
                        Object xiParentElement,
                        Object xiElement) {
    boolean lRet = false;

    SvnDiffManager diffManager = new SvnDiffManager();

    // Only consider rejecting IJavaElements
    if (xiElement instanceof IJavaElement)
    {
      IJavaElement element = (IJavaElement)xiElement;

      // Cut off the hierarchy at the type level
      if (IJavaElement.TYPE != element.getElementType())
      {
        lRet = diffManager.isSVNDirty(element, true);
      }
    }
    else
    {
      lRet = true;
    }
    return lRet;
  }

}
