package org.test.revisionfilter.utils.impl;

import java.util.List;

import junit.framework.TestCase;

import org.revisionfilter.utils.rcs.impl.LineOffsetEngine;
import org.revisionfilter.utils.rcs.impl.LineOffsetEngine.OffsetLineMapping;

public class LineOffsetEngineTest extends TestCase
{
  public void testNoDiffs()
  {    
    // Multi line
    {
      String testStr = "01\n34\n67\n\n10";
      List<OffsetLineMapping> offsetList = LineOffsetEngine.computeLineOffsets(testStr);
      assertNotNull(offsetList);
      assertEquals(5, offsetList.size());
      assertEquals(offsetList.toString(), 0, offsetList.get(0).offset);
      assertEquals(offsetList.toString(), 3, offsetList.get(1).offset);
      assertEquals(offsetList.toString(), 6, offsetList.get(2).offset);
      assertEquals(offsetList.toString(), 9, offsetList.get(3).offset);
      assertEquals(offsetList.toString(), 10, offsetList.get(4).offset);
    }
  }

}
