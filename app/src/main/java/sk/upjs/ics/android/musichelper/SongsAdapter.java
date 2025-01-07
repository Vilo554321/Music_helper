package sk.upjs.ics.android.musichelper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SongViewHolder> {
    private final List<String> songs;
    private final OnSongDeleteListener deleteListener;

    public SongsAdapter(List<String> songs, OnSongDeleteListener deleteListener) {
        this.songs = songs;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        String song = songs.get(position);
        holder.songNameTextView.setText(song);
        holder.deleteButton.setOnClickListener(v -> deleteListener.onSongDelete(song));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public interface OnSongDeleteListener {
        void onSongDelete(String song);
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView songNameTextView;
        Button deleteButton;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songNameTextView = itemView.findViewById(R.id.tvSongName);
            deleteButton = itemView.findViewById(R.id.btnDelete);
        }
    }
}
