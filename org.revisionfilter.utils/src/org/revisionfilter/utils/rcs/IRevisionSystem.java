package org.revisionfilter.utils.rcs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public interface IRevisionSystem
{
  public boolean isDirty(IResource resource, int flags);
  public String getBaseFileContents(IFile file);
}
