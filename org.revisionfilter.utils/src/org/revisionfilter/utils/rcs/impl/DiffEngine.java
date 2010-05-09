package org.revisionfilter.utils.rcs.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;

/**
 * Diff engine wrapping the google-diff-match-patch class
 */
public class DiffEngine
{
  /**
   * @param currentText
   * @param baseText
   * @return The new/changed lines of text.
   */
  public static Set<Integer> getDirtyLines(String currentText, String baseText)
  {
    Set<Integer> dirtyLines = new HashSet<Integer>();
    
    // Perform string diff
    diff_match_patch diffEngine = new diff_match_patch();
    LinkedList<Diff> fileDiffs = diffEngine.diff_main(currentText, baseText);
    diffEngine.diff_cleanupSemantic(fileDiffs);
    
    // Compute list of changed lines based on string diff
    boolean currentLineAdded = false;
    int currentLineNumber = 1;
    for (Diff diff : fileDiffs)
    {
      // Ignore deletions, process INSERT and EQUAL changes 
      if (Operation.DELETE != diff.operation)
      {
        if (!currentLineAdded && (Operation.INSERT == diff.operation))
        {
          dirtyLines.add(currentLineNumber);
          
          // Ensure that multiple diffs on the same line do not produce multiple "dirty lines"
          currentLineAdded = true;
        }
        
        String diffText = diff.text;
        int lastNewLineIndex = diffText.indexOf("\n",0);
        while (lastNewLineIndex > -1)
        {
          // Found a newline
          if (!currentLineAdded && (Operation.INSERT == diff.operation))
          {
            dirtyLines.add(currentLineNumber);
          }            
          
          currentLineNumber++;
          currentLineAdded = false;
          lastNewLineIndex = diffText.indexOf("\n",lastNewLineIndex + 1);
        }
      }
    }
    return dirtyLines;
  }
}
