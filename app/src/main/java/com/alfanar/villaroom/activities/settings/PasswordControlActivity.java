package com.alfanar.villaroom.activities.settings;


import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityPasswordControlBinding;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

public class PasswordControlActivity extends Activity implements View.OnClickListener {

    private final StringBuffer inputStringBuffer = new StringBuffer();
    ActivityPasswordControlBinding binding;
    private String pass = "0000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityPasswordControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setBackgroundDrawableResource(MyUtils.getInstance().getWindowBackground(-1));
        MyUtils.getInstance().hideNavigation(this);
        MainTimeout.getInstance().setTimeout(60 * 1000);
        binding.includeTop.imgOther.setVisibility(View.INVISIBLE);
        binding.includeTop.textTitle.setText(getString(R.string.password));
        binding.btn0.setOnClickListener(this);
        binding.btn1.setOnClickListener(this);
        binding.btn2.setOnClickListener(this);
        binding.btn3.setOnClickListener(this);
        binding.btn4.setOnClickListener(this);
        binding.btn5.setOnClickListener(this);
        binding.btn6.setOnClickListener(this);
        binding.btn7.setOnClickListener(this);
        binding.btn8.setOnClickListener(this);
        binding.btn9.setOnClickListener(this);
        binding.btnDelete.setOnClickListener(this);

        SharedPreferences sp = MyUtils.getInstance().getShared();
        pass = sp.getString("system_password", "0000");

        binding.includeTop.imgBack.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            setResult(-1);
            PasswordControlActivity.this.finish();
        });

        binding.txtInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 4) {
                    if (pass.equals(s.toString())) {
                        Logger.d("password correct");
                        binding.includeTop.textTitle.setTextColor(Color.parseColor("#00ff00"));
                        binding.includeTop.textTitle.setText(getString(R.string.pass_correct));
                        setResult(1);
                        try {
                            Thread.sleep(150);
                        } catch (InterruptedException e) {
                            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                        }
                        PasswordControlActivity.this.finish();
                    } else {
                        binding.includeTop.textTitle.setTextColor(Color.parseColor("#ff0000"));
                        binding.includeTop.textTitle.setText(getString(R.string.pass_incorrect));
                        binding.txtInput.setText("");
                        inputStringBuffer.setLength(0);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    public void onClick(View v) {
        v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
        if (v.getId() == binding.btn0.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.zero));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn1.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.one));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn2.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.two));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn3.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.three));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn4.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.four));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn5.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.five));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn6.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.six));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn7.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.seven));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn8.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.eight));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btn9.getId()) {
            if (binding.txtInput.getText().toString().length() < 5) {
                inputStringBuffer.append(getString(R.string.nine));
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        } else if (v.getId() == binding.btnDelete.getId()) {
            if (binding.txtInput.getText().toString().length() != 0) {
                inputStringBuffer.deleteCharAt(inputStringBuffer.length() - 1);
                binding.txtInput.setText(inputStringBuffer.toString());
            }
        }
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
    public boolean dispatchTouchEvent(MotionEvent event) {
        int stat = event.getAction();
        if (stat == MotionEvent.ACTION_DOWN) {
            MainTimeout.getInstance().setTimeout(60 * 1000);
        }
        return super.dispatchTouchEvent(event);
    }
}