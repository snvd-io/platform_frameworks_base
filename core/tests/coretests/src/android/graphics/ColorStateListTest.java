/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.graphics;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;

import androidx.test.filters.SmallTest;

import com.android.frameworks.coretests.R;

/**
 * Tests of {@link android.graphics.ColorStateList}
 */

public class ColorStateListTest extends AndroidTestCase {

    private Resources mResources;
    private int mFailureColor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
        mFailureColor = mResources.getColor(R.color.failColor);
    }

    @SmallTest
    public void testStateIsInList() throws Exception {
        ColorStateList colorStateList = mResources.getColorStateList(R.color.color1);
        int[] focusedState = {android.R.attr.state_focused};
        int focusColor = colorStateList.getColorForState(focusedState, R.color.failColor);
        assertEquals(mResources.getColor(R.color.testcolor1), focusColor);
    }

    @SmallTest
    public void testStateIsInList_proto() throws Exception {
        ColorStateList colorStateList = recreateFromProto(
                mResources.getColorStateList(R.color.color1));
        int[] focusedState = {android.R.attr.state_focused};
        int focusColor = colorStateList.getColorForState(focusedState, R.color.failColor);
        assertEquals(mResources.getColor(R.color.testcolor1), focusColor);
    }

    @SmallTest
    public void testEmptyState() throws Exception {
        ColorStateList colorStateList = mResources.getColorStateList(R.color.color1);
        int[] emptyState = {};
        int defaultColor = colorStateList.getColorForState(emptyState, mFailureColor);
        assertEquals(mResources.getColor(R.color.testcolor2), defaultColor);
    }

    @SmallTest
    public void testEmptyState_proto() throws Exception {
        ColorStateList colorStateList = recreateFromProto(
                mResources.getColorStateList(R.color.color1));
        int[] emptyState = {};
        int defaultColor = colorStateList.getColorForState(emptyState, mFailureColor);
        assertEquals(mResources.getColor(R.color.testcolor2), defaultColor);
    }

    @SmallTest
    public void testGetColor() throws Exception {
        int defaultColor = mResources.getColor(R.color.color1);
        assertEquals(mResources.getColor(R.color.testcolor2), defaultColor);
    }

    @SmallTest
    public void testGetColorWhenListHasNoDefault() throws Exception {
        int defaultColor = mResources.getColor(R.color.color_no_default);
        assertEquals(mResources.getColor(R.color.testcolor1), defaultColor);
    }

    @SmallTest
    public void testLstar() throws Exception {
        int defaultColor = mResources.getColor(R.color.color_with_lstar);
        assertEquals(mResources.getColor(R.color.testcolor3), defaultColor);
    }

    private ColorStateList recreateFromProto(ColorStateList colorStateList) throws Exception {
        ProtoOutputStream out = new ProtoOutputStream();
        colorStateList.writeToProto(out);
        ProtoInputStream in = new ProtoInputStream(out.getBytes());
        return ColorStateList.createFromProto(in);
    }
}
