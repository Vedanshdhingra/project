package com.example.project;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes;
    private List<Note> filteredNotes;
    private OnNoteClickListener listener;
    private boolean showOnlySecret = false;

    public interface OnNoteClickListener {
        void onNoteClick(Note note, int position);
    }

    public NotesAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.filteredNotes = new ArrayList<>(notes);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = filteredNotes.get(position);

        if (note.isSecret && !note.sessionUnlocked) {
            holder.lockedView.setVisibility(View.VISIBLE);
            holder.unlockedView.setVisibility(View.GONE);
            holder.container.setBackgroundResource(R.drawable.glass_card_bg);
            holder.lockedTitle.setText(note.title == null || note.title.isEmpty() ? "Secret note" : note.title);
        } else {
            holder.lockedView.setVisibility(View.GONE);
            holder.unlockedView.setVisibility(View.VISIBLE);
            
            if (note.isSecret) {
                holder.container.setBackgroundResource(R.drawable.glass_card_unlocked_bg);
                holder.noteTitle.setTextColor(0xFF00E5FF); // Cyan for secret
            } else {
                holder.container.setBackgroundResource(R.drawable.glass_card_bg);
                holder.noteTitle.setTextColor(0xFFFFFFFF);
            }

            holder.noteTitle.setText(note.title == null || note.title.isEmpty() ? "Untilted Note" : note.title);
            holder.noteContent.setText(note.content);

            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(note.lastModified);
            String date = DateFormat.format("hh:mm a", cal).toString();
            holder.noteTime.setText("Updated " + date);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int originalPos = notes.indexOf(note);
                listener.onNoteClick(note, originalPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredNotes.size();
    }

    public void filter(String query) {
        filteredNotes.clear();
        for (Note note : notes) {
            boolean matchesQuery = query.isEmpty() || 
                                   note.content.toLowerCase().contains(query.toLowerCase()) || 
                                   (note.title != null && note.title.toLowerCase().contains(query.toLowerCase()));
            
            boolean matchesSecretFilter = !showOnlySecret || note.isSecret;
            
            if (matchesQuery && matchesSecretFilter) {
                filteredNotes.add(note);
            }
        }
        notifyDataSetChanged();
    }

    public void setShowOnlySecret(boolean onlySecret) {
        this.showOnlySecret = onlySecret;
        filter("");
    }

    public void sortAndNotify() {
        Collections.sort(notes, (n1, n2) -> {
            if (n1.isPinned != n2.isPinned) return n1.isPinned ? -1 : 1;
            return Long.compare(n2.lastModified, n1.lastModified);
        });
        filter("");
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle, noteContent, noteTime, lockedTitle;
        View lockedView, unlockedView, container;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.noteContainer);
            lockedView = itemView.findViewById(R.id.lockedView);
            unlockedView = itemView.findViewById(R.id.unlockedView);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteContent = itemView.findViewById(R.id.noteContent);
            noteTime = itemView.findViewById(R.id.noteTime);
            lockedTitle = itemView.findViewById(R.id.lockedTitle);
        }
    }
}