package de.tobject.findbugs.actions;

import org.eclipse.jface.action.Action;

import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.view.explorer.IFilterGui;

/**
 * GUI Action used to toggle whether a filter is active or not.
 */
public class ToggleExtensionFilterAction extends Action implements IFilterGui {

	private boolean filterEnabled = false;
	private final String mFilterID;

	/**
	 * cTor
	 * @param xiFilterID
	 */
	public ToggleExtensionFilterAction(String xiFilterID) {
		super(MarkerUtil.getFilterName(xiFilterID), AS_CHECK_BOX);
		MarkerUtil.setFilterGui(xiFilterID, this);
		setChecked(MarkerUtil.isFilterEnabled(xiFilterID));		
		mFilterID = xiFilterID;
	}

	@Override
	public void run() {
		filterEnabled = isChecked();
		if (filterEnabled) {
			MarkerUtil.enableFilters(new String[] {mFilterID});
		} else {
			MarkerUtil.disableFilters(new String[] {mFilterID});
		}
		MarkerUtil.redisplayAllMarkers();
	}
}
