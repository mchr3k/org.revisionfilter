package org.revisionfilter.utils;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.revisionfilter.utils.svn.SVNRevisionSystem;

public class RevisionChecker implements IRevisionSystem
{
  private final IRevisionSystem system;

  public static enum RevisionSystem
  {
    SVN
  }
  
  public RevisionChecker(RevisionSystem type)
  {
    system = new SVNRevisionSystem();    
  }

  @Override
  public List<Integer> getDirtyLines(IFile file)
  {
    return system.getDirtyLines(file);
  }

  @Override
  public boolean isDirty(IFile file)
  {
    return system.isDirty(file);
  }
  
  
}
