/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.timezonepicker;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.timezonepicker.TimeZoneFilterTypeAdapter.OnSetFilterListener;
import com.android.timezonepicker.TimeZonePickerView.OnTimeZoneSetListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TimeZoneResultAdapter extends BaseAdapter implements OnClickListener,
        OnSetFilterListener {
    private static final String TAG = "TimeZoneResultAdapter";
    private static final int VIEW_TAG_TIME_ZONE = R.id.time_zone;

    /** SharedPref name and key for recent time zones */
    private static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
    private static final String KEY_RECENT_TIMEZONES = "preferences_recent_timezones";

    /**
     * The delimiter we use when serializing recent timezones to shared
     * preferences
     */
    private static final String RECENT_TIMEZONES_DELIMITER = ",";

    /** The maximum number of recent timezones to save */
    private static final int MAX_RECENT_TIMEZONES = 3;

    private static final int RESULT_LABEL_RECENT = -100;
    private static final int RESULT_LABEL_CURRENT = -200;

    static class ViewHolder {
        TextView timeZone;
        TextView timeOffset;
        TextView location;
        TextView label;

        static void setupViewHolder(View v) {
            ViewHolder vh = new ViewHolder();
            vh.timeZone = (TextView) v.findViewById(R.id.time_zone);
            vh.timeOffset = (TextView) v.findViewById(R.id.time_offset);
            vh.location = (TextView) v.findViewById(R.id.location);
            vh.label = (TextView) v.findViewById(R.id.label);
            v.setTag(vh);
        }
    }

    private Context mContext;
    private LayoutInflater mInflater;

    private OnTimeZoneSetListener mTimeZoneSetListener;
    private TimeZoneData mTimeZoneData;

    private int[] mFilteredTimeZoneIndices;
    private int mFilteredTimeZoneLength = 0;
    private int mFilterType;

    public TimeZoneResultAdapter(Context context, TimeZoneData tzd,
            com.android.timezonepicker.TimeZonePickerView.OnTimeZoneSetListener l) {
        super();

        mContext = context;
        mTimeZoneData = tzd;
        mTimeZoneSetListener = l;

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mFilteredTimeZoneIndices = new int[mTimeZoneData.size()];

        onSetFilter(TimeZoneFilterTypeAdapter.FILTER_TYPE_NONE, null, 0);
    }

    // Implements OnSetFilterListener
    @Override
    public void onSetFilter(int filterType, String str, int time) {
        Log.d(TAG, "onSetFilter: " + filterType + " [" + str + "] " + time);

        mFilterType = filterType;
        mFilteredTimeZoneLength = 0;
        int idx = 0;

        switch (filterType) {
            case TimeZoneFilterTypeAdapter.FILTER_TYPE_EMPTY:
                break;
            case TimeZoneFilterTypeAdapter.FILTER_TYPE_NONE:
                // Show the default/current value first
                int defaultTzIndex = mTimeZoneData.getDefaultTimeZoneIndex();
                if (defaultTzIndex != -1) {
                    mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = RESULT_LABEL_CURRENT;
                    mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = defaultTzIndex;
                }

                // Show the recent selections
                SharedPreferences prefs = mContext.getSharedPreferences(SHARED_PREFS_NAME,
                        Context.MODE_PRIVATE);
                String recentsString = prefs.getString(KEY_RECENT_TIMEZONES, null);
                if (!TextUtils.isEmpty(recentsString)) {
                    String[] recents = recentsString.split(RECENT_TIMEZONES_DELIMITER);
                    boolean first = true;
                    for (int i = recents.length - 1; i >= 0; i--) {
                        if (!TextUtils.isEmpty(recents[i])
                                && !recents[i].equals(mTimeZoneData.mDefaultTimeZoneId)) {
                            int index = mTimeZoneData.findIndexByTimeZoneIdSlow(recents[i]);
                            if (index != -1) {
                                if (first) {
                                    mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] =
                                            RESULT_LABEL_RECENT;
                                    first = false;
                                }
                                mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = index;
                            }
                        }
                    }
                }

                break;
            case TimeZoneFilterTypeAdapter.FILTER_TYPE_GMT:
                ArrayList<Integer> indices = mTimeZoneData.getTimeZonesByOffset(time);
                if (indices != null) {
                    for (Integer i : indices) {
                        mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = i;
                    }
                }
                break;
            case TimeZoneFilterTypeAdapter.FILTER_TYPE_TIME:
                // TODO make this faster
                long now = System.currentTimeMillis();
                for (TimeZoneInfo tzi : mTimeZoneData.mTimeZones) {
                    int localHr = tzi.getLocalHr(now);
                    boolean match = localHr == time;
                    if (!match && !TimeZoneData.is24HourFormat) {
                        // PM + noon cases
                        if ((time + 12 == localHr) || (time == 12 && localHr == 0)) {
                            match = true;
                        }
                    }
                    if (match) {
                        mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = idx;
                    }
                    idx++;
                }
                break;
            case TimeZoneFilterTypeAdapter.FILTER_TYPE_TIME_ZONE:
                if (str != null) {
                    for (TimeZoneInfo tzi : mTimeZoneData.mTimeZones) {
                        if (str.equalsIgnoreCase(tzi.mDisplayName)) {
                            mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = idx;
                        }
                        idx++;
                    }
                }
                break;
            case TimeZoneFilterTypeAdapter.FILTER_TYPE_COUNTRY:
                ArrayList<Integer> tzIds = mTimeZoneData.mTimeZonesByCountry.get(str);
                if (tzIds != null) {
                    for (Integer tzi : tzIds) {
                        mFilteredTimeZoneIndices[mFilteredTimeZoneLength++] = tzi;
                    }
                }
                break;
            case TimeZoneFilterTypeAdapter.FILTER_TYPE_STATE:
                // TODO Filter by state
                break;
            default:
                throw new IllegalArgumentException();
        }
        notifyDataSetChanged();
    }

    /**
     * Saves the given timezone ID as a recent timezone under shared
     * preferences. If there are already the maximum number of recent timezones
     * saved, it will remove the oldest and append this one.
     *
     * @param id the ID of the timezone to save
     * @see {@link #MAX_RECENT_TIMEZONES}
     */
    public void saveRecentTimezone(String id) {
        SharedPreferences prefs = mContext.getSharedPreferences(SHARED_PREFS_NAME,
                Context.MODE_PRIVATE);
        String recentsString = prefs.getString(KEY_RECENT_TIMEZONES, null);
        if (recentsString == null) {
            recentsString = id;
        } else {
            List<String> recents = new ArrayList<String>(
                    Arrays.asList(recentsString.split(RECENT_TIMEZONES_DELIMITER)));
            Iterator<String> it = recents.iterator();
            while (it.hasNext()) {
                String tz = it.next();
                if (id.equals(tz)) {
                    it.remove();
                }
            }

            while (recents.size() >= MAX_RECENT_TIMEZONES) {
                recents.remove(0);
            }
            recents.add(id);

            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String recent : recents) {
                if (first) {
                    first = false;
                } else {
                    builder.append(RECENT_TIMEZONES_DELIMITER);
                }
                builder.append(recent);
            }
            recentsString = builder.toString();
        }

        prefs.edit().putString(KEY_RECENT_TIMEZONES, recentsString).apply();
    }

    @Override
    public int getCount() {
        return mFilteredTimeZoneLength;
    }

    @Override
    public Object getItem(int position) {
        if (position < 0 || position >= mFilteredTimeZoneLength) {
            return null;
        }

        switch (mFilteredTimeZoneIndices[position]) {
            case RESULT_LABEL_CURRENT:
                return "CURRENT TIME ZONE";
            case RESULT_LABEL_RECENT:
                return "RECENT TIME ZONE";
        }

        return mTimeZoneData.get(mFilteredTimeZoneIndices[position]);
    }

    @Override
    public boolean isEnabled(int position) {
        return mFilteredTimeZoneIndices[position] >= 0;
    }

    @Override
    public long getItemId(int position) {
        return mFilteredTimeZoneIndices[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            v = mInflater.inflate(R.layout.time_zone_item, null);
            v.setOnClickListener(this);
            ViewHolder.setupViewHolder(v);
        }

        ViewHolder vh = (ViewHolder) v.getTag();

        if (mFilteredTimeZoneIndices[position] >= 0) {
            TimeZoneInfo tzi = mTimeZoneData.get(mFilteredTimeZoneIndices[position]);
            v.setTag(VIEW_TAG_TIME_ZONE, tzi);

            vh.label.setVisibility(View.GONE);
            vh.timeZone.setText(tzi.mDisplayName);
            vh.timeZone.setVisibility(View.VISIBLE);

            vh.timeOffset.setText(tzi.getGmtDisplayName(mContext));
            vh.timeOffset.setVisibility(View.VISIBLE);

            String location = tzi.mCountry;
            if (location == null) {
                vh.location.setVisibility(View.INVISIBLE);
            } else {
                vh.location.setText(location);
                vh.location.setVisibility(View.VISIBLE);
            }
        } else {
            if (mFilteredTimeZoneIndices[position] == RESULT_LABEL_CURRENT) {
                vh.label.setText(v.getResources().getText(R.string.current_time_zone));
            } else if (mFilteredTimeZoneIndices[position] == RESULT_LABEL_RECENT) {
                vh.label.setText(v.getResources().getQuantityText(R.plurals.recent_time_zone,
                        /* num of recent tzs */ mFilteredTimeZoneLength - position - 1));
            }
            vh.label.setVisibility(View.VISIBLE);
            vh.timeZone.setVisibility(View.GONE);
            vh.timeOffset.setVisibility(View.GONE);
            vh.location.setVisibility(View.GONE);
            v.setTag(VIEW_TAG_TIME_ZONE, null);
        }

        return v;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    // Implements OnClickListener
    @Override
    public void onClick(View v) {
        if (mTimeZoneSetListener != null) {
            TimeZoneInfo tzi = (TimeZoneInfo) v.getTag(VIEW_TAG_TIME_ZONE);
            mTimeZoneSetListener.onTimeZoneSet(tzi);
            saveRecentTimezone(tzi.mTzId);
        }
    }

}
