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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class TimeZonePickerView extends LinearLayout implements TextWatcher, OnItemClickListener {
    private static final String TAG = "TimeZonePickerView";

    private Context mContext;
    private AutoCompleteTextView mAutoCompleteTextView;
    private TimeZoneFilterTypeAdapter mFilterAdapter;
    TimeZoneResultAdapter mResultAdapter;

    public interface OnTimeZoneSetListener {
        void onTimeZoneSet(TimeZoneInfo tzi);
    }

    public TimeZonePickerView(Context context, AttributeSet attrs,
            String timeZone, long timeMillis, OnTimeZoneSetListener l) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.timezonepickerview, this, true);

        TimeZoneData tzd = new TimeZoneData(mContext, timeZone, timeMillis);

        mResultAdapter = new TimeZoneResultAdapter(mContext, tzd, l);
        ListView timeZoneList = (ListView) findViewById(R.id.timezonelist);
        timeZoneList.setAdapter(mResultAdapter);

        mAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.searchBox);
        mFilterAdapter = new TimeZoneFilterTypeAdapter(mContext, tzd, mResultAdapter);
        mAutoCompleteTextView.setAdapter(mFilterAdapter);
        mAutoCompleteTextView.addTextChangedListener(this);
        mAutoCompleteTextView.setOnItemClickListener(this);
    }

    // Implementation of TextWatcher
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    // Implementation of TextWatcher
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mFilterAdapter.getFilter().filter(s.toString());
    }

    // Implementation of TextWatcher
    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // An onClickListener for the view item because I haven't figured out a
        // way to update the AutoCompleteTextView without causing an infinite loop.
        mFilterAdapter.onClick(view);
    }
}
