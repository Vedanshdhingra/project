package com.example.project;

import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        holder.container.setBackgroundColor(note.color);

        if (note.isSecret) {
            holder.content.setVisibility(View.GONE);
            holder.lockIcon.setVisibility(View.VISIBLE);
            holder.time.setVisibility(View.GONE);
            holder.deadlineText.setVisibility(View.GONE);
            holder.urgentBadge.setVisibility(View.GONE);
        } else {
            holder.content.setVisibility(View.VISIBLE);
            holder.lockIcon.setVisibility(View.GONE);
            holder.time.setVisibility(View.VISIBLE);
            holder.content.setText(note.content);

            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(note.lastModified);
            String date = DateFormat.format("hh:mm a", cal).toString();
            holder.time.setText(date);

            // Deadline Handling
            if (note.deadline != null) {
                holder.deadlineText.setVisibility(View.VISIBLE);
                cal.setTimeInMillis(note.deadline);
                String deadlineStr = DateFormat.format("dd MMM, hh:mm a", cal).toString();
                holder.deadlineText.setText("⏰ " + deadlineStr);
                
                if (note.isUrgent()) {
                    holder.urgentBadge.setVisibility(View.VISIBLE);
                    holder.container.setPadding(30, 30, 30, 30); // Visually "bigger"
                    holder.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Scale up content
                } else {
                    holder.urgentBadge.setVisibility(View.GONE);
                    holder.container.setPadding(20, 20, 20, 20);
                    holder.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                }
            } else {
                holder.deadlineText.setVisibility(View.GONE);
                holder.urgentBadge.setVisibility(View.GONE);
                holder.container.setPadding(20, 20, 20, 20);
                holder.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            }
        }

        // Interactive Depth
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(100).start();
                    v.setElevation(20f);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    v.setElevation(12f);
                    break;
            }
            return false;
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int originalPos = notes.indexOf(note);
                listener.onNoteClick(note, originalPos);
            }
        });

        holder.itemView.setAlpha(note.isPinned ? 1.0f : 0.9f);
    }

    @Override
    public int getItemCount() {
        return filteredNotes.size();
    }

    public void filter(String query) {
        filteredNotes.clear();
        if (query.isEmpty()) {
            filteredNotes.addAll(notes);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Note note : notes) {
                if (note.content.toLowerCase().contains(lowerQuery) || 
                    (note.title != null && note.title.toLowerCase().contains(lowerQuery))) {
                    filteredNotes.add(note);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void sortAndNotify() {
        Collections.sort(notes, (n1, n2) -> {
            // Urgency/Deadline sorting
            if (n1.isUrgent() != n2.isUrgent()) {
                return n1.isUrgent() ? -1 : 1;
            }
            if (n1.isPinned != n2.isPinned) {
                return n1.isPinned ? -1 : 1;
            }
            return Long.compare(n2.lastModified, n1.lastModified);
        });
        filter("");
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView content, time, deadlineText, urgentBadge;
        ImageView lockIcon;
        LinearLayout container;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.noteContent);
            time = itemView.findViewById(R.id.noteTime);
            deadlineText = itemView.findViewById(R.id.deadlineText);
            urgentBadge = itemView.findViewById(R.id.urgentBadge);
            lockIcon = itemView.findViewById(R.id.lockIcon);
            container = itemView.findViewById(R.id.noteContainer);
        }
    }
}