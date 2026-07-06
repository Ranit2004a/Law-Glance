package com.example.ywinked;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RightsAdapter extends RecyclerView.Adapter<RightsAdapter.RightViewHolder> {

    private final List<RightModel> rightsList;
    private int expandedPosition = -1; // Only one expanded at a time

    public RightsAdapter(List<RightModel> rightsList) {
        this.rightsList = rightsList;
    }

    @NonNull
    @Override
    public RightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_right, parent, false);
        return new RightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RightViewHolder holder, int position) {
        RightModel right = rightsList.get(position);

        // Set title
        holder.title.setText(right.title);

        // Check if this item is expanded
        boolean isExpanded = position == expandedPosition;
        holder.detailsLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.expandArrow.setRotation(isExpanded ? 180f : 0f);

        // Set details text
        holder.description.setText("Description: " + right.description);
        holder.cause.setText("Cause: " + right.cause);
        holder.punishment.setText("Punishment: " + right.punishment);

        // Handle click for expand/collapse
        holder.itemView.setOnClickListener(v -> {
            expandedPosition = isExpanded ? -1 : position;
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return rightsList.size();
    }

    public static class RightViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, cause, punishment;
        LinearLayout detailsLayout;
        ImageView expandArrow;

        public RightViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.rightTitle);
            description = itemView.findViewById(R.id.rightDescription);
            cause = itemView.findViewById(R.id.rightCause);
            punishment = itemView.findViewById(R.id.rightPunishment);
            detailsLayout = itemView.findViewById(R.id.detailsLayout);
            expandArrow = itemView.findViewById(R.id.expandArrow);
        }
    }
}

