package org.revisionfilter.utils.rcs.svn;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.revisionfilter.utils.console.RevisionFilterConsoleFactory;
import org.revisionfilter.utils.rcs.CachedLineChangeSystem;
import org.revisionfilter.utils.rcs.IRevisionSystem;
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
    ISVNLocalResource svnResource = SVNWorkspaceRoot
        .getSVNResourceFor(resource);
    boolean isDirty = false;
    try
    {
      isDirty = svnResource.isDirty()
          || (isSet(flags, DIRTY_ADDED) && svnResource.isAdded())
          || (isSet(flags, DIRTY_UNVERSIONED) && !svnResource.isManaged());
    } catch (SVNException e)
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
  public String getBaseFileContents(IFile file)
  {
    String baseFileContents = "";

    if (file != null)
    {
      ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNFileFor(file);

      try
      {
        if ((svnResource != null) && (svnResource.getBaseResource() != null))
        {

          // Get InputStreams of the file data
          InputStream baseFileStream = svnResource.getBaseResource()
              .getStorage(new NullProgressMonitor()).getContents();

          // Extract string contents
          baseFileContents = CachedLineChangeSystem
              .getFileContents(baseFileStream);
        }
      } 
      catch (Exception ex)
      {
        // Throw away
        RevisionFilterConsoleFactory.outputLine(ex.toString());
        ex.printStackTrace();
      }
    }

    return baseFileContents;
  }

}
