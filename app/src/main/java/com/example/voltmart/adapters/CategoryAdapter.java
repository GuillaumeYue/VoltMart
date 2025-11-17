package com.example.voltmart.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.fragments.CategoryFragment;
import com.example.voltmart.model.CategoryModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Picasso;

public class CategoryAdapter extends FirestoreRecyclerAdapter<CategoryModel, CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private AppCompatActivity activity;
    public CategoryAdapter(@NonNull FirestoreRecyclerOptions<CategoryModel> options, Context context){
        super(options);
        this.context = context;
    }
    @Override
    protected void onBindViewHolder(@NonNull CategoryViewHolder categoryViewHolder, int i, @NonNull CategoryModel categoryModel) {
        android.util.Log.d("CategoryAdapter", "onBindViewHolder called for position: " + i);
        
        // Set category name with null check
        String categoryName = categoryModel.getName();
        android.util.Log.d("CategoryAdapter", "Category name: " + categoryName);
        
        if (categoryName != null && !categoryName.isEmpty()) {
            categoryViewHolder.categoryLabel.setText(categoryName);
        } else {
            categoryViewHolder.categoryLabel.setText("Category");
        }
        
        // Load category image using Picasso
        String iconUrl = categoryModel.getIcon();
        android.util.Log.d("CategoryAdapter", "Category icon: " + iconUrl);
        
        if (iconUrl != null && !iconUrl.isEmpty()) {
            Picasso.get().load(iconUrl).into(categoryViewHolder.categoryImage);
        } else {
            // Set a placeholder or default image if icon is missing
            categoryViewHolder.categoryImage.setImageResource(R.drawable.ic_launcher_background);
        }
        
        // Set click listener to navigate to CategoryFragment
        categoryViewHolder.itemView.setOnClickListener(v -> {
            try {
                Context context = v.getContext();
                if (context instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) context;
                    Fragment fragment = new CategoryFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("categoryName", categoryName != null ? categoryName : "Electronics");
                    fragment.setArguments(bundle);
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_frame_layout, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            } catch (Exception e) {
                android.util.Log.e("CategoryAdapter", "Error navigating to category fragment", e);
            }
        });
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        android.util.Log.d("CategoryAdapter", "onCreateViewHolder called");
        Context ctx = context != null ? context : parent.getContext();
        View view = LayoutInflater.from(ctx).inflate(R.layout.item_category_adapter,parent,false);
        try {
            activity = (AppCompatActivity) view.getContext();
        } catch (ClassCastException e) {
            android.util.Log.e("CategoryAdapter", "Context is not an AppCompatActivity", e);
        }
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onDataChanged() {
        super.onDataChanged();
        android.util.Log.d("CategoryAdapter", "onDataChanged called, itemCount: " + getItemCount());
    }



    public class CategoryViewHolder extends RecyclerView.ViewHolder{
        TextView categoryLabel;
        ImageView categoryImage;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImage);
            categoryLabel = itemView.findViewById(R.id.categoryLabel);
        }
    }

}


