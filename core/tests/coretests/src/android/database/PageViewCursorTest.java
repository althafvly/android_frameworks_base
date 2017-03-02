/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.annotation.Nullable;
import android.content.ContentResolver;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.util.MathUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

@RunWith(AndroidJUnit4.class)
public class PageViewCursorTest {

    private static final int ITEM_COUNT = 20;

    private static final String NAME_COLUMN = "name";
    private static final String NUM_COLUMN = "num";

    private static final String[] COLUMNS = new String[]{
      NAME_COLUMN,
      NUM_COLUMN
    };

    private static final String[] NAMES = new String[] {
            "000",
            "111",
            "222",
            "333",
            "444",
            "555",
            "666",
            "777",
            "888",
            "999",
            "aaa",
            "bbb",
            "ccc",
            "ddd",
            "eee",
            "fff",
            "ggg",
            "hhh",
            "iii",
            "jjj"
    };

    private MatrixCursor mDelegate;
    private PageViewCursor mCursor;

    @Before
    public void setUp() {
        Random rand = new Random();

        mDelegate = new MatrixCursor(COLUMNS);
        for (int i = 0; i < ITEM_COUNT; i++) {
            MatrixCursor.RowBuilder row = mDelegate.newRow();
            row.add(NAME_COLUMN, NAMES[i]);
            row.add(NUM_COLUMN, rand.nextInt());
        }

        mCursor = new PageViewCursor(mDelegate, 10, 5);
    }

    @Test
    public void testPage_Size() {
        assertEquals(5, mCursor.getCount());
    }

    @Test
    public void testPage_TotalSize() {
        assertEquals(ITEM_COUNT, mCursor.getExtras().getInt(ContentResolver.EXTRA_TOTAL_SIZE));
    }

    @Test
    public void testPage_OffsetExceedsCursorCount_EffectivelyEmptyCursor() {
        mCursor = new PageViewCursor(mDelegate, ITEM_COUNT * 2, 5);
        assertEquals(0, mCursor.getCount());
    }

    @Test
    public void testMoveToPosition() {
        assertTrue(mCursor.moveToPosition(0));
        assertEquals(NAMES[10], mCursor.getString(0));
        assertTrue(mCursor.moveToPosition(1));
        assertEquals(NAMES[11], mCursor.getString(0));
        assertTrue(mCursor.moveToPosition(4));
        assertEquals(NAMES[14], mCursor.getString(0));

        // and then back down again for good measure.
        assertTrue(mCursor.moveToPosition(1));
        assertEquals(NAMES[11], mCursor.getString(0));
        assertTrue(mCursor.moveToPosition(0));
        assertEquals(NAMES[10], mCursor.getString(0));
    }

    @Test
    public void testMoveToPosition_MoveToSamePosition_NoOp() {
        assertTrue(mCursor.moveToPosition(1));
        assertEquals(NAMES[11], mCursor.getString(0));
        assertTrue(mCursor.moveToPosition(1));
        assertEquals(NAMES[11], mCursor.getString(0));
    }

    @Test
    public void testMoveToPosition_PositionOutOfBounds_MovesToBeforeFirst() {
        assertTrue(mCursor.moveToPosition(0));
        assertEquals(NAMES[10], mCursor.getString(0));

        // move before
        assertFalse(mCursor.moveToPosition(-12));
        assertTrue(mCursor.isBeforeFirst());
    }

    @Test
    public void testMoveToPosition_PositionOutOfBounds_MovesToAfterLast() {
        assertTrue(mCursor.moveToPosition(0));
        assertEquals(NAMES[10], mCursor.getString(0));

        assertFalse(mCursor.moveToPosition(222));
        assertTrue(mCursor.isAfterLast());
    }

    @Test
    public void testPosition() {
        assertEquals(-1, mCursor.getPosition());
    }

    @Test
    public void testIsBeforeFirst() {
        assertTrue(mCursor.isBeforeFirst());
        mCursor.moveToFirst();
        assertFalse(mCursor.isBeforeFirst());
    }

    @Test
    public void testCount_ZeroForEmptyCursor() {
        mCursor = new PageViewCursor(mDelegate, 0, 0);
        assertEquals(0, mCursor.getCount());
    }

    @Test
    public void testIsBeforeFirst_TrueForEmptyCursor() {
        mCursor = new PageViewCursor(mDelegate, 0, 0);
        assertTrue(mCursor.isBeforeFirst());
    }

    @Test
    public void testIsAfterLast() {
        assertFalse(mCursor.isAfterLast());
        mCursor.moveToLast();
        mCursor.moveToNext();
        assertTrue(mCursor.isAfterLast());
    }

