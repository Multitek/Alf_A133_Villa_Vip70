package com.alfanar.villaroom.adapters;

import static android.widget.Toast.LENGTH_LONG;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.activities.DoorsAndRoomsActivity;
import com.alfanar.villaroom.activities.intercom.OutGoingDoorConnected;
import com.alfanar.villaroom.activities.intercom.OutGoingRoomCalling;
import com.alfanar.villaroom.databinding.DeviceBinding;
import com.alfanar.villaroom.databinding.DialogChangeDoorIpBinding;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.GeneralModel;
import com.alfanar.villaroom.sockets.MulticastPublisher;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.MyUtils;
import com.google.gson.Gson;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private final Activity activity;
    ArrayList<DeviceModel> list;
    private Dialog dialogIp;
    public DeviceAdapter(ArrayList<DeviceModel> li, Activity activity) {
        this.list = li;
        this.activity=activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DeviceBinding binding = DeviceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceModel device = list.get(holder.getAdapterPosition());
        holder.binding.txtAdpDeviceName.setCompoundDrawablePadding(MyUtils.getInstance().dpToPx(5));

        if (device.getType().equals("ROOM")) {
            holder.binding.txtAdpDeviceName.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_rooms_01, 0, 0);
            holder.binding.txtAdpDeviceName.setCompoundDrawablePadding(MyUtils.getInstance().dpToPx(5));
            holder.binding.txtAdpDeviceName.setText(device.getName());



            holder.binding.item.setOnClickListener(v -> {
                holder.binding.item.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                String ipTo = device.getIp();
                if (ipTo.equals("192.168.256.256")) {
                    DoorsAndRoomsActivity.getInstance().displayDialog(activity.getResources().getString(R.string.error_network_unreachable));
                } else if (!device.isState()) {
                    DoorsAndRoomsActivity.getInstance().displayDialog(activity.getResources().getString(R.string.cannot_make_call));
                } else if (ipTo.equals(MyUtils.getInstance().getIpAddress())) {
                    DoorsAndRoomsActivity.getInstance().displayDialog(activity.getResources().getString(R.string.can_not_call_yourself));
                } else {
                    if (!MyUtils.getInstance().ethernetState || MyUtils.getInstance().getIpAddress().equals("192.168.256.256")) {
                        DoorsAndRoomsActivity.getInstance().displayDialog(activity.getResources().getString(R.string.error_network_unreachable));
                    } else {
                        if(sameFirst3(MyUtils.getInstance().getIpAddress(),device.getIp())){
                            Intent i = new Intent(activity, OutGoingRoomCalling.class);
                            i.putExtra("remote_ip", ipTo);
                            activity.startActivity(i);
                        }else{
                            DoorsAndRoomsActivity.getInstance().displayDialog(activity.getResources().getString(R.string.diferrent_network));
                        }

                    }
                }
            });
        } else if (device.getType().equals("DOOR")) {
            holder.binding.txtAdpDeviceName.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_door, 0, 0);
            holder.binding.txtAdpDeviceName.setCompoundDrawablePadding(0);
            holder.binding.txtAdpDeviceName.setText(device.getName());


            holder.binding.item.setOnLongClickListener(v -> {
                displayModeDialog(device.getMac());
                return false;
            });


            holder.binding.item.setOnClickListener(v -> {
                holder.binding.item.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                if (!device.isState()) {
                    DoorsAndRoomsActivity.getInstance().displayDialog(activity.getResources().getString(R.string.cannot_make_call));
                } else {
                    if (!MyUtils.getInstance().ethernetState || MyUtils.getInstance().getIpAddress().equals("192.168.256.256")) {
                        DoorsAndRoomsActivity.getInstance().displayDialog(activity.getResources().getString(R.string.error_network_unreachable));
                    } else {
                        if(sameFirst3(MyUtils.getInstance().getIpAddress(),device.getIp())){
                            Intent i = new Intent(activity, OutGoingDoorConnected.class);
                            i.putExtra("remote_ip", device.getIp());
                            activity.startActivity(i);
                        }else{
                            DoorsAndRoomsActivity.getInstance().displayDialog(activity.getResources().getString(R.string.diferrent_network));
                        }

                    }
                }
            });
        }


    }

    private void displayModeDialog(String ip) {
        if(dialogIp!=null && dialogIp.isShowing()){
            dialogIp.dismiss();
        }
        DialogChangeDoorIpBinding dialogChangeDoorIpBinding = DialogChangeDoorIpBinding.inflate(activity.getLayoutInflater());

        dialogIp = MyUtils.getInstance().dialogPublic2(activity);
        dialogIp.setContentView(dialogChangeDoorIpBinding.getRoot());



        dialogChangeDoorIpBinding.btnStatic.setOnClickListener(v -> {
            MyUtils.getInstance().setViewClickTimeout(v,500);
             MulticastPublisher.send(new Gson().toJson(new GeneralModel(AppEnums.ODU_SET_STATIC.name(), ip)));
            dialogIp.dismiss();
            Toast.makeText(activity,activity.getResources().getString(R.string.saved),LENGTH_LONG).show();

        });
        dialogChangeDoorIpBinding.btnDhcp.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
             MulticastPublisher.send(new Gson().toJson(new GeneralModel(AppEnums.ODU_SET_DHCP.name(), ip)));
            Toast.makeText(activity,activity.getResources().getString(R.string.saved),LENGTH_LONG).show();
            dialogIp.dismiss();
        });

        dialogChangeDoorIpBinding.btnCancel.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            dialogIp.dismiss();
        });
        dialogIp.show();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        DeviceBinding binding;

        public ViewHolder(DeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public  boolean sameFirst3(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() < 3 || b.length() < 3) return false;
        return a.regionMatches(0, b, 0, 3);
    }

}
