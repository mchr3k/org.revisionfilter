package org.test.revisionfilter.utils.impl;

import java.util.Set;

import org.revisionfilter.utils.impl.DiffEngine;

import junit.framework.TestCase;

public class DiffEngineTest extends TestCase
{
  public void testNoDiffs()
  {
    // Single line
    {
      String testStr = "foo bar";
      String testBaseStr = testStr;
      Set<Integer> diffLines = DiffEngine.getDirtyLines(testStr, testBaseStr);
      assertNotNull(diffLines);
      assertEquals(0, diffLines.size());
    }
    
    // Multi line
    {
      String testStr = "foo \n bar";
      String testBaseStr = testStr;
      Set<Integer> diffLines = DiffEngine.getDirtyLines(testStr, testBaseStr);
      assertNotNull(diffLines);
      assertEquals(0, diffLines.size());
    }
  }
  
  public void testDiffs()
  {
    // Single line
    {
      String testStr = "foo bar";
      String testBaseStr = "foo foo";
      Set<Integer> diffLines = DiffEngine.getDirtyLines(testStr, testBaseStr);
      assertNotNull(diffLines);
      assertEquals(1, diffLines.size());
      assertTrue(diffLines.contains(Integer.valueOf(1)));
    }
    
    // Multi line
    {
      String testStr = "foo \n bar";
      String testBaseStr = "foo \n foo";
      Set<Integer> diffLines = DiffEngine.getDirtyLines(testStr, testBaseStr);
      assertNotNull(diffLines);
      assertEquals(1, diffLines.size());
      assertTrue(diffLines.contains(Integer.valueOf(2)));
    }
  }
}
