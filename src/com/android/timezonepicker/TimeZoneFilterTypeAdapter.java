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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;

public class TimeZoneFilterTypeAdapter extends BaseAdapter implements Filterable, OnClickListener {
    public static final String TAG = "TimeZoneFilterTypeAdapter";

    public static final int FILTER_TYPE_EMPTY = -1;
    public static final int FILTER_TYPE_NONE = 0;
    public static final int FILTER_TYPE_TIME = 1;
    public static final int FILTER_TYPE_TIME_ZONE = 2;
    public static final int FILTER_TYPE_COUNTRY = 3;
    public static final int FILTER_TYPE_STATE = 4;
    public static final int FILTER_TYPE_GMT = 5;

    public interface OnSetFilterListener {
        void onSetFilter(int filterType, String str, int time);
    }

    static class ViewHolder {
        int filterType;
        String str;
        int time;

        TextView typeTextView;
        TextView strTextView;

        static void setupViewHolder(View v) {
            ViewHolder vh = new ViewHolder();
            vh.typeTextView = (TextView) v.findViewById(R.id.type);
            vh.strTextView = (TextView) v.findViewById(R.id.value);
            v.setTag(vh);
        }
    }

    class FilterTypeResult {
        boolean showLabel;
        int type;
        String constraint;
        public int time;

        @Override
        public String toString() {
            return constraint;
        }
    }

    private ArrayList<FilterTypeResult> mLiveResults = new ArrayList<FilterTypeResult>();
    private int mLiveResultsCount = 0;

    private ArrayFilter mFilter;

    private LayoutInflater mInflater;

    private TimeZoneData mTimeZoneData;
    private OnSetFilterListener mListener;

    public TimeZoneFilterTypeAdapter(Context context, TimeZoneData tzd, OnSetFilterListener l) {
        mTimeZoneData = tzd;
        mListener = l;

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mLiveResultsCount;
    }

