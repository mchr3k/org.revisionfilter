package org.revisionfilter.utils;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public interface IRevisionSystem
{
  public boolean isDirty(IResource resource, int flags);
  public Set<Integer> getDirtyLines(IFile file);
}
