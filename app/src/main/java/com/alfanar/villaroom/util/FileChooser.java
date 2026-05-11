package com.alfanar.villaroom.util;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.adapters.FileArrayAdapter;
import com.alfanar.villaroom.models.Item;

import java.io.File;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileChooser extends ListActivity {
    private File currentDir;
    private FileArrayAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        MyUtils.getInstance().hideNavigation(this);
        currentDir = new File("/mnt/sdcard/");
        fill(currentDir);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyUtils.getInstance().hideNavigation(this);
    }

    private void fill(File f) {
        File[] dirs = f.listFiles();
        this.setTitle("Current Dir: " + f.getName());
        List<Item> dir = new ArrayList<>();
        List<Item> fls = new ArrayList<>();
        try {
            for (File ff : dirs) {
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if (ff.isDirectory()) {

                    File[] fbuf = ff.listFiles();
                    int buf;
                    if (fbuf != null) {
                        buf = fbuf.length;
                    } else buf = 0;
                    String num_item = String.valueOf(buf);
                    if (buf == 0) num_item = num_item + " item";
                    else num_item = num_item + " items";

                    dir.add(new Item(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), "ic_folder"));
                } else {
                    fls.add(new Item(ff.getName(), ff.length() + " Byte", date_modify, ff.getAbsolutePath(), "ic_file"));
                }
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if (f.getName().equalsIgnoreCase("mnt")) {
            dir.add(0, new Item("**", getResources().getString(R.string.back_to_home), "", "parent", "ic_file_choose"));
        } else {
            dir.add(0, new Item("**", getResources().getString(R.string.back_to_home), "", "parent", "ic_file_choose"));
            dir.add(1, new Item("..", getResources().getString(R.string.parent_directory), "", f.getParent(), "ic_back"));
        }

        adapter = new FileArrayAdapter(FileChooser.this, R.layout.file_view, dir);
        this.setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Item o = adapter.getItem(position);
        if (o.getImage().equalsIgnoreCase("ic_folder") || o.getImage().equalsIgnoreCase("ic_back")) {
            currentDir = new File(o.getPath());
            fill(currentDir);
        } else if (o.getImage().equalsIgnoreCase("ic_file_choose")) {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        } else {
            onFileClick(o);
        }
    }

    private void onFileClick(Item o) {
        String fullPath = currentDir.toString() + "/" + o.getName();
        Intent intent = new Intent();
        intent.putExtra("GetPath", currentDir.toString());
        intent.putExtra("GetFileName", o.getName());
        intent.putExtra("GetFileFullPath", fullPath);
        intent.putExtra("lent", o.getPath());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(FileChooser.this);
    }
}
