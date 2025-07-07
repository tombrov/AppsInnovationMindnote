package com.example.mindnote;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class EntryDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_detail);

        TextView titleView = findViewById(R.id.detail_title);
        TextView contentView = findViewById(R.id.detail_content);
        TextView timestampView = findViewById(R.id.detail_timestamp);
        ImageView imageView = findViewById(R.id.detail_image);

        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String timestamp = getIntent().getStringExtra("timestamp");

        if (title != null) titleView.setText(title);
        if (content != null) contentView.setText(content);
        if (timestamp != null) timestampView.setText(timestamp);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(imageView);
        } else {
            imageView.setVisibility(ImageView.GONE);
        }
    }
}
