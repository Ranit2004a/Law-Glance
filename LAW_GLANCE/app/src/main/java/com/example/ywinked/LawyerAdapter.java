package com.example.ywinked;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LawyerAdapter extends RecyclerView.Adapter<LawyerAdapter.ViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(LawyerModel lawyer);
    }

    private final List<LawyerModel> lawyerList;
    private final OnBookClickListener listener;

    public LawyerAdapter(List<LawyerModel> lawyerList, OnBookClickListener listener) {
        this.lawyerList = lawyerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lawyer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LawyerModel lawyer = lawyerList.get(position);
        holder.txtLawyerName.setText(lawyer.name);
        holder.txtSpecialization.setText(lawyer.specialization);
        holder.txtLocation.setText(lawyer.city);
        
        String exp = lawyer.experience;
        if (!exp.toLowerCase().contains("year")) {
            exp = exp + " Years";
        }
        holder.txtExperience.setText(exp);
        holder.txtPrice.setText(lawyer.price);

        // Load base64 image or set default
        if (lawyer.profileImageBase64 != null && !lawyer.profileImageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(lawyer.profileImageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivLawyerPic.setImageBitmap(bitmap);
                holder.ivLawyerPic.setImageTintList(null);
            } catch (Exception e) {
                holder.ivLawyerPic.setImageResource(R.drawable.ic_profile_vector);
            }
        } else {
            holder.ivLawyerPic.setImageResource(R.drawable.ic_profile_vector);
        }

        holder.btnBook.setOnClickListener(v -> listener.onBookClick(lawyer));
    }

    @Override
    public int getItemCount() {
        return lawyerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLawyerPic;
        TextView txtLawyerName, txtSpecialization, txtLocation, txtExperience, txtPrice;
        Button btnBook;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLawyerPic = itemView.findViewById(R.id.ivLawyerPic);
            txtLawyerName = itemView.findViewById(R.id.txtLawyerName);
            txtSpecialization = itemView.findViewById(R.id.txtSpecialization);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtExperience = itemView.findViewById(R.id.txtExperience);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            btnBook = itemView.findViewById(R.id.btnBook);
        }
    }
}
