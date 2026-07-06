package com.example.ywinked;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {

    // Interface for click handling
    public interface OnItemClickListener {
        void onItemClick(String query);
    }

    private List<SuggestionModel> list;
    private OnItemClickListener listener;

    // UPDATED constructor
    public SuggestionAdapter(List<SuggestionModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String text = list.get(position).getTitle();
        holder.text.setText(text);

        // CLICK EVENT
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(text);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tvSuggestion);
        }
    }
}


