package com.alfanar.villaroom.activities.cameras;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.R;

public class CameraViewHolder extends RecyclerView.ViewHolder {
    TextView txtAddress;
    TextView txtName;


    public CameraViewHolder(View itemView) {
        super(itemView);
        txtAddress = itemView.findViewById(R.id.txtCameraAddress);
        txtName = itemView.findViewById(R.id.txtCameraName);

    }
}
