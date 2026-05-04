package id.icapps.savera.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


import id.icapps.savera.activities.AbstractGBFragment;
import id.icapps.savera.activities.charts.DaySleepChartFragment;
import id.icapps.savera.activities.charts.WeekSleepChartFragment;

public class SleepFragmentAdapter extends NestedFragmentAdapter {
    public SleepFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        super(fragment, childFragmentManager);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DaySleepChartFragment();
            case 1:
                return WeekSleepChartFragment.newInstance(7);
            case 2:
                return WeekSleepChartFragment.newInstance(30);
        }
        return new DaySleepChartFragment();
    }
}
