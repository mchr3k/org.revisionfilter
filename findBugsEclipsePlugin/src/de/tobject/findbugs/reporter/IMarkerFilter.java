package de.tobject.findbugs.reporter;

import org.eclipse.core.resources.IProject;

import edu.umd.cs.findbugs.BugInstance;

/**
 * Interface for code which filters bug markers
 */
public interface IMarkerFilter {

	/**
	 * @return A unique ID for this filter.
	 */
	String getFilterID();
	
	/**
	 * @return Name of the filter.
	 */
	String getName();
	
	/**
	 * Called before each round of marker processing
	 */
	void resetFilter();
	
	/**
	 * @param project Project which the bug is within.
	 * @param bugInstance Bug to potentially filter.
	 * @return true if the given BugInstance is filtered.
	 */
	boolean isFiltered(IProject project, BugInstance bugInstance);
}
