package com.example.mindnote;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<JournalEntry> entries;
    private final Context context;

    public NotesAdapter(Context context) {
        this.context = context;
    }

    public void setEntries(List<JournalEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        JournalEntry entry = entries.get(position);

        holder.noteText.setText(entry.getNote() != null ? entry.getNote() : "No content");
        holder.dateText.setText(entry.getFormattedDate());

        if (entry.getImagePath() != null && !JournalDataManager.isDemoImage(entry.getImagePath())) {
            Glide.with(context).load(entry.getImagePath()).into(holder.entryImage);
            holder.entryImage.setVisibility(View.VISIBLE);
        } else {
            holder.entryImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, JournalActivity.class);
            intent.putExtra("entryId", entry.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteText, dateText;
        ImageView entryImage;

        NoteViewHolder(View itemView) {
            super(itemView);
            noteText = itemView.findViewById(R.id.notePreviewText);
            dateText = itemView.findViewById(R.id.dateText);
            entryImage = itemView.findViewById(R.id.entryImage);
        }
    }
}
