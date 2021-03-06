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
 * limitations under the License
 */

package com.android.systemui.pip.phone;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.android.systemui.SysuiTestCase;
import com.android.systemui.pip.phone.PipTouchState;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PipTouchStateTest extends SysuiTestCase {

    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private PipTouchState mTouchState;
    private CountDownLatch mDoubleTapCallbackTriggeredLatch;

    @Before
    public void setUp() throws Exception {
        mHandlerThread = new HandlerThread("PipTouchStateTestThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mDoubleTapCallbackTriggeredLatch = new CountDownLatch(1);
        mTouchState = new PipTouchState(ViewConfiguration.get(getContext()),
                mHandler, () -> {
            mDoubleTapCallbackTriggeredLatch.countDown();
        });
        assertFalse(mTouchState.isDoubleTap());
        assertFalse(mTouchState.isWaitingForDoubleTap());
    }

    @Test
    public void testDoubleTapLongSingleTap_notDoubleTapAndNotWaiting() {
        final long currentTime = SystemClock.uptimeMillis();

        mTouchState.onTouchEvent(createMotionEvent(ACTION_DOWN, currentTime, 0, 0));
        mTouchState.onTouchEvent(createMotionEvent(ACTION_UP,
                currentTime + PipTouchState.DOUBLE_TAP_TIMEOUT + 10, 0, 0));
        assertFalse(mTouchState.isDoubleTap());
        assertFalse(mTouchState.isWaitingForDoubleTap());
        assertTrue(mTouchState.getDoubleTapTimeoutCallbackDelay() == -1);
    }

    @Test
    public void testDoubleTapTimeout_timeoutCallbackCalled() throws Exception {
        final long currentTime = SystemClock.uptimeMillis();

        mTouchState.onTouchEvent(createMotionEvent(ACTION_DOWN, currentTime, 0, 0));
        mTouchState.onTouchEvent(createMotionEvent(ACTION_UP,
                currentTime + PipTouchState.DOUBLE_TAP_TIMEOUT - 10, 0, 0));
        assertFalse(mTouchState.isDoubleTap());
        assertTrue(mTouchState.isWaitingForDoubleTap());

        assertTrue(mTouchState.getDoubleTapTimeoutCallbackDelay() == 10);
        mTouchState.scheduleDoubleTapTimeoutCallback();
        mDoubleTapCallbackTriggeredLatch.await(1, TimeUnit.SECONDS);
        assertTrue(mDoubleTapCallbackTriggeredLatch.getCount() == 0);
    }

    @Test
    public void testDoubleTapDrag_doubleTapCanceled() {
        final long currentTime = SystemClock.uptimeMillis();

        mTouchState.onTouchEvent(createMotionEvent(ACTION_DOWN, currentTime, 0, 0));
        mTouchState.onTouchEvent(createMotionEvent(ACTION_MOVE, currentTime + 10, 500, 500));
        mTouchState.onTouchEvent(createMotionEvent(ACTION_UP, currentTime + 20, 500, 500));
        assertTrue(mTouchState.isDragging());
        assertFalse(mTouchState.isDoubleTap());
        assertFalse(mTouchState.isWaitingForDoubleTap());
        assertTrue(mTouchState.getDoubleTapTimeoutCallbackDelay() == -1);
    }

    @Test
    public void testDoubleTap_doubleTapRegistered() {
        final long currentTime = SystemClock.uptimeMillis();

        mTouchState.onTouchEvent(createMotionEvent(ACTION_DOWN, currentTime, 0, 0));
        mTouchState.onTouchEvent(createMotionEvent(ACTION_UP, currentTime + 10, 0, 0));
        mTouchState.onTouchEvent(createMotionEvent(ACTION_DOWN,
                currentTime + PipTouchState.DOUBLE_TAP_TIMEOUT - 20, 0, 0));
        mTouchState.onTouchEvent(createMotionEvent(ACTION_UP,
                currentTime + PipTouchState.DOUBLE_TAP_TIMEOUT - 10, 0, 0));
        assertTrue(mTouchState.isDoubleTap());
        assertFalse(mTouchState.isWaitingForDoubleTap());
        assertTrue(mTouchState.getDoubleTapTimeoutCallbackDelay() == -1);
    }

    private MotionEvent createMotionEvent(int action, long eventTime, float x, float y) {
        return MotionEvent.obtain(0, eventTime, action, x, y, 0);
    }
}