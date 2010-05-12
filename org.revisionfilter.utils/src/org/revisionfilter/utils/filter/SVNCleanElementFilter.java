package org.revisionfilter.utils.filter;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
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
    boolean showElement = true;

    SVNRevisionSystem revisionSystem = new SVNRevisionSystem();

    IResource resource = null;
    if (xiElement instanceof IJavaElement)
    {
      IJavaElement element = (IJavaElement)xiElement;
      resource = element.getResource();
    }
    else if (xiElement instanceof IResource)
    {
      resource = (IResource)xiElement;
    }
    
    if (resource != null)
    {
      showElement = revisionSystem.isDirty(resource, 
                                        SVNRevisionSystem.DIRTY_ADDED);
    }
    
    return showElement;
  }
}
