package org.test.revisionfilter.utils.impl;

import java.util.Set;

import org.revisionfilter.utils.rcs.impl.DiffEngine;

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
    
    // Multi line diffs with multiline parts the same
    {
      String testStr = "foo foo \n " +
      		           "bar foo bar \n" + 
      		           "foo foo foo \n" +
      		           "bar bar bar";
      String testBaseStr = "foo bar \n " +
					       "bar foo bar \n" + 
					       "foo bar foo \n" +
					       "bar bar foo";
      Set<Integer> diffLines = DiffEngine.getDirtyLines(testStr, testBaseStr);
      assertNotNull(diffLines);
      assertEquals(3, diffLines.size());
      assertTrue(diffLines.contains(Integer.valueOf(1)));
      assertTrue(diffLines.contains(Integer.valueOf(3)));
      assertTrue(diffLines.contains(Integer.valueOf(4)));
    }
  }
}
