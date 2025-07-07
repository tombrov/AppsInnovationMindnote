package com.example.mindnote;

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

    public interface OnNoteClickListener {
        void onNoteClick(JournalEntry entry);
    }

    private final List<JournalEntry> entries;
    private final OnNoteClickListener listener;

    public NotesAdapter(List<JournalEntry> entries, OnNoteClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {

        private final TextView noteText;
        private final TextView dateText;
        private final TextView moodIcon;
        private final TextView tagsText;
        private final ImageView entryImage;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteText = itemView.findViewById(R.id.noteText);
            dateText = itemView.findViewById(R.id.dateText);
            moodIcon = itemView.findViewById(R.id.moodIcon);
            tagsText = itemView.findViewById(R.id.tagsText);
            entryImage = itemView.findViewById(R.id.entryImageView);
        }

        public void bind(JournalEntry entry) {
            noteText.setText(entry.getNote());
            dateText.setText(entry.getShortDate());
            moodIcon.setText(entry.getMoodEmoji());

            if (entry.getTags() != null && !entry.getTags().isEmpty()) {
                tagsText.setText(entry.getTagsAsString());
                tagsText.setVisibility(View.VISIBLE);
            } else {
                tagsText.setVisibility(View.GONE);
            }

            if (entry.getImagePath() != null && !entry.getImagePath().isEmpty()) {
                entryImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(entry.getImagePath()).into(entryImage);
            } else {
                entryImage.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onNoteClick(entry));
        }
    }
}
