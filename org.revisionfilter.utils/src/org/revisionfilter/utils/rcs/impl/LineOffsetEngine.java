package org.revisionfilter.utils.rcs.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logic for computing a map between string offset and line number 
 */
public class LineOffsetEngine
{
  /**
   * @param inputString
   * @return Map from string offset to line number
   */
  public static Map<Integer, Integer> computeLineOffsets(String inputString)
  {
    Map<Integer, Integer> lineOffsets = new ConcurrentHashMap<Integer, Integer>();
    int lineOffset = 0;
    int lineNo = 1;
    lineOffsets.put(lineOffset, lineNo);
    lineOffset = inputString.indexOf("\n",0);
    lineNo++;
    while (lineOffset > -1)
    {
      lineOffsets.put(lineOffset + 1, lineNo);
      lineOffset = inputString.indexOf("\n",lineOffset + 1);
      lineNo++;
    }
    return lineOffsets;
  }
}
