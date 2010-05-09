package org.revisionfilter.utils.rcs.svn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.revisionfilter.utils.rcs.IRevisionSystem;
import org.revisionfilter.utils.rcs.impl.DiffEngine;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

public class SVNRevisionSystem implements IRevisionSystem
{
  public static final int DIRTY_ADDED = 0x1;
  public static final int DIRTY_UNVERSIONED = 0x2;
  
  @Override
  public boolean isDirty(IResource resource, int flags)
  {
    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
    boolean isDirty = false;
    try
    {
      isDirty = svnResource.isDirty() || 
                (isSet(flags, DIRTY_ADDED) && svnResource.isAdded()) || 
                (isSet(flags, DIRTY_UNVERSIONED) && !svnResource.isManaged());
    } 
    catch (SVNException e)
    {
      e.printStackTrace();
    }
    
    return isDirty;
  }

  private boolean isSet(int flags, int flag)
  {
    return ((flags & flag) > 0);
  }

  @Override
  public Set<Integer> getDirtyLines(IFile file)
  {
    Set<Integer> dirtyLines = new HashSet<Integer>();
    
    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNFileFor(file);   
    try
    {
      // Get InputStreams of the file data
      InputStream fileStream = file.getContents();
      InputStream baseFileStream = svnResource.getBaseResource().
                                                 getStorage(new NullProgressMonitor()).
                                                 getContents();
      
      // Extract string contents
      String fileString = getFileContents(fileStream);
      String baseFileString = getFileContents(baseFileStream);
      
      // Compute the set of dirty lines
      dirtyLines = DiffEngine.getDirtyLines(fileString, baseFileString);
    } 
    catch (CoreException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } 
    
    return dirtyLines;
  }

  private String getFileContents(InputStream fileStream) throws IOException
  {
    StringBuffer fileString = new StringBuffer();
    BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileStream));
    String fileLine;
    while ((fileLine = fileReader.readLine()) != null)
    {
      fileString.append(fileLine + "\n");
    }
    return fileString.toString();
  }

}
