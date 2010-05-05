package org.revisionfilter.utils;

import java.util.List;

import org.eclipse.core.resources.IFile;

public interface IRevisionSystem
{
  public boolean isDirty(IFile file);
  public List<Integer> getDirtyLines(IFile file);
}
