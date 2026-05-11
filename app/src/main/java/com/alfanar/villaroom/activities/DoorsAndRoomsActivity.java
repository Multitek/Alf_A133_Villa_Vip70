package com.alfanar.villaroom.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.adapters.DeviceAdapter;
import com.alfanar.villaroom.databinding.ActivityDoorsAndRoomsBinding;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.MyUtils;
import com.alfanar.villaroom.util.SpacesItemDecoration;

import java.util.ArrayList;

public class DoorsAndRoomsActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    protected static DoorsAndRoomsActivity instance;
    ArrayList<DeviceModel> doorList = new ArrayList<>();
    ArrayList<DeviceModel> roomList = new ArrayList<>();
    ActivityDoorsAndRoomsBinding binding;
    private DeviceAdapter roomsAdapter;
    private DeviceAdapter doorsAdapter;
    private Dialog dialog;

    public static DoorsAndRoomsActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityDoorsAndRoomsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setBackgroundDrawableResource(MyUtils.getInstance().getWindowBackground(-1));

        roomList = DeviceController.getInstance().getRoomsList();
        doorList = DeviceController.getInstance().getDoorsList();


        MyUtils.getInstance().hideNavigation(this);
        MainTimeout.getInstance().setTimeout(60 * 1000);

        roomsAdapter = new DeviceAdapter(roomList, DoorsAndRoomsActivity.this);
        binding.recycleRoom.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recycleRoom.addItemDecoration(new SpacesItemDecoration(5));
        binding.recycleRoom.setAdapter(roomsAdapter);


        doorsAdapter = new DeviceAdapter(doorList, DoorsAndRoomsActivity.this);
        binding.recycleDoor.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recycleDoor.addItemDecoration(new SpacesItemDecoration(5));
        binding.recycleDoor.setAdapter(doorsAdapter);


        ImageView back = findViewById(R.id.img_back);
        back.setOnClickListener(v -> {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            finish();
        });
        ImageView settings = findViewById(R.id.img_other);
        settings.setVisibility(View.GONE);
        TextView txtTitle = findViewById(R.id.text_title);
        txtTitle.setText(getResources().getString(R.string.rooms_doors));

        instance = this;
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyUtils.getInstance().hideNavigation(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        instance = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void notifyAdapters() {
        this.runOnUiThread(() -> {
            roomList = DeviceController.getInstance().getRoomsList();
            doorList = DeviceController.getInstance().getDoorsList();

            doorsAdapter = new DeviceAdapter(doorList, DoorsAndRoomsActivity.this);
            binding.recycleDoor.setAdapter(doorsAdapter);

            roomsAdapter = new DeviceAdapter(roomList, DoorsAndRoomsActivity.this);
            binding.recycleRoom.setAdapter(roomsAdapter);

        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int stat = event.getAction();
        if (stat == MotionEvent.ACTION_DOWN) {
            MainTimeout.getInstance().setTimeout(60 * 1000);
        }
        return super.dispatchTouchEvent(event);
    }

    public void displayDialog(String text) {
        dialog = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_ok);
        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialog.findViewById(R.id.btnOk);
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(DoorsAndRoomsActivity.this);
    }
}
