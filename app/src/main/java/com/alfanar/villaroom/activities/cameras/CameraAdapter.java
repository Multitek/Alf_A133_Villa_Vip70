package com.alfanar.villaroom.activities.cameras;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.models.Camera2Model;
import com.alfanar.villaroom.util.MyUtils;

import java.util.ArrayList;

public class CameraAdapter extends RecyclerView.Adapter<CameraViewHolder> {

    private final Activity ac;
    public ArrayList<Camera2Model> list;

    public CameraAdapter(Activity ac, ArrayList<Camera2Model> list) {
        this.list = list;
        this.ac = ac;
    }

    @NonNull
    @Override
    public CameraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_camera_devices, parent, false);
        return new CameraViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CameraViewHolder holder, int position) {
        Camera2Model device = list.get(position);
        holder.txtAddress.setText(device.getIp());
        holder.txtName.setText(device.getName());
        holder.itemView.setOnLongClickListener(v -> {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            IpCameras.instance.displayDialogEdit(position);
            return false;

        });
        holder.itemView.setOnClickListener(v -> {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            Intent i = new Intent(ac, CameraPlayer.class);
            i.putExtra("index", position);
            ac.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


}
