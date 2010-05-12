package org.test.revisionfilter.utils.impl;

import java.util.Map;

import junit.framework.TestCase;

import org.revisionfilter.utils.rcs.impl.LineOffsetEngine;

public class LineOffsetEngineTest extends TestCase
{
  public void testNoDiffs()
  {    
    // Multi line
    {
      String testStr = "01\n34\n67";
      Map<Integer, Integer> offsetMap = LineOffsetEngine.computeLineOffsets(testStr);
      assertNotNull(offsetMap);
      assertEquals(3, offsetMap.size());
      assertEquals(offsetMap.toString(), (Integer)1, offsetMap.get(0));
      assertEquals(offsetMap.toString(), (Integer)2, offsetMap.get(3));
      assertEquals(offsetMap.toString(), (Integer)3, offsetMap.get(6));
    }
  }

}
