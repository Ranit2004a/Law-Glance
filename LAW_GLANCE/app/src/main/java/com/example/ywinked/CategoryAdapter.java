package com.example.ywinked;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<CategoryModel> categoryList;
    private final CategoryClickListener listener;

    public interface CategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    public CategoryAdapter(List<CategoryModel> categoryList, CategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryModel category = categoryList.get(position);
        holder.name.setText(category.name);

        // Update background selection
        holder.name.setSelected(category.isSelected);

        holder.itemView.setOnClickListener(v -> {
            // Only one selected at a time
            for (CategoryModel cat : categoryList) cat.isSelected = false;
            category.isSelected = true;
            notifyDataSetChanged();

            listener.onCategoryClick(category.name);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.categoryName);
        }
    }
}