    @Override
    public FilterTypeResult getItem(int position) {
        return mLiveResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;

        if (convertView != null) {
            v = convertView;
        } else {
            v = mInflater.inflate(R.layout.time_zone_filter_item, null);
            ViewHolder.setupViewHolder(v);
        }

        ViewHolder vh = (ViewHolder) v.getTag();

        if (position >= mLiveResults.size()) {
            Log.e(TAG, "getView: " + position + " of " + mLiveResults.size());
        }

        FilterTypeResult filter = mLiveResults.get(position);

        vh.filterType = filter.type;
        vh.str = filter.constraint;
        vh.time = filter.time;

        if (filter.showLabel) {
            int resId;
            switch (filter.type) {
                case FILTER_TYPE_GMT:
                    resId = R.string.gmt_offset;
                    break;
                case FILTER_TYPE_TIME:
                    resId = R.string.local_time;
                    break;
                case FILTER_TYPE_TIME_ZONE:
                    resId = R.string.time_zone;
                    break;
                case FILTER_TYPE_COUNTRY:
                    resId = R.string.country;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            vh.typeTextView.setText(resId);
            vh.typeTextView.setVisibility(View.VISIBLE);
            vh.strTextView.setVisibility(View.GONE);
        } else {
            vh.typeTextView.setVisibility(View.GONE);
            vh.strTextView.setVisibility(View.VISIBLE);
        }
        vh.strTextView.setText(filter.constraint);
        return v;
    }

    OnClickListener mDummyListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
        }
    };

    // Implements OnClickListener

    // This onClickListener is actually called from the AutoCompleteTextView's
    // onItemClickListener. Trying to update the text in AutoCompleteTextView
    // is causing an infinite loop.
    @Override
    public void onClick(View v) {
        if (mListener != null && v != null) {
            ViewHolder vh = (ViewHolder) v.getTag();
            mListener.onSetFilter(vh.filterType, vh.str, vh.time);
        }
        notifyDataSetInvalidated();
    }

    // Implements Filterable
    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            Log.e(TAG, "performFiltering >>>> [" + prefix + "]");

            FilterResults results = new FilterResults();
            String prefixString = null;
            if (prefix != null) {
                prefixString = prefix.toString().trim().toLowerCase();
            }

            if (TextUtils.isEmpty(prefixString)) {
                results.values = null;
                results.count = 0;
                return results;
            }

            // TODO Perf - we can loop through the filtered list if the new
            // search string starts with the old search string
            ArrayList<FilterTypeResult> filtered = new ArrayList<FilterTypeResult>();

            // ////////////////////////////////////////
            // Search by local time and GMT offset
            // ////////////////////////////////////////
            boolean gmtOnly = false;
            int startParsePosition = 0;
            if (prefixString.charAt(0) == '+' || prefixString.charAt(0) == '-') {
                gmtOnly = true;
            }

            if (prefixString.startsWith("gmt")) {
                startParsePosition = 3;
                gmtOnly = true;
            }

            int num = parseNum(prefixString, startParsePosition);
            if (num != Integer.MIN_VALUE) {
                boolean positiveOnly = prefixString.length() > startParsePosition
                        && prefixString.charAt(startParsePosition) == '+';
                handleSearchByGmt(filtered, num, positiveOnly);

                // Search by time
//                if (!gmtOnly) {
//                    for(TimeZoneInfo tzi : mTimeZoneData.mTimeZones) {
//                        tzi.getLocalHr(referenceTime)
//                    }
//                }

            }

            // ////////////////////////////////////////
            // Search by country
            // ////////////////////////////////////////
            boolean first = true;
            for (String country : mTimeZoneData.mTimeZonesByCountry.keySet()) {
                // TODO Perf - cache toLowerCase()?
                if (country != null && country.toLowerCase().startsWith(prefixString)) {
                    FilterTypeResult r;
                    if (first) {
                        r = new FilterTypeResult();
                        filtered.add(r);
                        r.type = FILTER_TYPE_COUNTRY;
                        r.constraint = null;
                        r.showLabel = true;
                        first = false;
                    }
                    r = new FilterTypeResult();
                    filtered.add(r);
                    r.type = FILTER_TYPE_COUNTRY;
                    r.constraint = country;
                    r.showLabel = false;
                }
            }

            // ////////////////////////////////////////
            // Search by time zone name
            // ////////////////////////////////////////
            first = true;
            for (String timeZoneName : mTimeZoneData.mTimeZoneNames) {
                // TODO Perf - cache toLowerCase()?
                if (timeZoneName.toLowerCase().startsWith(prefixString)) {
                    FilterTypeResult r;
                    if (first) {
                        r = new FilterTypeResult();
                        filtered.add(r);
                        r.type = FILTER_TYPE_TIME_ZONE;
                        r.constraint = null;
                        r.showLabel = true;
                        first = false;
                    }
                    r = new FilterTypeResult();
                    filtered.add(r);
                    r.type = FILTER_TYPE_TIME_ZONE;
                    r.constraint = timeZoneName;
                    r.showLabel = false;
                }
            }

            // ////////////////////////////////////////
            // TODO Search by state
            // ////////////////////////////////////////
            Log.e(TAG, "performFiltering <<<< " + filtered.size() + "[" + prefix + "]");

            results.values = filtered;
            results.count = filtered.size();
            return results;
        }

        private void handleSearchByGmt(ArrayList<FilterTypeResult> filtered, int num,
                boolean positiveOnly) {
            FilterTypeResult r;
            int originalResultCount = filtered.size();

            // Separator
            r = new FilterTypeResult();
            filtered.add(r);
            r.type = FILTER_TYPE_GMT;
            r.showLabel = true;

            if (num >= 0) {
                if (num == 1) {
                    for (int i = 19; i >= 10; i--) {
                        if (mTimeZoneData.hasTimeZonesInHrOffset(i)) {
                            r = new FilterTypeResult();
                            filtered.add(r);
                            r.type = FILTER_TYPE_GMT;
                            r.time = i;
                            r.constraint = "GMT+" + r.time;
                            r.showLabel = false;
                        }
                    }
                }

                if (mTimeZoneData.hasTimeZonesInHrOffset(num)) {
                    r = new FilterTypeResult();
                    filtered.add(r);
                    r.type = FILTER_TYPE_GMT;
                    r.time = num;
                    r.constraint = "GMT+" + r.time;
                    r.showLabel = false;
                }
                num *= -1;
            }

            if (!positiveOnly && num != 0) {
                if (mTimeZoneData.hasTimeZonesInHrOffset(num)) {
                    r = new FilterTypeResult();
                    filtered.add(r);
                    r.type = FILTER_TYPE_GMT;
                    r.time = num;
                    r.constraint = "GMT" + r.time;
                    r.showLabel = false;
                }

                if (num == -1) {
                    for (int i = -10; i >= -19; i--) {
                        if (mTimeZoneData.hasTimeZonesInHrOffset(i)) {
                            r = new FilterTypeResult();
                            filtered.add(r);
                            r.type = FILTER_TYPE_GMT;
                            r.time = i;
                            r.constraint = "GMT" + r.time;
                            r.showLabel = false;
                        }
                    }
                }
            }

            // Nothing was added except for the separator. Let's remove it.
            if (filtered.size() == originalResultCount + 1) {
                filtered.remove(originalResultCount);
            }
            return;
        }

        //
        // int start = Integer.MAX_VALUE;
        // int end = Integer.MIN_VALUE;
        // switch(num) {
        // case 2:
        // if (TimeZoneData.is24HourFormat) {
        // start = 23;
        // end = 20;
        // }
        // break;
        // case 1:
        // if (TimeZoneData.is24HourFormat) {
        // start = 19;
        // } else {
        // start = 12;
        // }
        // end = 10;
        // break;
        // }

        /**
         * Acceptable strings are in the following format: [+-]?[0-9]?[0-9]
         *
         * @param str
         * @param startIndex
         * @return Integer.MIN_VALUE as invalid
         */
        public int parseNum(String str, int startIndex) {
            int idx = startIndex;
            int num = Integer.MIN_VALUE;
            int negativeMultiplier = 1;

            // First char - check for + and -
            char ch = str.charAt(idx++);
            switch (ch) {
                case '-':
                    negativeMultiplier = -1;
                    // fall through
                case '+':
                    if (idx >= str.length()) {
                        // No more digits
                        return Integer.MIN_VALUE;
                    }

                    ch = str.charAt(idx++);
                    break;
            }

            if (!Character.isDigit(ch)) {
                // No digit
                return Integer.MIN_VALUE;
            }

            // Got first digit
            num = Character.digit(ch, 10);

            // Check next char
            if (idx < str.length()) {
                ch = str.charAt(idx++);
                if (Character.isDigit(ch)) {
                    // Got second digit
                    num = 10 * num + Character.digit(ch, 10);
                } else {
                    return Integer.MIN_VALUE;
                }
            }

            if (idx != str.length()) {
                // Invalid
                return Integer.MIN_VALUE;
            }

            Log.e(TAG, "Parsing " + str + " -> " + negativeMultiplier * num);
            return negativeMultiplier * num;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults
                results) {
            if (results.values == null || results.count == 0) {
                if (mListener != null) {
                    int filterType;
                    if (TextUtils.isEmpty(constraint)) {
                        filterType = FILTER_TYPE_NONE;
                    } else {
                        filterType = FILTER_TYPE_EMPTY;
                    }
                    mListener.onSetFilter(filterType, null, 0);
                }
                Log.e(TAG, "publishResults: " + results.count + " of null [" + constraint);
            } else {
                mLiveResults = (ArrayList<FilterTypeResult>) results.values;
                Log.e(TAG, "publishResults: " + results.count + " of " + mLiveResults.size() + " ["
                        + constraint);
            }
            mLiveResultsCount = results.count;

            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
