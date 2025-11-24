package com.example.voltmart.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
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
        String status = categoryModel.getStatus();
        String categoryName = categoryModel.getName();
        
        if (categoryName != null && (categoryName.toLowerCase().contains("tv") || categoryName.toLowerCase().contains("television"))) {
            Log.d("CategoryAdapter", "TV category found - Name: " + categoryName + ", Status: " + status + ", Position: " + i);
        }
        
        categoryViewHolder.itemView.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams params = categoryViewHolder.itemView.getLayoutParams();
        if (params != null) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            categoryViewHolder.itemView.setLayoutParams(params);
        }
        
        if (categoryName != null && !categoryName.isEmpty()) {
            categoryViewHolder.categoryLabel.setText(categoryName);
        } else {
            categoryViewHolder.categoryLabel.setText("");
            Log.w("CategoryAdapter", "Category at position " + i + " has empty name");
        }
        
        String iconUrl = categoryModel.getIcon();
        if (iconUrl != null && !iconUrl.isEmpty()) {
            Picasso.get().load(iconUrl).into(categoryViewHolder.categoryImage);
        } else {
            Log.w("CategoryAdapter", "Category " + categoryName + " has empty icon URL");
        }
        
        categoryViewHolder.itemView.setOnClickListener(v -> {
            if (categoryName != null && !categoryName.isEmpty()) {
                AppCompatActivity activity = (AppCompatActivity) context;
                CategoryFragment categoryFragment = new CategoryFragment();
                android.os.Bundle bundle = new android.os.Bundle();
                bundle.putString("categoryName", categoryName);
                categoryFragment.setArguments(bundle);
                
                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.main_frame_layout, categoryFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_adapter,parent,false);
        activity = (AppCompatActivity) view.getContext();
        return new CategoryViewHolder(view);
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