    @Test
    public void testIsAfterLast_TrueForEmptyCursor() {
        mCursor = new PageViewCursor(mDelegate, 0, 0);
        assertTrue(mCursor.isAfterLast());
    }

    @Test
    public void testIsFirst() {
        assertFalse(mCursor.isFirst());
        mCursor.moveToFirst();
        assertTrue(mCursor.isFirst());
    }

    @Test
    public void testIsLast() {
        assertFalse(mCursor.isLast());
        mCursor.moveToLast();
        assertTrue(mCursor.isLast());
    }

    @Test
    public void testMove() {
        // note that initial position is -1, so moving
        // 2 will only put as at 1.
        mCursor.move(2);
        assertEquals(NAMES[11], mCursor.getString(0));
        mCursor.move(-1);
        assertEquals(NAMES[10], mCursor.getString(0));
    }

    @Test
    public void testMoveToFist() {
        mCursor.moveToPosition(3);
        mCursor.moveToFirst();
        assertEquals(NAMES[10], mCursor.getString(0));
    }

    @Test
    public void testMoveToLast() {
        mCursor.moveToLast();
        assertEquals(NAMES[14], mCursor.getString(0));
    }

    @Test
    public void testMoveToNext() {
        // default position is -1, so next is 0.
        mCursor.moveToNext();
        assertEquals(NAMES[10], mCursor.getString(0));
    }

    @Test
    public void testMoveToNext_AfterLastReturnsFalse() {
        mCursor.moveToLast();
        assertFalse(mCursor.moveToNext());
    }

    @Test
    public void testMoveToPrevious() {
        mCursor.moveToPosition(3);
        mCursor.moveToPrevious();
        assertEquals(NAMES[12], mCursor.getString(0));
    }

    @Test
    public void testMoveToPrevious_BeforeFirstReturnsFalse() {
        assertFalse(mCursor.moveToPrevious());
    }

    @Test
    public void testWindow_ReadPastEnd() {
        assertFalse(mCursor.moveToPosition(10));
    }

    @Test
    public void testOffset_LimitOutOfBounds() {
        mCursor = new PageViewCursor(mDelegate, 5, 100);
        assertEquals(15, mCursor.getCount());
    }

    @Test
    public void testPagingMarker() {
        mCursor = new PageViewCursor(mDelegate, 5, 100);
        assertTrue(mCursor.getExtras().getBoolean(PageViewCursor.EXTRA_AUTO_PAGED));
    }

    @Test
    public void testWrap() {
        Bundle queryArgs = new Bundle();
        queryArgs.putInt(ContentResolver.QUERY_ARG_OFFSET, 5);
        queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, 5);
        Cursor wrapped = PageViewCursor.wrap(mDelegate, queryArgs);
        assertTrue(wrapped instanceof PageViewCursor);
        assertEquals(5, wrapped.getCount());
    }

    @Test
    public void testWrap_NoOpWithoutPagingArgs() {
        Cursor wrapped = PageViewCursor.wrap(mDelegate, Bundle.EMPTY);
        assertTrue(mDelegate == wrapped);
    }

    @Test
    public void testWrap_NoOpCursorsWithExistingPaging_ByTotalSize() {
        Bundle extras = new Bundle();
        extras.putInt(ContentResolver.EXTRA_TOTAL_SIZE, 5);
        mDelegate.setExtras(extras);

        Bundle queryArgs = new Bundle();
        queryArgs.putInt(ContentResolver.QUERY_ARG_OFFSET, 5);
        queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, 5);
        Cursor wrapped = PageViewCursor.wrap(mDelegate, queryArgs);
        assertTrue(mDelegate == wrapped);
    }

    @Test
    public void testWrap_NoOpCursorsWithExistingPaging_ByHonoredArgs() {
        Bundle extras = new Bundle();
        extras.putStringArray(
                ContentResolver.EXTRA_HONORED_ARGS,
                new String[] {
                    ContentResolver.QUERY_ARG_OFFSET,
                    ContentResolver.QUERY_ARG_LIMIT
                });
        mDelegate.setExtras(extras);

        Bundle queryArgs = new Bundle();
        queryArgs.putInt(ContentResolver.QUERY_ARG_OFFSET, 5);
        queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, 5);
        Cursor wrapped = PageViewCursor.wrap(mDelegate, queryArgs);
        assertTrue(mDelegate == wrapped);
    }

    private void assertStringAt(int row, int column, String expected) {
        mCursor.moveToPosition(row);
        assertEquals(expected, mCursor.getString(column));
    }
}
