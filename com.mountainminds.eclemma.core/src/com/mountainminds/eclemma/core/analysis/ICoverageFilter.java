package com.mountainminds.eclemma.core.analysis;

import org.eclipse.jdt.core.IJavaElement;

/**
 * Interface for 3rd party code to decide which parts of Java Coverage
 * should be included.
 */
public interface ICoverageFilter {
  
  // Coverage mode constants
  final int INSTRUCTIONS = 0;
  final int BLOCKS = 1;
  final int LINES = 2;
  final int METHODS = 3;
  final int TYPES = 4;
  
  // Setup functions
  
  /**
   * @return the preferred coverage mode which will be enabled when this filter 
   * is enabled. 
   */
  int preferredMode();
  
  /**
   * @return int[] array of coverage mode constants corresponding to the
   * coverage modes which are disabled.
   */
  int[] disabledModes();
  
  /**
   * @return Name of the filter.
   */
  String getName();
    
  // Filter functions
  
  /**
   * Reset this filter.
   */
  void resetFilter();
  
  /**
   * @param element Element to test.
   * @return True if the element should be excluded from the coverage view.
   */
  boolean isElementFiltered(IJavaElement element);

  /**
   * @param xiFilteredLines
   * @param xiElement
   * @return xiFilteredLines with any filtered lines set to -1
   */
  int[] getFilteredLines(int[] xiFilteredLines, IJavaElement xiElement);
  
}
