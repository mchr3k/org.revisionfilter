package org.revisionfilter.utils.rcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.revisionfilter.utils.console.RevisionFilterConsoleFactory;
import org.revisionfilter.utils.rcs.impl.DiffEngine;
import org.revisionfilter.utils.rcs.impl.LineOffsetEngine;

/**
 * Wrap another revision control system and cache all results. The cache cannot
 * be reset, the caller must create a new instance.
 */
public class CachedLineChangeSystem
{
  private final IRevisionSystem system;
  private final Map<String, Set<Integer>> cachedDirtyLines = new ConcurrentHashMap<String, Set<Integer>>();
  private final Map<String, Map<Integer, Integer>> cachedLineOffsets = new ConcurrentHashMap<String, Map<Integer, Integer>>();

  /**
   * cTor for a specific type of backend
   * 
   * @param type
   */
  public CachedLineChangeSystem(IRevisionSystem wrappedSystem)
  {
    system = wrappedSystem;
  }

  public Set<Integer> getDirtyLines(IFile file)
  {
    Set<Integer> dirtyLines = new HashSet<Integer>();
    
    // Firewall args
    if ((file != null) && (file.getLocation() != null))
    {
      
      // Check cache
      String absoluteFileName = file.getLocation().toOSString();
      dirtyLines = cachedDirtyLines.get(absoluteFileName);
      if (dirtyLines == null)
      {
        // Compute dirty lines
        try
        {
          // Get source strings
          InputStream fileStream = file.getContents();          
          String fileString = getFileContents(fileStream);
          String baseFileString = system.getBaseFileContents(file);          
            
          // Compute dirty lines
          dirtyLines = DiffEngine.getDirtyLines(fileString, baseFileString);        
          
          cachedDirtyLines.put(absoluteFileName, dirtyLines);
        }
        catch (Exception ex)
        {
          // Throw away
          RevisionFilterConsoleFactory.outputLine(ex.toString());
          ex.printStackTrace();
        }
      }
    }
    return dirtyLines;
  }
  
  /**
   * @param file
   * @return Map from string offset to line number
   */
  public Map<Integer, Integer> getLineOffsets(IFile file)
  {
    Map<Integer, Integer> lineOffsets = new ConcurrentHashMap<Integer, Integer>();
    
    // Firewall args
    if ((file != null) && (file.getLocation() != null))
    {
      
      // Check cache
      String absoluteFileName = file.getLocation().toOSString();
      lineOffsets = cachedLineOffsets.get(absoluteFileName);
      if (lineOffsets == null)
      {
        // Computer line offsets
        try
        {
          // Get source string
          InputStream fileStream = file.getContents();          
          String fileString = getFileContents(fileStream);
          
          // Compute line offsets
          lineOffsets = LineOffsetEngine.computeLineOffsets(fileString);        
          
          cachedLineOffsets.put(absoluteFileName, lineOffsets);
        }
        catch (Exception ex)
        {
          // Throw away
          RevisionFilterConsoleFactory.outputLine(ex.toString());
          ex.printStackTrace();
        }
      }
    }
    return lineOffsets;
  }

  public static String getFileContents(InputStream fileStream) throws IOException
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
