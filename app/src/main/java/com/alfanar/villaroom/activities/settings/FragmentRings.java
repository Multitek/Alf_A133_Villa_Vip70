package com.alfanar.villaroom.activities.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.util.GeneralMediaPlayer;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import org.linphone.AndroidAudioManager;

import java.text.MessageFormat;
@SuppressLint("ApplySharedPref")
public class FragmentRings extends Fragment implements View.OnClickListener {
    private final TextView[] durations = new TextView[20];
    private final TextView[] doorRings = new TextView[6];
    private final TextView[] roomRings = new TextView[6];
    private SharedPreferences sp;
    private ToggleButton silentSwitch;
    private TextView ringDurationTitle;
    private TextView doorRingTitle;
    private TextView roomRingTitle;
    private LinearLayout layoutRingDuration;
    private LinearLayout layoutRingDoor;
    private LinearLayout layoutRingRoom;
    private LinearLayout layoutRingDurationItems;
    private LinearLayout layoutDoorRingItems;
    private LinearLayout layoutRoomRingItems;
    private ImageView expandCollapseRingDuration, expandCollapseRingDoor, expandCollapseRingRoom;
    private View selectedLayout;
    private ImageView selectedImgExpandCollapse;
    private boolean expandState = true;
    private LayoutInflater inflaterRing;
    private Dialog dialog;
    private SeekBar volumeBar;
    private Activity context;
    private TextView volumeText;
    private final int volumeDevice = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_rings, container, false);
        if (getActivity() != null) {
            context = getActivity();
        }
        sp = MyUtils.getInstance().getShared();
        inflaterRing = LayoutInflater.from(getActivity());
        volumeBar = view.findViewById(R.id.seek_volume);

        silentSwitch = view.findViewById(R.id.toggle_silent);
        ringDurationTitle = view.findViewById(R.id.text_ring_duration_title);
        doorRingTitle = view.findViewById(R.id.text_door_ring_title);
        roomRingTitle = view.findViewById(R.id.text_room_ring_title);
        layoutRingDuration = view.findViewById(R.id.layout_ring_duration);
        layoutRingDoor = view.findViewById(R.id.layout_door_ring);
        layoutRingRoom = view.findViewById(R.id.layout_room_ring);
        LinearLayout layoutRingDurationClick = view.findViewById(R.id.layout_ring_duration_click);
        LinearLayout layoutRingDoorClick = view.findViewById(R.id.layout_door_ring_click);
        LinearLayout layoutRingRoomClick = view.findViewById(R.id.layout_room_ring_click);
        layoutRingDurationItems = view.findViewById(R.id.layout_ring_duration_items);
        layoutDoorRingItems = view.findViewById(R.id.layout_door_ring_items);
        layoutRoomRingItems = view.findViewById(R.id.layout_room_ring_items);
        expandCollapseRingDuration = view.findViewById(R.id.img_expand_collapse_ring_duration);
        expandCollapseRingDoor = view.findViewById(R.id.img_expand_collapse_door_ring);
        expandCollapseRingRoom = view.findViewById(R.id.img_expand_collapse_room_ring);
        silentSwitch.setOnClickListener(this);
        layoutRingDurationClick.setOnClickListener(this);
        layoutRingDoorClick.setOnClickListener(this);
        layoutRingRoomClick.setOnClickListener(this);
        boolean disturbState = sp.getBoolean("DISTURB", false);
        silentSwitch.setChecked(disturbState);
        volumeBar.setMax(15);
        int vol = sp.getInt("volIndex", 7);
        volumeBar.setProgress(vol);
        setRingDuration();
        setDoorRing();
        setRoomRing();
        int ringDuration = sp.getInt("ringDuration", 8);
        ringDurationTitle.setText(String.valueOf(ringDuration));

        int ringDoorIndex = sp.getInt("ringDoorIndex", 0);
        doorRingTitle.setText(MessageFormat.format("{0}{1}", context.getResources().getString(R.string.tone), (ringDoorIndex + 1)));

        int ringRoomIndex = sp.getInt("ringRoomIndex", 0);
        roomRingTitle.setText(MessageFormat.format("{0}{1}", context.getResources().getString(R.string.tone), (ringRoomIndex + 1)));

        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int val, boolean fromUser) {

            }



            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int val = seekBar.getProgress();
                Logger.d("onStopTrackingTouch val=" + seekBar.getProgress());
                sp.edit().putInt("volIndex", val).commit();
                if (val == 0) {
                    sp.edit().putBoolean("DISTURB", true).commit();
                    silentSwitch.setChecked(true);
                    AndroidAudioManager.getInstance().stopMedia();
                } else {
                    sp.edit().putBoolean("DISTURB", false).commit();
                    silentSwitch.setChecked(false);
                    AndroidAudioManager.getInstance().adjustRingLevel(1);
                }
            }
        });


    /*    volumeText = view.findViewById(R.id.volume_text);
        ImageView  imgPlus = view.findViewById(R.id.volume_plus);
        ImageView  imgMinus = view.findViewById(R.id.volume_minus);
        SharedPreferences sp2 = context.getSharedPreferences("DEVICE_SETTINGS", MODE_PRIVATE);
         volumeDevice = sp2.getInt("device_volume", 0);
        setVolumeText(volumeDevice);

        imgMinus.setOnClickListener(v -> {
            if(volumeDevice>0){
                volumeDevice  = volumeDevice -1;
                setVolumeText(volumeDevice);
                sp2.edit().putInt("device_volume",volumeDevice).commit();
                LinphoneManager.getAudioManager().setVolumes();
            }

        });


        imgPlus.setOnClickListener(v -> {
            if(volumeDevice<9){
                volumeDevice  = volumeDevice +1;
                setVolumeText(volumeDevice);
                sp2.edit().putInt("device_volume",volumeDevice).commit();
                LinphoneManager.getAudioManager().setVolumes();
            }

        });*/


        return view;
    }


   /* private void setVolumeText (int volumeDevice){
        if(volumeDevice==1){
            volumeText.setText("71");
        }else  if(volumeDevice==2){
            volumeText.setText("72");
        }else  if(volumeDevice==3){
            volumeText.setText("73");
        }else  if(volumeDevice==4){
            volumeText.setText("74");
        }else  if(volumeDevice==5){
            volumeText.setText("75");
        }else  if(volumeDevice==6){
            volumeText.setText("76");
        }else  if(volumeDevice==7){
            volumeText.setText("77");
        }else  if(volumeDevice==8){
            volumeText.setText("78");
        }else  if(volumeDevice==9){
            volumeText.setText("79");
        }else{
            volumeText.setText("70");
        }
    }*/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        GeneralMediaPlayer.getInstance().stopMedia();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void setRingDuration() {
        layoutRingDurationItems.removeAllViews();
        for (int i = 1; i < 13; i++) {
            View viewDuration = inflaterRing.inflate(R.layout.layout_ring, null);
            final int pos = i * 5;
            durations[i] = viewDuration.findViewById(R.id.text);
            durations[i].setText(String.valueOf(pos));
            durations[i].setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ApplySharedPref")
                @Override
                public void onClick(View v) {
                    sp.edit().putInt("ringDuration", pos).commit();
                    setSize(layoutRingDuration, expandCollapseRingDuration);
                    ringDurationTitle.setText(String.valueOf(pos));
                    displayDialog(context.getResources().getString(R.string.saved));
                }
            });
            layoutRingDurationItems.addView(viewDuration);
        }
    }

    private void setDoorRing() {
        layoutDoorRingItems.removeAllViews();
        for (int i = 0; i < 6; i++) {
            View viewDuration = inflaterRing.inflate(R.layout.layout_ring, null);
            final int pos = i;
            doorRings[i] = viewDuration.findViewById(R.id.text);
            doorRings[i].setText(MessageFormat.format("{0}{1}", context.getResources().getString(R.string.tone), (i + 1)));
            doorRings[i].setOnClickListener(v -> {
                sp.edit().putInt("ringDoorIndex", pos).commit();
                AndroidAudioManager.getInstance().adjustRingLevel(1);
                setSize(layoutRingDoor, expandCollapseRingDoor);
                doorRingTitle.setText(MessageFormat.format("{0}{1}", context.getResources().getString(R.string.tone), (pos + 1)));
                displayDialog(getActivity().getResources().getString(R.string.saved));
            });
            layoutDoorRingItems.addView(viewDuration);
        }
    }

    private void setRoomRing() {
        layoutRoomRingItems.removeAllViews();
        for (int i = 0; i < 6; i++) {
            View viewDuration = inflaterRing.inflate(R.layout.layout_ring, null);
            final int pos = i;
            roomRings[i] = viewDuration.findViewById(R.id.text);
            roomRings[i].setText(MessageFormat.format("{0}{1}", context.getResources().getString(R.string.tone), (i + 1)));
            roomRings[i].setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ApplySharedPref")
                @Override
                public void onClick(View v) {
                    sp.edit().putInt("ringRoomIndex", pos).commit();
                    AndroidAudioManager.getInstance().adjustRingLevel(2);
                    setSize(layoutRingRoom, expandCollapseRingRoom);
                    roomRingTitle.setText(MessageFormat.format("{0}{1}", context.getResources().getString(R.string.tone), (pos + 1)));
                    displayDialog(context.getResources().getString(R.string.saved));
                }
            });
            layoutRoomRingItems.addView(viewDuration);
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

    @SuppressLint("ApplySharedPref")
    @Override
    public void onClick(View v) {
        v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
        int id = v.getId();
        if (id == R.id.toggle_silent) {
            boolean isChecked = silentSwitch.isChecked();
            sp.edit().putBoolean("DISTURB", isChecked).commit();
            displayDialog(context.getResources().getString(R.string.saved));
            if (isChecked) {
                volumeBar.setProgress(0);
            } else {
                volumeBar.setProgress(7);
            }
        } else if (id == R.id.layout_ring_duration_click) {
            setSize(layoutRingDuration, expandCollapseRingDuration);
        } else if (id == R.id.layout_door_ring_click) {
            setSize(layoutRingDoor, expandCollapseRingDoor);
        } else if (id == R.id.layout_room_ring_click) {
            setSize(layoutRingRoom, expandCollapseRingRoom);
        }
    }

    private void displayDialog(String text) {
        dialog = MyUtils.getInstance().dialogPublic(context, R.layout.dialog_ok);
        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialog.findViewById(R.id.btnOk);
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
