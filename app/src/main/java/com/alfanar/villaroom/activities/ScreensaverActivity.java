package com.alfanar.villaroom.activities;


import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.alfanar.villaroom.R;

public class ScreensaverActivity extends AppCompatActivity {
    public static ScreensaverActivity instance;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        System.out.println("uykuya girdi");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screensaver);


        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 0.5f; // Range: 0 (dark) to 1 (bright)
        getWindow().setAttributes(layout);

        // Optional: Hide navigation bar for full screen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        instance=this;
    }

    // Exit screensaver on any touch
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        finish();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        instance=null;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance=null;
    }

    public void destroyActivity(){
        ScreensaverActivity.this.finish();
    }
}
