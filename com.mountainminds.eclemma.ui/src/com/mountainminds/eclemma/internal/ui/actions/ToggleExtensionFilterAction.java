package com.mountainminds.eclemma.internal.ui.actions;

import org.eclipse.jface.action.Action;

import com.mountainminds.eclemma.core.CoverageTools;
import com.mountainminds.eclemma.core.ISessionManager;
import com.mountainminds.eclemma.core.analysis.ICoverageFilter;
import com.mountainminds.eclemma.internal.ui.coverageview.CoverageView;
import com.mountainminds.eclemma.internal.ui.coverageview.SelectCounterModeAction;
import com.mountainminds.eclemma.internal.ui.coverageview.ViewSettings;

/**
 * GUI Action used to toggle whether a filter is active or not.
 */
public class ToggleExtensionFilterAction extends Action {

  private boolean filterEnabled = false;
  private final ICoverageFilter mFilter;
  private final SelectCounterModeAction[] mCoverageModes;
  private final ViewSettings mSettings;
  private final CoverageView mView;

  public ToggleExtensionFilterAction(ICoverageFilter filter, CoverageView view,
      ViewSettings settings, SelectCounterModeAction[] otherModes) {
    super(filter.getName(), AS_CHECK_BOX);
    setChecked(filterEnabled);
    mFilter = filter;
    mSettings = settings;
    mCoverageModes = otherModes;
    mView = view;
  }

  public void run() {
    filterEnabled = isChecked();
    if (filterEnabled) {
      // Enable the filter

      // Uncheck all modes
      for (int ii = 0; ii < mCoverageModes.length; ii++) {
        mCoverageModes[ii].setChecked(false);
      }

      // Set the preferred mode
      mCoverageModes[mFilter.preferredMode()].setChecked(true);
      mSettings.setCounterMode(mFilter.preferredMode());
      mView.updateColumnHeaders();

      // Disable all disabled modes
      int[] disabledModes = mFilter.disabledModes();
      for (int ii = 0; ii < disabledModes.length; ii++) {
        mCoverageModes[disabledModes[ii]].setEnabled(false);
      }

      // Setup the filter
      mFilter.resetFilter();
      CoverageTools.addCoverageFilter(mFilter);
    } else {
      // Disable the filter
      CoverageTools.removeCoverageFilter(mFilter);

      // Check the Coverage Filters state
      if (!CoverageTools.coverageFiltersActive()) {
        // Only revert the UI if there are no filters left
        // Enabled all previously disabled modes
        int[] disabledModes = mFilter.disabledModes();
        for (int ii = 0; ii < disabledModes.length; ii++) {
          mCoverageModes[disabledModes[ii]].setEnabled(true);
        }
      }
    }
    ISessionManager manager = CoverageTools.getSessionManager();
    manager.refreshActiveSession();
  }
}
