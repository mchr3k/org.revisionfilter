package de.tobject.findbugs.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Methods used to extend the behaviour of a FindBugsAction.
 */
public interface IFindBugsActionHelper {

	/**
	 * Perform a transformation on the selected elements. This will expected to be used to
	 * allow a subset of the selected elements to be chosen.
	 * <p>
	 * For example this method could expand a selected project and then exclude classes
	 * within a test package.
	 * 
	 * @param sSelection
	 *            The users selection.
	 * @return An iterator over the elements which should be analysed by FindBugs.
	 */
	public Iterator<?> transformSelection(IStructuredSelection sSelection);

	/**
	 * If transformSelection will take a long time it must be done asynchronously. This
	 * method should return the IProject to use in the FindBugs job mutex.
	 * 
	 * @param xiSSelection
	 *            The users selection.
	 * @return IProject to lock (see above) or null if the transformation will complete
	 *         quickly.
	 */
	public IProject getAsyncProject(IStructuredSelection xiSSelection);

	/**
	 * If transformSelection will take a long time it must be done asynchronously. This
	 * method should return the IProject to use in the FindBugs job mutex.
	 * 
	 * @param xiSSelection
	 *            The users selection.
	 * @return IProject to lock (see above) or null if the transformation will complete
	 *         quickly.
	 */
	public String getAsyncName(IStructuredSelection xiSSelection);

	/**
	 * @return An array of marker filter IDs which should be enabled before the FindBugs
	 *         analysis is performed. If no filters are to be enabled, an empty array
	 *         should be returned.
	 */
	public String[] getRequiredFilters();
}
