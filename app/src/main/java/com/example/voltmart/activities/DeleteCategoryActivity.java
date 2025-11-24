package com.example.voltmart.activities;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voltmart.R;
import com.example.voltmart.model.CategoryModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.example.voltmart.utils.WindowInsetsHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * 删除分类活动页面
 * 管理员删除分类
 */
public class DeleteCategoryActivity extends AppCompatActivity {

    LinearLayout detailsLinearLayout;
    TextView nameTextView, descTextView, colorTextView;
    Button deleteCategoryBtn;
    ImageView backBtn, categoryImageView;

    AutoCompleteTextView idDropDown;
    ArrayAdapter<String> idAdapter;
    CategoryModel currCategory;
    String docId;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_category);
        
        LinearLayout topLayout = findViewById(R.id.topHeaderLayout);
        if (topLayout != null) {
            WindowInsetsHelper.applyTopWindowInsets(topLayout, 4);
        }

        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        nameTextView = findViewById(R.id.nameTextView);
        descTextView = findViewById(R.id.descriptionTextView);
        colorTextView = findViewById(R.id.colorTextView);
        categoryImageView = findViewById(R.id.categoryImageView);

        deleteCategoryBtn = findViewById(R.id.deleteCategoryBtn);
        backBtn = findViewById(R.id.backBtn);

        deleteCategoryBtn.setOnClickListener(v -> {
            deleteFromFirebase();
        });

        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });

        initDropDown(new MyCallback() {
            @Override
            public void onCallback(List<CategoryModel> categoriesList, List<String> docIdList) {
                String[] ids = new String[categoriesList.size()];
                for (int i = 0; i < categoriesList.size(); i++)
                    ids[i] = Integer.toString(categoriesList.get(i).getCategoryId());

                idAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, ids);
                idDropDown.setAdapter(idAdapter);
                idDropDown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        docId = docIdList.get(i);
                        initCategory(categoriesList.get(i));
                    }
                });
            }
        });
    }

    private void initDropDown(MyCallback myCallback) {
        FirebaseUtil.getCategories().orderBy("categoryId")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<CategoryModel> categories = new ArrayList<>();
                            List<String> docIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                categories.add(document.toObject(CategoryModel.class));
                                docIds.add(document.getId());
                            }
                            myCallback.onCallback(categories, docIds);
                        }
                    }
                });
    }

    private void initCategory(CategoryModel model) {
        currCategory = model;

        Picasso.get().load(currCategory.getIcon()).into(categoryImageView);

        detailsLinearLayout.setVisibility(View.VISIBLE);
        categoryImageView.setVisibility(View.VISIBLE);

        nameTextView.setText("Name: " + currCategory.getName());
        descTextView.setText("Description: " + currCategory.getBrief());
        colorTextView.setText("Color: " + currCategory.getColor());
    }

    private void deleteFromFirebase() {
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(context, "Please select a category to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        SweetAlertDialog confirmDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        confirmDialog
                .setTitleText("Are you sure?")
                .setContentText("This action cannot be undone! Category: " + (currCategory != null ? currCategory.getName() : ""))
                .setConfirmText("Yes, Delete")
                .setCancelText("Cancel")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        confirmDialog.dismiss();
                        
                        SweetAlertDialog progressDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
                        progressDialog.getProgressHelper().setBarColor(Color.parseColor("#FF0000"));
                        progressDialog.setTitleText("Deleting...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        // Delete category image from storage using categoryId
                        if (currCategory != null) {
                            try {
                                FirebaseUtil.getCategoryImageReference(String.valueOf(currCategory.getCategoryId()))
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("DeleteCategory", "Image deleted successfully");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("DeleteCategory", "Failed to delete image", e);
                                            // Continue with document deletion even if image deletion fails
                                        });
                            } catch (Exception e) {
                                Log.e("DeleteCategory", "Error deleting image", e);
                            }
                        }

                        // Delete category document from Firestore
                        FirebaseUtil.getCategories().document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Category deleted successfully!", Toast.LENGTH_SHORT).show();
                                    
                                    // Clear UI
                                    detailsLinearLayout.setVisibility(View.GONE);
                                    categoryImageView.setVisibility(View.GONE);
                                    idDropDown.setText("");
                                    docId = null;
                                    currCategory = null;
                                    
                                    // Refresh dropdown
                                    initDropDown(new MyCallback() {
                                        @Override
                                        public void onCallback(List<CategoryModel> categoriesList, List<String> docIdList) {
                                            String[] ids = new String[categoriesList.size()];
                                            for (int i = 0; i < categoriesList.size(); i++)
                                                ids[i] = Integer.toString(categoriesList.get(i).getCategoryId());

                                            idAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, ids);
                                            idDropDown.setAdapter(idAdapter);
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Failed to delete category: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("DeleteCategory", "Error deleting category", e);
                                });
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        confirmDialog.cancel();
                    }
                })
                .show();
    }

    public interface MyCallback {
        void onCallback(List<CategoryModel> categories, List<String> docIds);
    }
}

