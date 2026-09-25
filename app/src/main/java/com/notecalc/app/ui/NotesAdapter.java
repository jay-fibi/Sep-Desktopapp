package com.notecalc.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.notecalc.app.R;
import com.notecalc.app.data.NoteItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(NoteItem note);
        void onNoteDelete(NoteItem note);
    }

    private final List<NoteItem> notes;
    private final OnNoteClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    public NotesAdapter(List<NoteItem> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoteItem note = notes.get(position);
        holder.tvTitle.setText(note.getTitle().isEmpty() ? "Untitled Note" : note.getTitle());
        holder.tvPreview.setText(note.getContent().isEmpty() ? "(Empty)" : note.getContent().replace("\n", "  |  "));
        holder.tvDate.setText(dateFormat.format(new Date(note.getTimestamp())));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoteClick(note);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onNoteDelete(note);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvPreview;
        TextView tvDate;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvPreview = itemView.findViewById(R.id.tvNotePreview);
            tvDate = itemView.findViewById(R.id.tvNoteDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteNote);
        }
    }
}
