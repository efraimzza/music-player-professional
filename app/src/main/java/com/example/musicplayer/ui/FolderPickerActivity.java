package com.example.musicplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.example.musicplayer.R;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FolderPickerActivity extends Activity {

    private TextView currentPathText;
    private ListView listView;
    private Button selectButton;
    private File currentDir;
    private FolderAdapter adapter;
    private List<File> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_picker);
        currentPathText = findViewById(R.id.current_path);
        listView = findViewById(R.id.folder_list);
        selectButton = findViewById(R.id.select_button);
        adapter = new FolderAdapter();
        listView.setAdapter(adapter);

        // Start at / (or external storage)
        currentDir = Environment.getExternalStorageDirectory();
        refreshList();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    File clicked = items.get(position);
                    if (clicked.isDirectory()) {
                        currentDir = clicked;
                        refreshList();
                    }
                }
            });
        selectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent result = new Intent();
                    result.putExtra("folder_path", currentDir.getAbsolutePath());
                    setResult(RESULT_OK, result);
                    finish();
                }
            });
    }

    private void refreshList() {
        currentPathText.setText(currentDir.getAbsolutePath());
        File[] files = currentDir.listFiles();
        items.clear();
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    }
                });
            for (File f : files) {
                if (f.isDirectory() && f.canRead()) {
                    items.add(f);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    class FolderAdapter extends android.widget.BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int pos) { return items.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView text = convertView.findViewById(android.R.id.text1);
            text.setText(items.get(position).getName());
            return convertView;
        }
    }
}
