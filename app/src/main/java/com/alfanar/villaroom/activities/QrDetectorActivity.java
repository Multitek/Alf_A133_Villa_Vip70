package com.alfanar.villaroom.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.util.MyUtils;


public class QrDetectorActivity extends Activity {

    public static String id = "none";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_detector);
        MyUtils.getInstance().hideNavigation(this);
        TextView txtid = findViewById(R.id.txtid);
        txtid.setText(id);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }
}
