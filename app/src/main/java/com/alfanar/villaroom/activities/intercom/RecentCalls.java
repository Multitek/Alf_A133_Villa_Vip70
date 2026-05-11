package com.alfanar.villaroom.activities.intercom;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.interfaces.HistoryListener;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.alfanar.villaroom.util.SpacesItemDecoration;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class RecentCalls extends AppCompatActivity implements HistoryListener, View.OnClickListener, DatePickerDialog.OnDateSetListener {

    @SuppressLint("StaticFieldLeak")
    public static RecentCalls instance;
    private RecyclerView rViewCall;
    private CallsAdapter callsAdapter;
    private ArrayList<CallModel> callsList;
    private Dialog deleteDialog, dateDialog;
    private CardView cardEmpty;
    private boolean clickable = true, clickDialog = true;
    private CardView cardArchive;
    private FloatingActionButton fabRemove;
    private LinearLayout layoutView, layoutSearch;
    private Calendar calendar;
    private long endDate, startDate;
    private boolean startDateSelect = false;
    private DateFormat dateFormat;
    private TextView txtStartDate, txtEndDate, txtArchive;
    private int select = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_calls);
        getWindow().setBackgroundDrawableResource(MyUtils.getInstance().getWindowBackground(-1));
        MyUtils.getInstance().hideNavigation(this);
        MainTimeout.getInstance().setTimeout(60 * 1000);
        ImageView back = findViewById(R.id.img_back);
        back.setOnClickListener(this);
        ImageView imgOther = findViewById(R.id.img_other);
        imgOther.setVisibility(View.GONE);
        TextView txtTitle = findViewById(R.id.text_title);
        txtTitle.setText(getResources().getString(R.string.history_calls));
        instance = this;
        rViewCall = findViewById(R.id.call_list);
        rViewCall.setItemAnimator(new DefaultItemAnimator());
        rViewCall.addItemDecoration(new SpacesItemDecoration(MyUtils.getInstance().dpToPx(5)));
        fabRemove = findViewById(R.id.fab_remove_all_calls);
        fabRemove.setOnClickListener(this);
        cardEmpty = findViewById(R.id.card_empty);
        cardArchive = findViewById(R.id.card_archive);
        cardArchive.setOnClickListener(this);
        layoutView = findViewById(R.id.layout_view);
        layoutSearch = findViewById(R.id.search_layout);
        txtStartDate = findViewById(R.id.txt_start_date);
        txtEndDate = findViewById(R.id.txt_end_date);
        txtArchive = findViewById(R.id.txt_archive);
        ImageView imgSearch = findViewById(R.id.img_search);
        CardView cardStartDate = findViewById(R.id.card_start_date);
        CardView cardEndDate = findViewById(R.id.card_end_date);
        cardStartDate.setOnClickListener(this);
        cardEndDate.setOnClickListener(this);
        imgSearch.setOnClickListener(this);
        setList(0);
        new Thread() {
            @Override
            public void run() {
                super.run();
                DatabaseHelper.getInstance().setAllCallReadState();
            }
        }.start();
        calendar = Calendar.getInstance();
        dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        MyUtils.getInstance().historyListener = this;
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
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        MyUtils.getInstance().historyListener = null;
        if (deleteDialog != null && deleteDialog.isShowing()) {
            deleteDialog.dismiss();
        }
        if (dateDialog != null && dateDialog.isShowing()) {
            dateDialog.dismiss();
        }
    }


    private void setList(int selectListOption) {
        clickable = true;
        select = selectListOption;
        if (selectListOption == 0) {
            runOnUiThread(() -> {
                layoutSearch.setVisibility(View.GONE);
                txtArchive.setText(getString(R.string.archive));
            });
            callsList = DatabaseHelper.getInstance().getShowCalls();
        } else {
            startDate = DatabaseHelper.getInstance().getAllCallsFirst();
            endDate = DatabaseHelper.getInstance().getAllCallsLast();
            runOnUiThread(() -> {
                layoutSearch.setVisibility(View.VISIBLE);
                txtArchive.setText(getString(R.string.current));
                txtStartDate.setText(dateFormat.format(new Date(startDate)));
                txtEndDate.setText(dateFormat.format(new Date(endDate)));
            });
            callsList = DatabaseHelper.getInstance().getAllCallsWithDate(startDate, endDate);
        }
        try {
            Collections.sort(callsList, (o1, o2) -> {
                Date date1 = new Date(Long.parseLong(o1.getCallDate()));
                Date date2 = new Date(Long.parseLong(o2.getCallDate()));
                return date1.compareTo(date2);
            });
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        Collections.reverse(callsList);
        callsAdapter = new CallsAdapter();
        rViewCall.setAdapter(callsAdapter);
        if (!callsList.isEmpty()) {
            layoutView.setVisibility(View.VISIBLE);
            cardEmpty.setVisibility(View.GONE);
            fabRemove.setVisibility(View.VISIBLE);
            if (selectListOption == 0) {
                if (MyUtils.getInstance().callsCount > 500) {
                    cardArchive.setVisibility(View.VISIBLE);
                } else {
                    cardArchive.setVisibility(View.GONE);
                }
            } else {
                cardArchive.setVisibility(View.VISIBLE);
            }

        } else {
            layoutView.setVisibility(View.GONE);
            cardEmpty.setVisibility(View.VISIBLE);
            fabRemove.setVisibility(View.GONE);
            if (selectListOption == 0) {
                cardArchive.setVisibility(View.GONE);
            } else {
                cardArchive.setVisibility(View.VISIBLE);
            }
        }
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
    public void refreshCalls() {
        runOnUiThread(() -> setList(0));
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(RecentCalls.this);
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();
        if (id == R.id.img_back) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            finish();
        } else if (id == R.id.fab_remove_all_calls) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            if (clickable) {
                clickable = false;
                if (callsAdapter != null && callsAdapter.getItemCount() != 0) {
                    clickDialog = true;
                    deleteDialog = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_yes_no);
                    TextView customText = deleteDialog.findViewById(R.id.customText);
                    customText.setText(getString(R.string.delete_show_records));
                    Button yes = deleteDialog.findViewById(R.id.btnYes);
                    yes.setOnClickListener(view -> {
                        MyUtils.getInstance().setViewClickTimeout(view, 250);
                        if (clickDialog) {
                            clickDialog = false;
                            customText.setText(getString(R.string.processing));
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    try {
                                        DatabaseHelper.getInstance().deleteCalls(callsList);
                                    } catch (Exception e) {
                                        Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                                    } finally {
                                        runOnUiThread(() -> {
                                            setList(select);
                                            deleteDialog.dismiss();
                                        });

                                    }

                                }
                            }.start();
                        }
                    });
                    Button no = deleteDialog.findViewById(R.id.btnNo);
                    no.setOnClickListener(view -> {
                        MyUtils.getInstance().setViewClickTimeout(view, 250);
                        if (clickDialog) {
                            deleteDialog.dismiss();
                        }
                    });
                    deleteDialog.setOnDismissListener(dialog -> {
                        clickDialog = true;
                        clickable = true;
                    });
                    deleteDialog.show();
                } else {
                    clickable = true;
                }
            }

        } else if (id == R.id.card_archive) {
            if (select == 0) {
                setList(1);
            } else {
                setList(0);
            }

        } else if (id == R.id.img_search) {
            if (clickable) {
                clickable = false;
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        ArrayList<CallModel> callListSearch = new ArrayList<>();
                        try {
                            callListSearch = DatabaseHelper.getInstance().getAllCallsWithDate(startDate, endDate);
                        } catch (Exception e) {
                            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                        } finally {
                            if (!callListSearch.isEmpty()) {
                                callsList = callListSearch;
                                try {
                                    Collections.sort(callsList, (o1, o2) -> {
                                        Date date1 = new Date(Long.parseLong(o1.getCallDate()));
                                        Date date2 = new Date(Long.parseLong(o2.getCallDate()));
                                        return date1.compareTo(date2);
                                    });
                                } catch (Exception e) {
                                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                                }
                                Collections.reverse(callsList);
                                runOnUiThread(() -> {
                                    callsAdapter = new CallsAdapter();
                                    rViewCall.setAdapter(callsAdapter);
                                });

                            } else {
                                runOnUiThread(() -> Toast.makeText(RecentCalls.this, getString(R.string.not_found), Toast.LENGTH_LONG).show());

                            }
                            clickable = true;
                        }
                    }
                }.start();
            }
        } else if (id == R.id.card_start_date) {
            startDateSelect = true;
            showDate();
        } else if (id == R.id.card_end_date) {
            startDateSelect = false;
            showDate();
        }
    }

    public void showDate() {
        dateDialog = new DatePickerDialog(this, DatePickerDialog.THEME_HOLO_DARK, this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dateDialog.show();
        View decorView = dateDialog.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void displayDialog(String text) {
        final Dialog dialog = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_ok);
        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialog.findViewById(R.id.btnOk);
        ok.setOnClickListener(view -> {
            if (dialog.isShowing()) {
                MyUtils.getInstance().setViewClickTimeout(view, 250);
                dialog.dismiss();
            }
        });
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) dialog.dismiss();
        }, 2000);

        dialog.show();
    }


    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        if (startDateSelect) {
            calendar.set(year, month, dayOfMonth, 0, 0);
            Date date = calendar.getTime();
            startDate = date.getTime();
            txtStartDate.setText(dateFormat.format(date));
        } else {
            calendar.set(year, month, dayOfMonth, 23, 59);
            Date date = calendar.getTime();
            endDate = date.getTime();
            txtEndDate.setText(dateFormat.format(date));
        }

    }

    public class CallsAdapter extends RecyclerView.Adapter<CallsAdapter.MyViewHolder> {

        private View view;

        @NonNull
        @Override
        public CallsAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calls_adapter_item, parent, false);

            return new CallsAdapter.MyViewHolder();
        }




        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            final CallModel callModel = callsList.get(position);
            File f = null;
            if (!callModel.getCallType().equals("ROOM")) {
                if (callModel.getCallImgPath() != null) {
                    f = new File(callModel.getCallImgPath());
                }
            }


            String name;
            String remoteIp;
            String info;

            SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault());
            if (callModel.getCallDate() != null) {
                Date date = new Date(Long.parseLong(callModel.getCallDate()));
                String formattedTime = df.format(date);
                holder.txtDate.setText(formattedTime);
            } else {
                holder.txtDate.setText("00 000 0000 00:00:00");
            }
            DeviceModel device;
            if (callModel.getCallType().equals("DOOR")) {
                if (callModel.getCallState().equals("Outgoing") && callModel.getCallData().equals("Connected")) {
                    device = DeviceController.getInstance().getDoorWithMac(callModel.getCallTo());
                    holder.imgDirection.setImageResource(R.drawable.ic_call_outgoing);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.green_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    info = getResources().getString(R.string.connected);
                } else if (callModel.getCallState().equals("Outgoing")) {
                    device = DeviceController.getInstance().getDoorWithMac(callModel.getCallTo());
                    holder.imgDirection.setImageResource(R.drawable.ic_call_outgoing);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.red_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    info = getResources().getString(R.string.not_connected);
                } else if (callModel.getCallState().equals("Answered") && callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress())) {
                    device = DeviceController.getInstance().getDoorWithMac(callModel.getCallFrom());
                    holder.imgDirection.setImageResource(R.drawable.ic_call_incoming);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.green_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    info = getResources().getString(R.string.answered);
                } else if (callModel.getCallState().equals("Answered")) {
                    device = DeviceController.getInstance().getDoorWithMac(callModel.getCallFrom());
                    holder.imgDirection.setImageResource(R.drawable.ic_call_other);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.green_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    info = String.format(getResources().getString(R.string.call_answered), callModel.getCallData(), callModel.getCallerName());
                } else if (callModel.getCallState().equals("Declined") && callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress())) {
                    device = DeviceController.getInstance().getDoorWithMac(callModel.getCallFrom());
                    holder.imgDirection.setImageResource(R.drawable.ic_call_incoming);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.red_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    info = getResources().getString(R.string.declined);
                } else if (callModel.getCallState().equals("Declined")) {
                    device = DeviceController.getInstance().getDoorWithMac(callModel.getCallFrom());
                    holder.imgDirection.setImageResource(R.drawable.ic_call_other);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.red_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    info = String.format(getResources().getString(R.string.call_declined), callModel.getCallData(), callModel.getCallerName());
                } else {
                    device = DeviceController.getInstance().getDoorWithMac(callModel.getCallFrom());
                    holder.imgDirection.setImageResource(R.drawable.ic_call_incoming);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.peru), android.graphics.PorterDuff.Mode.SRC_IN);
                    info = String.format(getResources().getString(R.string.call_missed), callModel.getCallerName());
                }
            } else {
                if (callModel.getCallState().equals("Outgoing") && callModel.getCallData().equals("Connected")) {
                    holder.imgDirection.setImageResource(R.drawable.ic_call_outgoing);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.green_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    device = DeviceController.getInstance().getRoomWithMac(callModel.getCallTo());
                    info = getResources().getString(R.string.connected);
                } else if (callModel.getCallState().equals("Outgoing")) {
                    holder.imgDirection.setImageResource(R.drawable.ic_call_outgoing);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.red_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    device = DeviceController.getInstance().getRoomWithMac(callModel.getCallTo());
                    info = getResources().getString(R.string.not_connected);
                } else if (callModel.getCallState().equals("Incoming") && callModel.getCallData().equals("Connected")) {
                    holder.imgDirection.setImageResource(R.drawable.ic_call_incoming);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.green_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    device = DeviceController.getInstance().getRoomWithMac(callModel.getCallFrom());
                    info = getResources().getString(R.string.answered);
                } else if (callModel.getCallState().equals("Incoming") && callModel.getCallData().equals("Not connected")) {
                    holder.imgDirection.setImageResource(R.drawable.ic_call_incoming);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.red_new), android.graphics.PorterDuff.Mode.SRC_IN);
                    device = DeviceController.getInstance().getRoomWithMac(callModel.getCallFrom());
                    info = getResources().getString(R.string.declined);
                } else {
                    holder.imgDirection.setImageResource(R.drawable.ic_call_incoming);
                    holder.imgDirection.setColorFilter(ContextCompat.getColor(RecentCalls.this, R.color.peru), android.graphics.PorterDuff.Mode.SRC_IN);
                    device = DeviceController.getInstance().getRoomWithMac(callModel.getCallFrom());
                    info = String.format(getResources().getString(R.string.call_missed), callModel.getCallerName());
                }
            }


            if (device != null) {
                name = device.getName();
                remoteIp = device.getIp();
            } else {
                if (callModel.getCallType().equals("DOOR")) {
                    name = getResources().getString(R.string.item_door);
                } else {
                    name = getResources().getString(R.string.item_room);
                }
                remoteIp = "";
            }


            holder.txtInfo.setText(info);
            holder.txtName.setText(name);

            final File fX = f;
            if (callModel.getCallType().equals("DOOR")) {
                if (f != null && f.exists()) {
                    Glide.with(RecentCalls.this).load(callModel.getCallImgPath()).fitCenter().into(holder.imgPhoto);
                } else {
                    holder.imgPhoto.setImageResource(R.drawable.ic_outdoor);
                }
                holder.cardItem.setOnClickListener(v -> {
                    if (clickable) {
                        clickable = false;
                        MyUtils.getInstance().setViewClickTimeout(v, 250);
                        final Dialog dialogPicture = MyUtils.getInstance().dialogPublic(RecentCalls.this, R.layout.dialog_call_image);
                        ConstraintLayout main = dialogPicture.findViewById(R.id.dialog_root);
                        ImageView img = dialogPicture.findViewById(R.id.img);
                        Button btnOk = dialogPicture.findViewById(R.id.btnOk);
                        btnOk.setOnClickListener(view -> dialogPicture.dismiss());
                        if (fX != null && fX.exists()) {
                            try {
                                Glide.with(RecentCalls.this).load(new File(callModel.getCallImgPath())).into(img);
                            } catch (Exception e) {
                                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                            }
                        }
                        main.setOnClickListener(v1 -> dialogPicture.dismiss());
                        dialogPicture.show();
                        clickable = true;
                    }

                });
            } else {
                try {
                    holder.imgPhoto.setImageResource(R.drawable.ic_avatar);
                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));

                }
            }

            final String ip = remoteIp;
            holder.imgCall.setOnClickListener(v -> {
                if (clickable) {
                    clickable = false;
                    MyUtils.getInstance().setViewClickTimeout(v, 250);
                    if (ip != null && !ip.isEmpty()) {
                        if (!MyUtils.getInstance().ethernetState || MyUtils.getInstance().getIpAddress().equals("192.168.256.256")) {
                            displayDialog(getString(R.string.error_network_unreachable));
                        } else {
                            Intent i;
                            if (!callModel.getCallType().equals("ROOM")) {
                                i = new Intent(RecentCalls.this, OutGoingDoorConnected.class);
                            } else {
                                i = new Intent(RecentCalls.this, OutGoingRoomCalling.class);
                            }
                            i.putExtra("remote_ip", ip);
                            startActivity(i);
                        }
                    } else {
                        displayDialog(getString(R.string.cannot_make_call));
                    }
                    clickable = true;
                }
            });

            holder.delete.setOnClickListener(v -> {
                if (clickable) {
                    if (!callsList.isEmpty()) {
                        clickable = false;
                        MyUtils.getInstance().setViewClickTimeout(v, 250);
                        DatabaseHelper.getInstance().deleteCall(callModel.getCallId());
                        notifyItemRemoved(position);
                        setList(select);
                        if (fX != null && fX.exists()) {
                            boolean res = fX.delete();
                            Logger.d("file removing res = " + res);
                        } else {
                            Logger.d("file not exist");
                        }
                    }

                }
            });
            if (!callModel.isCallReadState()) {
                if (MyUtils.getInstance().darkTheme) {
                    holder.cardItem.setCardBackgroundColor(getColor(R.color.color9));
                } else {
                    holder.cardItem.setCardBackgroundColor(getColor(R.color.color8));
                }
                callModel.setCallReadState(true);
                Animation animation = AnimationUtils.loadAnimation(RecentCalls.this, R.anim.vibrate);
                animation.setRepeatCount(1);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (MyUtils.getInstance().darkTheme) {
                            holder.cardItem.setCardBackgroundColor(getColor(R.color.black));
                        } else {
                            holder.cardItem.setCardBackgroundColor(getColor(R.color.white));
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                holder.itemView.startAnimation(animation);
                callsList.set(position, callModel);
            }
        }

        @Override
        public int getItemCount() {

            return callsList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView delete;
            ImageView imgCall;
            ImageView imgPhoto;
            TextView txtName;
            TextView txtDate;
            TextView txtInfo;
            CardView cardItem;
            ImageView imgDirection;

            public MyViewHolder() {
                super(view);
                delete = view.findViewById(R.id.img_delete);
                imgCall = view.findViewById(R.id.img_call);
                imgPhoto = view.findViewById(R.id.img_photo);
                txtInfo = view.findViewById(R.id.txt_state);
                txtName = view.findViewById(R.id.txt_name);
                txtDate = view.findViewById(R.id.txt_date);
                cardItem = view.findViewById(R.id.card_item);
                imgDirection = view.findViewById(R.id.img_direction);
            }
        }


    }
}
