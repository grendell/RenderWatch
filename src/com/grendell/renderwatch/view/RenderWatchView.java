package com.grendell.renderwatch.view;

import java.util.TimeZone;

import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.Int2;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Type;
import android.util.AttributeSet;
import android.view.View;

import com.grendell.renderwatch.R;
import com.grendell.renderwatch.ScriptC_watch;

public class RenderWatchView extends View {
    private static final float HOUR_HAND_MAGNITUDE = 0.5f;
    private static final int HOUR_HAND_WIDTH_SQR = 8 * 8;
    private static final float MINUTE_HAND_MAGNITUDE = 0.7f;
    private static final int MINUTE_HAND_WIDTH_SQR = 4 * 4;
    private static final float SECOND_HAND_MAGNITUDE = 0.8f;
    private static final int SECOND_HAND_WIDTH_SQR = 2 * 2;

    private static final long SEC_PER_MIN = 60L;
    private static final long SEC_PER_HOUR = 60L * 60L;
    private static final long SEC_PER_TWELVE_HOURS = 12L * 60L * 60L;

    private int[] mPixelBuffer;

    private RenderScript mRS;
    private ScriptC_watch mScript;
    private Allocation mPixelAlloc;

    private Int2 mCenter;
    private Int2 mHourHandEndpoint;
    private Int2 mMinuteHandEndpoint;
    private Int2 mSecondHandEndpoint;

    private boolean mIsActive;
    private AsyncTask< Void, Void, Void > mRenderTask;

    private Runnable mRenderRunnable = new Runnable() {

        @Override
        public void run() {
            if (!mIsActive) {
                return;
            }

            if (mRenderTask != null) {
                mRenderTask.cancel(true);
            }

            mRenderTask = new AsyncTask< Void, Void, Void >() {
                private long mRenderStart;

                @Override
                protected Void doInBackground(Void... params) {
                    mRenderStart = System.currentTimeMillis();

                    long now = System.currentTimeMillis();
                    long seconds = (now + TimeZone.getDefault().getOffset(now)) / 1000L;
                    float second = (1.0f / SEC_PER_MIN) * (seconds % SEC_PER_MIN);
                    float minute = (1.0f / SEC_PER_HOUR) * (seconds % SEC_PER_HOUR);
                    float hour = (1.0f / SEC_PER_TWELVE_HOURS) * (seconds % SEC_PER_TWELVE_HOURS);

                    int maxHandLength = Math.min(mCenter.x, mCenter.y);
                    double twoPi = 2.0 * Math.PI;
                    mSecondHandEndpoint.x = mCenter.x + (int) Math.round(maxHandLength * SECOND_HAND_MAGNITUDE * Math.sin(twoPi * second));
                    mSecondHandEndpoint.y = mCenter.y - (int) Math.round(maxHandLength * SECOND_HAND_MAGNITUDE * Math.cos(twoPi * second));
                    mMinuteHandEndpoint.x = mCenter.x + (int) Math.round(maxHandLength * MINUTE_HAND_MAGNITUDE * Math.sin(twoPi * minute));
                    mMinuteHandEndpoint.y = mCenter.y - (int) Math.round(maxHandLength * MINUTE_HAND_MAGNITUDE * Math.cos(twoPi * minute));
                    mHourHandEndpoint.x = mCenter.x + (int) Math.round(maxHandLength * HOUR_HAND_MAGNITUDE * Math.sin(twoPi * hour));
                    mHourHandEndpoint.y = mCenter.y - (int) Math.round(maxHandLength * HOUR_HAND_MAGNITUDE * Math.cos(twoPi * hour));

                    mScript.set_gCenter(mCenter);
                    mScript.set_gSecondHandEndpoint(mSecondHandEndpoint);
                    mScript.set_gSecondHandWidthSqr(SECOND_HAND_WIDTH_SQR);
                    mScript.set_gMinuteHandEndpoint(mMinuteHandEndpoint);
                    mScript.set_gMinuteHandWidthSqr(MINUTE_HAND_WIDTH_SQR);
                    mScript.set_gHourHandEndpoint(mHourHandEndpoint);
                    mScript.set_gHourHandWidthSqr(HOUR_HAND_WIDTH_SQR);

                    mScript.forEach_render(mPixelAlloc, mPixelAlloc);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    mPixelAlloc.copyTo(mPixelBuffer);
                    invalidate();

                    mRenderTask = null;
                    long delta = Math.max(0L, System.currentTimeMillis() - mRenderStart);
                    postDelayed(mRenderRunnable, 1000L - delta);
                }
            };
            mRenderTask.execute();
        }
    };

    public RenderWatchView(Context context) {
        super(context);
        init(context);
    }

    public RenderWatchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RenderWatchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mRS = RenderScript.create(context);
        mScript = new ScriptC_watch(mRS, context.getResources(), R.raw.watch);

        mCenter = new Int2();
        mHourHandEndpoint = new Int2();
        mMinuteHandEndpoint = new Int2();
        mSecondHandEndpoint = new Int2();

        mIsActive = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mPixelBuffer = new int[ w * h ];
        mPixelAlloc = Allocation.createTyped(mRS, new Type.Builder(mRS, Element.U32(mRS)).setX(w).setY(h).create(), Allocation.USAGE_SCRIPT);

        mCenter.x = w >> 1;
        mCenter.y = h >> 1;

        post(mRenderRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mPixelBuffer != null) {
            canvas.drawBitmap(mPixelBuffer, 0, getWidth(), 0, 0, getWidth(), getHeight(), false, null);
        }
    }

    public void onPause() {
        mIsActive = false;

        if (mRenderTask != null) {
            mRenderTask.cancel(true);
            mRenderTask = null;
        }
    }

    public void onResume() {
        mIsActive = true;
        post(mRenderRunnable);
    }
}