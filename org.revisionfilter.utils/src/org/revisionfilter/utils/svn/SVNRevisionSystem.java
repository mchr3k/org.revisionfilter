package org.revisionfilter.utils.svn;

import java.io.InputStream;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
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
    
    try
    {
      InputStream fileContents = file.getContents();
      InputStream baseFileContents = svnResource.getBaseResource().
                                                 getStorage(new NullProgressMonitor()).
                                                 getContents();
            
      diff_match_patch diffEngine = new diff_match_patch();
      // Do line based diff
    } 
    catch (CoreException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
    
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
