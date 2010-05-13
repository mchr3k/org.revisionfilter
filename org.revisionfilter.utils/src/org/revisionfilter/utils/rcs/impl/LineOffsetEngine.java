package org.revisionfilter.utils.rcs.impl;

import java.util.LinkedList;
import java.util.List;

/**
 * Logic for computing a map between string offset and line number 
 */
public class LineOffsetEngine
{
  /**
   * @param inputString
   * @return Map from string offset to line number
   */
  public static List<OffsetLineMapping> computeLineOffsets(String inputString)
  {
    List<OffsetLineMapping> lineOffsets = new LinkedList<OffsetLineMapping>();
    int lineOffset = 0;
    int lineNo = 1;
    lineOffsets.add(new OffsetLineMapping(lineOffset, lineNo));
    lineOffset = inputString.indexOf("\n",0);
    lineNo++;
    while (lineOffset > -1)
    {
      lineOffsets.add(new OffsetLineMapping(lineOffset + 1, lineNo));
      lineOffset = inputString.indexOf("\n",lineOffset + 1);
      lineNo++;
    }
    return lineOffsets;
  }
    
  public static class OffsetLineMapping
  {
    public OffsetLineMapping(int offset, int lineNo)
    {
      this.offset = offset;
      this.lineNo = lineNo;
    }
    public final int offset;
    public final int lineNo;
  }
}
