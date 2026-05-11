package com.alfanar.villaroom.activities.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.util.MyUtils;

public class FragmentScreen extends Fragment implements View.OnClickListener {
    private Dialog dialog, dialogTheme;
    private Activity context;
    private SharedPreferences sp;
    private ImageView expandCollapseSleep;
    private LinearLayout layoutSleep;
    private TextView sleepTitle;
    private ImageView selectedImgExpandCollapse;
    private View selectedLayout;
    private boolean expandState = true;
    private int bgIndex = 1;
    private Drawable buttonDrawable;
    private boolean darkTheme;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @SuppressLint("ApplySharedPref")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_screen, container, false);

        context = getActivity();

        sp = MyUtils.getInstance().getShared();
        darkTheme = sp.getBoolean("DarkTheme", false);

        sleepTitle = view.findViewById(R.id.text_sleep_title);
        TextView sleepNever = view.findViewById(R.id.sleepNever);
        TextView sleep2 = view.findViewById(R.id.sleep2);
        TextView sleep5 = view.findViewById(R.id.sleep5);
        TextView sleep10 = view.findViewById(R.id.sleep10);
        TextView sleep30 = view.findViewById(R.id.sleep30);

        layoutSleep = view.findViewById(R.id.layout_sleep);

        LinearLayout layoutSleepClick = view.findViewById(R.id.layout_sleep_click);
        expandCollapseSleep = view.findViewById(R.id.img_expand_collapse_sleep);
        CardView cardTheme = view.findViewById(R.id.theme_card);
        cardTheme.setOnClickListener(this);
        layoutSleepClick.setOnClickListener(this);

        sleepNever.setOnClickListener(this);
        sleep2.setOnClickListener(this);
        sleep5.setOnClickListener(this);
        sleep10.setOnClickListener(this);
        sleep30.setOnClickListener(this);

        setSleepText();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        if (dialogTheme != null && dialogTheme.isShowing()) {
            dialogTheme.dismiss();
        }
    }

    private void setSleepText() {
        // Varsayılan değeri 1 yerine 0 (Sonsuz) yaptık
        int index = sp.getInt("timeout_index", 0);
        switch (index) {
            case 0:
                sleepTitle.setText(context.getResources().getString(R.string.never_sleep));
                break;
            case 1:
                sleepTitle.setText(context.getResources().getString(R.string.sleep_2));
                break;
            case 2:
                sleepTitle.setText(context.getResources().getString(R.string.sleep_5));
                break;
            case 3:
                sleepTitle.setText(context.getResources().getString(R.string.sleep_10));
                break;
            case 4:
                sleepTitle.setText(context.getResources().getString(R.string.sleep_30));
                break;
            default:
                sleepTitle.setText(context.getResources().getString(R.string.never_sleep));
                break;
        }
    }



    @SuppressLint("ApplySharedPref")
    @Override
    public void onClick(View v) {
        v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
        int id = v.getId();

        if (id == R.id.layout_sleep_click) {
            setSize(layoutSleep, expandCollapseSleep);
        }  else if (id == R.id.sleepNever) {
            sp.edit().putInt("timeout_index", 0).commit();
            displayDialog(context.getResources().getString(R.string.saved));
            setSize(layoutSleep, expandCollapseSleep);
            setSleepText();
        } else if (id == R.id.sleep2) {
            sp.edit().putInt("timeout_index", 1).commit();
            displayDialog(context.getResources().getString(R.string.saved));
            setSize(layoutSleep, expandCollapseSleep);
            setSleepText();
        } else if (id == R.id.sleep5) {
            sp.edit().putInt("timeout_index", 2).commit();
            displayDialog(context.getResources().getString(R.string.saved));
            setSize(layoutSleep, expandCollapseSleep);
            setSleepText();
        } else if (id == R.id.sleep10) {
            sp.edit().putInt("timeout_index", 3).commit();
            displayDialog(context.getResources().getString(R.string.saved));
            setSize(layoutSleep, expandCollapseSleep);
            setSleepText();
        } else if (id == R.id.sleep30) {
            sp.edit().putInt("timeout_index", 4).commit();
            displayDialog(context.getResources().getString(R.string.saved));
            setSize(layoutSleep, expandCollapseSleep);
            setSleepText();
        } else if (id == R.id.theme_card) {
            displayDialogTheme();
        }
    }

    private void setSize(View viewLayout, ImageView imgExpandCollapse) {
        if (selectedLayout != null) {
            if (selectedLayout == viewLayout) {
                if (expandState) {
                    expandState = false;
                    MyUtils.getInstance().collapse(viewLayout, 60);
                    imgExpandCollapse.setImageResource(R.drawable.ic_expand);
                } else {
                    expandState = true;
                    MyUtils.getInstance().expand(viewLayout);
                    imgExpandCollapse.setImageResource(R.drawable.ic_collapse);
                }
            } else {
                expandState = true;
                MyUtils.getInstance().expand(viewLayout);
                MyUtils.getInstance().collapse(selectedLayout, 60);
                imgExpandCollapse.setImageResource(R.drawable.ic_collapse);
                selectedImgExpandCollapse.setImageResource(R.drawable.ic_expand);
            }
        } else {
            expandState = true;
            MyUtils.getInstance().expand(viewLayout);
            imgExpandCollapse.setImageResource(R.drawable.ic_collapse);
        }
        selectedLayout = viewLayout;
        selectedImgExpandCollapse = imgExpandCollapse;
    }

    private void displayDialog(String text) {
        dialog = MyUtils.getInstance().dialogPublic(context, R.layout.dialog_ok);
        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialog.findViewById(R.id.btnOk);
        ok.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            dialog.dismiss();
        });

        dialog.show();
    }

    @SuppressLint({"ApplySharedPref", "UseCompatLoadingForDrawables"})
    private void displayDialogTheme() {
        if (dialogTheme != null && dialogTheme.isShowing()) {
            dialogTheme.dismiss();
        }
        dialogTheme = MyUtils.getInstance().dialogPublic(context, R.layout.dialog_theme);
        CardView cardDark = dialogTheme.findViewById(R.id.dark_theme);
        CardView cardLight = dialogTheme.findViewById(R.id.light_theme);
        LinearLayout layoutDark = dialogTheme.findViewById(R.id.dark_main_background);
        LinearLayout layoutLight = dialogTheme.findViewById(R.id.light_main_background);
        RadioButton radioDark = dialogTheme.findViewById(R.id.radio_dark);
        RadioButton radioLight = dialogTheme.findViewById(R.id.radio_light);
        bgIndex = sp.getInt("background_img_index", -1);
        radioDark.setClickable(false);
        radioLight.setClickable(false);
        if (darkTheme) {
            radioDark.setChecked(true);
            radioLight.setChecked(false);
            cardLight.setScaleX(0.5f);
            cardLight.setScaleY(0.5f);
            cardDark.setScaleX(1f);
            cardDark.setScaleY(1f);
            if (bgIndex == -1) {
                bgIndex = 1;
            }
        } else {
            radioDark.setChecked(false);
            radioLight.setChecked(true);
            cardDark.setScaleX(0.5f);
            cardDark.setScaleY(0.5f);
            cardLight.setScaleX(1f);
            cardLight.setScaleY(1f);
            if (bgIndex == -1) {
                bgIndex = 7;
            }
        }

        layoutDark.setBackgroundResource(MyUtils.getInstance().getWindowBackground(bgIndex));
        layoutLight.setBackgroundResource(MyUtils.getInstance().getWindowBackground(bgIndex));
        cardDark.setOnClickListener(view -> {
            darkTheme = true;
            radioDark.setChecked(true);
            radioLight.setChecked(false);
            cardDark.setScaleX(1f);
            cardDark.setScaleY(1f);
            cardLight.setScaleX(0.5f);
            cardLight.setScaleY(0.5f);
            layoutDark.setBackgroundResource(MyUtils.getInstance().getWindowBackground(bgIndex));

        });
        cardLight.setOnClickListener(view -> {
            darkTheme = false;
            radioDark.setChecked(false);
            radioLight.setChecked(true);
            cardDark.setScaleX(0.5f);
            cardDark.setScaleY(0.5f);
            cardLight.setScaleX(1f);
            cardLight.setScaleY(1f);
            layoutLight.setBackgroundResource(MyUtils.getInstance().getWindowBackground(bgIndex));
        });
        ImageView[] bg = new ImageView[10];
        bg[0] = dialogTheme.findViewById(R.id.background1);
        bg[1] = dialogTheme.findViewById(R.id.background2);
        bg[2] = dialogTheme.findViewById(R.id.background3);
        bg[3] = dialogTheme.findViewById(R.id.background4);
        bg[4] = dialogTheme.findViewById(R.id.background5);
        bg[5] = dialogTheme.findViewById(R.id.background6);
        bg[6] = dialogTheme.findViewById(R.id.background7);
        bg[7] = dialogTheme.findViewById(R.id.background8);
        bg[8] = dialogTheme.findViewById(R.id.background9);
        bg[9] = dialogTheme.findViewById(R.id.background10);

        buttonDrawable = context.getDrawable(R.drawable.layer3);
        buttonDrawable = DrawableCompat.wrap(buttonDrawable);
        DrawableCompat.setTint(buttonDrawable, context.getColor(R.color.green_new));
        bg[bgIndex - 1].setBackground(buttonDrawable);
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            bg[i].setOnClickListener(view -> {
                if (darkTheme) {
                    layoutDark.setBackgroundResource(MyUtils.getInstance().getWindowBackground((finalI + 1)));
                } else {
                    layoutLight.setBackgroundResource(MyUtils.getInstance().getWindowBackground((finalI + 1)));
                }
                bg[bgIndex - 1].setBackground(null);
                bgIndex = finalI + 1;
                bg[finalI].setBackground(buttonDrawable);
            });
        }

        Button set = dialogTheme.findViewById(R.id.btn_set);
        set.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            sp.edit().putBoolean("DarkTheme", darkTheme).commit();
            sp.edit().putInt("background_img_index", bgIndex).commit();
            MyUtils.getInstance().restartApp();
            dialogTheme.dismiss();
        });

        Button cancel = dialogTheme.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            dialogTheme.dismiss();
        });
        dialogTheme.show();
    }
}