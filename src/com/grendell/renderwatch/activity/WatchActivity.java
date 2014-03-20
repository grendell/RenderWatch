package com.grendell.renderwatch.activity;

import android.app.Activity;
import android.os.Bundle;

import com.grendell.renderwatch.R;
import com.grendell.renderwatch.view.RenderWatchView;

public class WatchActivity extends Activity {

    private RenderWatchView mRenderWatchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        mRenderWatchView = (RenderWatchView) findViewById(R.id.watch);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRenderWatchView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRenderWatchView.onPause();
    }
}