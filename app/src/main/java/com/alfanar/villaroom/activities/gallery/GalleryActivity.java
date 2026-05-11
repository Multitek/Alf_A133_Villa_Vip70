package com.alfanar.villaroom.activities.gallery;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.ImagesModel;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.bumptech.glide.Glide;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView rv;
    private CardView cardEmpty;
    private Dialog dialog, dialogPicture;
    private ArrayList<ImagesModel> imgList;
    private FloatingActionButton fabDelete;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        getWindow().setBackgroundDrawableResource(MyUtils.getInstance().getWindowBackground(-1));
        MainTimeout.getInstance().setTimeout(60 * 1000);
        ImageView back = findViewById(R.id.img_back);
        back.setOnClickListener(this);
        ImageView imgOther = findViewById(R.id.img_other);
        imgOther.setVisibility(View.GONE);
        TextView txtTitle = findViewById(R.id.text_title);
        txtTitle.setText(getResources().getString(R.string.gallery));
        rv = findViewById(R.id.gallery_list);
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.CENTER);
        layoutManager.setAlignItems(AlignItems.CENTER);
        rv.setLayoutManager(layoutManager);
        LayoutAnimationController animationRecyclerView = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_from_left);
        rv.setLayoutAnimation(animationRecyclerView);
        cardEmpty = findViewById(R.id.card_empty);
        fabDelete = findViewById(R.id.fab_delete);
        fabDelete.setOnClickListener(this);
        setGalleryFiles();
        MyUtils.getInstance().hideNavigation(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        if (dialogPicture != null && dialogPicture.isShowing()) {
            dialogPicture.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
        int id = v.getId();
        if (id == R.id.img_back) {
            finish();
        } else if (id == R.id.fab_delete) {
            fabDelete.setClickable(false);
            if (imgList.size() != 0) {
                dialog = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_yes_no);
                TextView customText = dialog.findViewById(R.id.customText);
                customText.setText(getString(R.string.all_gallery_deleted));
                Button yes = dialog.findViewById(R.id.btnYes);
                yes.setClickable(true);
                yes.setOnClickListener(view -> {
                    yes.setClickable(false);
                    view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                    DatabaseHelper.getInstance().deleteAllImages();
                    setGalleryFiles();
                    dialog.dismiss();
                });
                Button no = dialog.findViewById(R.id.btnNo);
                no.setOnClickListener(view -> {
                    view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                    dialog.dismiss();
                });

                dialog.setOnDismissListener(dialogInterface -> fabDelete.setClickable(true));
                dialog.show();
            }

        }
    }

    private void setGalleryFiles() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                imgList = DatabaseHelper.getInstance().getAllImages();
                try {
                    Collections.sort(imgList, (o1, o2) -> {
                        Date date1 = new Date(Long.parseLong(o1.getTime()));
                        Date date2 = new Date(Long.parseLong(o2.getTime()));
                        return date1.compareTo(date2);
                    });
                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
                Collections.reverse(imgList);
                runOnUiThread(() -> {
                    GalleryRVAdapter galleryAdapter;
                    galleryAdapter = new GalleryRVAdapter();
                    rv.setAdapter(galleryAdapter);
                    if (!imgList.isEmpty()) {
                        cardEmpty.setVisibility(View.GONE);
                        fabDelete.setVisibility(View.VISIBLE);
                        fabDelete.setClickable(true);
                    } else {
                        cardEmpty.setVisibility(View.VISIBLE);
                        fabDelete.setVisibility(View.GONE);
                    }
                });
            }
        }.start();
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
        MyUtils.getInstance().hideNavigation(this);
    }

    private class GalleryRVAdapter extends RecyclerView.Adapter<GalleryRVAdapter.MyViewHolder> {

        boolean clickable = true;

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_images_adapter_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
            // darkTheme = true;
            final ImagesModel selectedProduct = imgList.get(position);
            DeviceModel device = DeviceController.getInstance().getDoorWithMac(selectedProduct.getMacAddress());
            String name;
            if (device != null) {
                name = device.getName();
                if (name.equals("")) {
                    name = String.valueOf(position);
                }
            } else {
                name = getResources().getString(R.string.item_door);
            }

            String dateString = selectedProduct.getTime();

            File f = null;
            if (selectedProduct.getPath() != null) {
                f = new File(selectedProduct.getPath());
            }

            if (dateString.equals("")) {
                holder.txtDate.setText("00 000 0000 00:00:00");
            } else {
                SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault());
                Date date = new Date(Long.parseLong(dateString));
                String formattedTime = df.format(date);
                holder.txtDate.setText(formattedTime);
            }
            holder.txtName.setText(name);
            if (f != null && f.exists()) {
                Glide.with(GalleryActivity.this).load(selectedProduct.getPath()).fitCenter().into(holder.img);
            }
            final File fX = f;
            holder.itemView.setOnClickListener(v -> {
                if (clickable) {
                    clickable = false;
                    v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                    dialogPicture = MyUtils.getInstance().dialogPublic(GalleryActivity.this, R.layout.dialog_gallery_detail);
                    ImageView img = dialogPicture.findViewById(R.id.img);
                    if (fX != null && fX.exists()) {
                        try {
                            Glide.with(GalleryActivity.this).load(new File(selectedProduct.getPath())).into(img);
                        } catch (Exception e) {
                            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                        }
                    }
                    Button btnRemove = dialogPicture.findViewById(R.id.btnDelete);
                    Button btnCancel = dialogPicture.findViewById(R.id.btnCancel);
                    btnCancel.setOnClickListener(view -> {
                        view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                        clickable = true;
                        dialogPicture.dismiss();
                    });
                    btnRemove.setOnClickListener(view -> {
                        view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                        DatabaseHelper.getInstance().deleteImages(selectedProduct.getTime());
                        setGalleryFiles();
                        clickable = true;
                        dialogPicture.dismiss();
                    });

                    dialogPicture.show();
                }

            });
        }

        @Override
        public int getItemCount() {
            return imgList.size();
        }


        class MyViewHolder extends RecyclerView.ViewHolder {
            private final TextView txtName;
            private final TextView txtDate;
            private final ImageView img;


            private MyViewHolder(View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.imgPhotox);
                txtName = itemView.findViewById(R.id.txtName);
                txtDate = itemView.findViewById(R.id.txtTime);
            }
        }

    }
}
