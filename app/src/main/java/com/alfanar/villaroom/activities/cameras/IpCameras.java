package com.alfanar.villaroom.activities.cameras;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityIpCamerasBinding;
import com.alfanar.villaroom.models.Camera2Model;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.MyUtils;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class IpCameras extends AppCompatActivity implements View.OnClickListener {

    public static IpCameras instance;
    public ArrayList<Camera2Model> list;
    ActivityIpCamerasBinding binding;
    private Dialog dialogAdd, dialogEdit;
    private boolean clickable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityIpCamerasBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(binding.getRoot());
        MyUtils.getInstance().hideNavigation(this);
        getWindow().setBackgroundDrawableResource(MyUtils.getInstance().getWindowBackground(-1));
        MainTimeout.getInstance().setTimeout(2 * 60 * 1000);


        binding.includeTop.imgBack.setOnClickListener(this);
        binding.includeTop.imgOther.setVisibility(View.VISIBLE);
        binding.includeTop.imgOther.setImageResource(R.drawable.ic_add_circle);
        binding.includeTop.imgOther.setOnClickListener(this);

        binding.includeTop.textTitle.setText(getResources().getString(R.string.cameras));


        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.CENTER);
        layoutManager.setAlignItems(AlignItems.CENTER);
        binding.recyclerViewCameras.setLayoutManager(layoutManager);
        instance = this;
        setList();

    }


    @Override
    protected void onResume() {
        super.onResume();
        MyUtils.getInstance().hideNavigation(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialogAdd != null && dialogAdd.isShowing()) {
            dialogAdd.dismiss();
        }
        if (dialogEdit != null && dialogEdit.isShowing()) {
            dialogEdit.dismiss();
        }
        instance = null;
    }

    @Override
    public void onClick(View v) {
        MyUtils.getInstance().setViewClickTimeout(v, 500);
        int id = v.getId();
        if (id == binding.includeTop.imgOther.getId()) {
            if (clickable) {
                clickable = false;
                displayDialogAdd();
            }
        } else if (id == binding.includeTop.imgBack.getId()) {
            finish();
        }
    }

    private void setList() {
        list = DatabaseHelper.getInstance().getAllCameras2();
        if (list.isEmpty()) {
            binding.cardEmpty.setVisibility(View.VISIBLE);
            binding.info.setVisibility(View.GONE);
        } else {
            binding.info.setVisibility(View.VISIBLE);
            binding.cardEmpty.setVisibility(View.GONE);
            Collections.sort(list, (o1, o2) -> {
                String v1 = (o1.getName());
                String v2 = (o2.getName());
                return v1.compareToIgnoreCase(v2);
            });
        }
        CameraAdapter adapter = new CameraAdapter(this, list);
        binding.recyclerViewCameras.setAdapter(adapter);
    }

    private void displayDialogAdd() {
        MainTimeout.getInstance().setTimeout(120 * 1000);
        dialogAdd = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_camera_add);
        ConstraintLayout dialogRoot = dialogAdd.findViewById(R.id.dialog_root);
        dialogRoot.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) IpCameras.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                View decorView = dialogAdd.getWindow().getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        });
        final TextInputEditText edCameraName = dialogAdd.findViewById(R.id.ed_camera_name);
        final TextInputEditText edIp = dialogAdd.findViewById(R.id.ed_ip);
        final TextInputEditText edUser = dialogAdd.findViewById(R.id.ed_user);
        final TextInputEditText edPass = dialogAdd.findViewById(R.id.ed_pass);


        edIp.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) IpCameras.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    View decorView = dialogAdd.getWindow().getDecorView();
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }
            return false;
        });


        final TextInputLayout textInputLayoutRtsp = dialogAdd.findViewById(R.id.text_input_ip);
        final TextInputLayout textInputLayoutCameraName = dialogAdd.findViewById(R.id.text_input_camera_name);
        Button cancel = dialogAdd.findViewById(R.id.btnCancel);
        cancel.setOnClickListener(v -> dialogAdd.dismiss());

        Button save = dialogAdd.findViewById(R.id.btnSave);
        save.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            String ip = Objects.requireNonNull(edIp.getText()).toString().replace(" ", "");
            String name = Objects.requireNonNull(edCameraName.getText()).toString().trim();
            String user = Objects.requireNonNull(edUser.getText()).toString().trim();
            String pass = Objects.requireNonNull(edPass.getText()).toString().trim();


            if (name.isEmpty() || name.length() > 20) {
                int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}};
                int[] colors = new int[]{getColor(R.color.red_new)};
                ColorStateList colorStateList = new ColorStateList(states, colors);
                textInputLayoutCameraName.setErrorEnabled(true);
                textInputLayoutCameraName.setError(getResources().getString(R.string.check_input));
                textInputLayoutCameraName.setErrorTextColor(colorStateList);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    textInputLayoutCameraName.setHint(getResources().getString(R.string.camera_name));
                    textInputLayoutCameraName.setError(null);
                }, 2500);
            } else if (ip.trim().isEmpty() || ip.replace(" ", "").isEmpty()) {

                int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}};
                int[] colors = new int[]{getColor(R.color.red_new)};
                ColorStateList colorStateList = new ColorStateList(states, colors);
                textInputLayoutRtsp.setErrorEnabled(true);
                textInputLayoutRtsp.setError(getResources().getString(R.string.check_input));
                textInputLayoutRtsp.setErrorTextColor(colorStateList);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    textInputLayoutRtsp.setHint(getResources().getString(R.string.camera_ip));
                    textInputLayoutRtsp.setError(null);
                }, 2500);
            } else {
                Camera2Model cam = new Camera2Model(0, name, ip, user, pass);
                DatabaseHelper.getInstance().insertCamera2(cam);
                setList();
                dialogAdd.dismiss();
            }
        });
        dialogAdd.setOnDismissListener(dialog -> {
            MyUtils.getInstance().hideKeyboard(IpCameras.this);
            MyUtils.getInstance().hideNavigation(IpCameras.this);
            clickable = true;
        });
        dialogAdd.show();
    }

    public void displayDialogEdit(final int pos) {
        MainTimeout.getInstance().setTimeout(120 * 1000);
        final Camera2Model model = list.get(pos);
        dialogEdit = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_camera_edit);
        ConstraintLayout dialogRoot = dialogEdit.findViewById(R.id.dialog_root);
        dialogRoot.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) IpCameras.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                View decorView = dialogEdit.getWindow().getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        });
        final TextInputEditText edCameraName = dialogEdit.findViewById(R.id.ed_camera_name);
        final TextInputEditText edIp = dialogEdit.findViewById(R.id.ed_ip);
        final TextInputEditText edUser = dialogEdit.findViewById(R.id.ed_user);
        final TextInputEditText edPass = dialogEdit.findViewById(R.id.ed_pass);


        edCameraName.setText(model.getName());
        edIp.setText(model.getIp());
        edUser.setText(model.getUserName());
        edPass.setText(model.getPassword());


        edIp.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) IpCameras.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    View decorView = dialogEdit.getWindow().getDecorView();
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }
            return false;
        });


        final TextInputLayout textInputLayoutRtsp = dialogEdit.findViewById(R.id.text_input_ip);
        final TextInputLayout textInputLayoutCameraName = dialogEdit.findViewById(R.id.text_input_camera_name);
        Button cancel = dialogEdit.findViewById(R.id.btnCancel);
        cancel.setOnClickListener(v -> dialogEdit.dismiss());

        Button save = dialogEdit.findViewById(R.id.btnSave);
        save.setOnClickListener(v -> {

            String ip = Objects.requireNonNull(edIp.getText()).toString().replace(" ", "");
            String name = Objects.requireNonNull(edCameraName.getText()).toString().trim();
            String user = Objects.requireNonNull(edUser.getText()).toString().trim();
            String pass = Objects.requireNonNull(edPass.getText()).toString().trim();


            if (name.isEmpty() || name.length() > 20) {
                int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}};
                int[] colors = new int[]{getColor(R.color.red_new)};
                ColorStateList colorStateList = new ColorStateList(states, colors);
                textInputLayoutCameraName.setErrorEnabled(true);
                textInputLayoutCameraName.setError(getResources().getString(R.string.check_input));
                textInputLayoutCameraName.setErrorTextColor(colorStateList);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    textInputLayoutCameraName.setHint(getResources().getString(R.string.camera_name));
                    textInputLayoutCameraName.setError(null);
                }, 2500);
            } else if (ip.trim().isEmpty() || ip.replace(" ", "").isEmpty()) {

                int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}};
                int[] colors = new int[]{getColor(R.color.red_new)};
                ColorStateList colorStateList = new ColorStateList(states, colors);
                textInputLayoutRtsp.setErrorEnabled(true);
                textInputLayoutRtsp.setError(getResources().getString(R.string.check_input));
                textInputLayoutRtsp.setErrorTextColor(colorStateList);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    textInputLayoutRtsp.setHint(getResources().getString(R.string.camera_ip));
                    textInputLayoutRtsp.setError(null);
                }, 2500);
            } else {
                model.setIp(ip);
                model.setName(name);
                model.setUserName(user);
                model.setPassword(pass);
                DatabaseHelper.getInstance().updateCamera2(model);

                setList();
                dialogEdit.dismiss();
            }
        });

        Button remove = dialogEdit.findViewById(R.id.btnDelete);
        remove.setOnClickListener(v -> {
            DatabaseHelper.getInstance().deleteCamera2(model.getId());
            setList();
            dialogEdit.dismiss();
        });
        dialogEdit.setOnDismissListener(dialog -> {
            MyUtils.getInstance().hideKeyboard(IpCameras.this);
            MyUtils.getInstance().hideNavigation(IpCameras.this);
            clickable = true;
        });
        dialogEdit.show();
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int stat = event.getAction();
        if (stat == MotionEvent.ACTION_DOWN) {
            MainTimeout.getInstance().setTimeout(60 * 1000);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(IpCameras.this);
    }


}
