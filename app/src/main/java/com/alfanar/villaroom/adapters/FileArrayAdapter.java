package com.alfanar.villaroom.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.models.Item;
import com.alfanar.villaroom.util.MyUtils;

import java.util.List;

public class FileArrayAdapter extends ArrayAdapter<Item> {

    private final Context c;
    private final int id;
    private final List<Item> items;

    public FileArrayAdapter(Context context, int textViewResourceId, List<Item> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }

    public Item getItem(int i) {
        return items.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        final Item o = items.get(position);
        if (o != null) {
            TextView t1 = v.findViewById(R.id.TextView01);
            TextView t2 = v.findViewById(R.id.TextView02);
            TextView t3 = v.findViewById(R.id.TextViewDate);
            ImageView image = v.findViewById(R.id.fd_Icon1);
            switch (o.getImage()) {
                case "ic_file_choose":
                    image.setImageResource(R.drawable.ic_file_choose);
                    t1.setTextSize(25);
                    t2.setTextSize(25);
                    break;
                case "ic_back":
                    image.setImageResource(R.drawable.ic_back);
                    image.setPadding(MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(5), MyUtils.getInstance().dpToPx(5), MyUtils.getInstance().dpToPx(5));
                    t1.setTextSize(22);
                    t2.setTextSize(22);
                    break;
                case "ic_file":
                    image.setImageResource(R.drawable.ic_file);
                    image.setPadding(MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10));
                    t1.setTextSize(18);
                    t2.setTextSize(15);
                    break;
                case "ic_folder":
                    image.setImageResource(R.drawable.ic_folder);
                    t1.setTextSize(18);
                    t2.setTextSize(15);
                    break;
            }
            t1.setText(o.getName());
            t2.setText(o.getData());
            t3.setText(o.getDate());
        }
        return v;
    }
}
