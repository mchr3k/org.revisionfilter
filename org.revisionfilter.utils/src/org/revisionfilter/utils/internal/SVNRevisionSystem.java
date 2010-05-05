package org.revisionfilter.utils.internal;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.revisionfilter.utils.IRevisionSystem;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

public class SVNRevisionSystem implements IRevisionSystem
{

  @Override
  public List<Integer> getDirtyLines(IFile file)
  {
    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNFileFor(file);    
    
    
    // Do line based diff
    
    return null;
  }

  @Override
  public boolean isDirty(IFile file)
  {
    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(file);
    boolean isDirty = false;
    try
    {
      isDirty = svnResource.isDirty() || 
                svnResource.isAdded() || 
                !svnResource.isManaged();
    } 
    catch (SVNException e)
    {
      e.printStackTrace();
    }
    
    return isDirty;
  }

}
