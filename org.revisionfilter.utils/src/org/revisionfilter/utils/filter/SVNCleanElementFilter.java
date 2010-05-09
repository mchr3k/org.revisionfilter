package org.revisionfilter.utils.filter;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.revisionfilter.utils.rcs.svn.SVNRevisionSystem;

/**
 * Filter for excluding all clean elements
 */
public class SVNCleanElementFilter extends ViewerFilter 
{
  @Override
  public boolean select(Viewer xiViewer,
                        Object xiParentElement,
                        Object xiElement) {
    boolean hideElement = false;

    SVNRevisionSystem diffManager = new SVNRevisionSystem();

    if (xiElement instanceof IResource)
    {
      IResource element = (IResource)xiElement;
      hideElement = !diffManager.isDirty(element, 
                                         SVNRevisionSystem.DIRTY_ADDED | 
                                         SVNRevisionSystem.DIRTY_UNVERSIONED);
    }
    else
    {
      hideElement = true;
    }
    return hideElement;
  }
}
