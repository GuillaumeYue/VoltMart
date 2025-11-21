package com.example.voltmart.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.voltmart.R;
import com.example.voltmart.model.CategoryModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * 添加分类活动页面
 * 管理员添加新商品分类到系统
 * 功能包括：
 * - 输入分类信息（ID、名称、描述、颜色）
 * - 上传分类图标
 * - 保存分类到Firebase
 */
public class AddCategoryActivity extends AppCompatActivity {

    // UI组件 - 输入框
    TextInputEditText idEditText;      // 分类ID输入框
    TextInputEditText nameEditText;    // 分类名称输入框
    TextInputEditText descEditText;    // 分类描述输入框
    TextInputEditText colorEditText;   // 分类颜色输入框
    Button imageBtn;                   // 选择图片按钮
    Button addCategoryBtn;             // 添加分类按钮
    ImageView backBtn;                 // 返回按钮
    ImageView categoryImageView;       // 分类图标预览
    TextView removeImageBtn;            // 移除图片按钮

    // 数据
    String categoryImage;  // 分类图标URL
    String productName;    // 产品名称（可能未使用）
    int categoryId = 1;     // 分类ID（初始化为默认值1）
    Context context = this; // 上下文
    boolean imageUploaded = false; // 图片是否已上传

    SweetAlertDialog dialog; // 进度对话框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_category);

        idEditText = findViewById(R.id.idEditText);
        nameEditText = findViewById(R.id.nameEditText);
        descEditText = findViewById(R.id.descriptionEditText);
        colorEditText = findViewById(R.id.colorEditText);
        categoryImageView = findViewById(R.id.categoryImageView);

        imageBtn = findViewById(R.id.imageBtn);
        addCategoryBtn = findViewById(R.id.addCategoryBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        // Clear all fields to remove previous data
        clearFields();

        // Set initial categoryId value immediately
        idEditText.setText(categoryId + "");

        // First, check actual categories in database to find the highest ID
        // Use an array to hold the value so it can be modified in inner classes
        final int[] maxCategoryId = {0};
        
        FirebaseUtil.getCategories().get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    CategoryModel category = document.toObject(CategoryModel.class);
                                    if (category != null && category.getCategoryId() > maxCategoryId[0]) {
                                        maxCategoryId[0] = category.getCategoryId();
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("AddCategoryActivity", "Error parsing category", e);
                                }
                            }
                        }
                        
                        // If no categories exist in database, start from 1
                        if (maxCategoryId[0] == 0) {
                            categoryId = 1;
                            idEditText.setText(categoryId + "");
                            return;
                        }
                        
                        // Use the higher value between maxCategoryId from database and lastCategoryId from details
                        FirebaseUtil.getDetails().get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        // Use the maximum from database as base (since we know categories exist)
                                        int lastCategoryId = maxCategoryId[0];
                                        
                                        if (task.isSuccessful() && task.getResult() != null) {
                                            DocumentSnapshot document = task.getResult();
                                            Object lastCategoryIdObj = document.get("lastCategoryId");
                                            if (lastCategoryIdObj != null) {
                                                try {
                                                    int detailsCategoryId = Integer.parseInt(lastCategoryIdObj.toString());
                                                    // Use the maximum of both values, but prioritize actual database value
                                                    lastCategoryId = Math.max(maxCategoryId[0], detailsCategoryId);
                                                } catch (NumberFormatException e) {
                                                    android.util.Log.e("AddCategoryActivity", "Error parsing lastCategoryId", e);
                                                    // If parsing fails, use the database value
                                                    lastCategoryId = maxCategoryId[0];
                                                }
                                            }
                                        }
                                        
                                        // Set categoryId to the next available ID
                                        categoryId = lastCategoryId + 1;
                                        idEditText.setText(categoryId + "");
                                    }
                                });
                    }
                });

        imageBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        addCategoryBtn.setOnClickListener(v -> {
            addToFirebase();
        });

        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });

        removeImageBtn.setOnClickListener(v -> {
            removeImage();
        });

        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Uploading image...");
        dialog.setCancelable(false);
    }

    private void addToFirebase() {
        if (!validate())
            return;

        categoryId = Integer.parseInt(idEditText.getText().toString());
        String name = nameEditText.getText().toString();
        String desc = descEditText.getText().toString();
        String color = colorEditText.getText().toString();

        CategoryModel category = new CategoryModel(name, categoryImage, color, desc, categoryId);

        FirebaseUtil.getCategories().add(category)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Use set with merge to create document if it doesn't exist
                        Map<String, Object> detailsMap = new HashMap<>();
                        detailsMap.put("lastCategoryId", categoryId);
                        FirebaseUtil.getDetails().set(detailsMap, SetOptions.merge())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(AddCategoryActivity.this, "Category has been added successfully!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            // Even if updating details fails, category was added successfully
                                            Toast.makeText(AddCategoryActivity.this, "Category has been added successfully!", Toast.LENGTH_SHORT).show();
                                            android.util.Log.e("AddCategoryActivity", "Failed to update lastCategoryId", task.getException());
                                            finish();
                                        }
                                    }
                                });
                    }
                });

    }

    private void removeImage() {
        SweetAlertDialog alertDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        alertDialog
                .setTitleText("Are you sure?")
                .setContentText("Do you want to remove this image?")
                .setConfirmText("Yes")
                .setCancelText("No")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        // Only delete image if categoryId is set and image was uploaded
                        if (categoryId > 0 && imageUploaded && categoryImage != null) {
                            FirebaseUtil.getCategoryImageReference(categoryId + "").delete();
                        }
                        
                        imageUploaded = false;
                        categoryImageView.setImageDrawable(null);
                        categoryImageView.setVisibility(View.GONE);
                        removeImageBtn.setVisibility(View.GONE);
                        categoryImage = null;
                        
                        alertDialog.dismiss();
                    }
                }).show();
    }

    private boolean validate() {
        boolean isValid = true;
        if (idEditText.getText().toString().trim().length() == 0) {
            idEditText.setError("Id is required");
            isValid = false;
        }
        if (nameEditText.getText().toString().trim().length() == 0) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (descEditText.getText().toString().trim().length() == 0) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        String colorText = colorEditText.getText().toString().trim();
        if (colorText.length() == 0) {
            colorEditText.setError("Color is required");
            isValid = false;
        } else if (colorText.charAt(0) != '#') {
            colorEditText.setError("Color should be HEX value");
            isValid = false;
        }

        if (!imageUploaded) {
            Toast.makeText(context, "Image is not selected", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                if (idEditText.getText().toString().trim().length() == 0) {
                    Toast.makeText(this, "Please fill the id first", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.show();

                categoryId = Integer.parseInt(idEditText.getText().toString());
                FirebaseUtil.getCategoryImageReference(categoryId + "").putFile(imageUri)
                        .addOnCompleteListener(t -> {
                            imageUploaded = true;

                            FirebaseUtil.getCategoryImageReference(categoryId + "").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    categoryImage = uri.toString();

                                    Picasso.get().load(uri).into(categoryImageView, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            dialog.dismiss();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                        }
                                    });
                                    categoryImageView.setVisibility(View.VISIBLE);
                                    removeImageBtn.setVisibility(View.VISIBLE);
                                }
                            });
                        });
            }
        }
    }

    private void clearFields() {
        // Clear all text fields
        nameEditText.setText("");
        descEditText.setText("");
        colorEditText.setText("");
        
        // Clear image
        categoryImageView.setImageDrawable(null);
        categoryImageView.setVisibility(View.GONE);
        removeImageBtn.setVisibility(View.GONE);
        categoryImage = null;
        imageUploaded = false;
    }

    public void onBackPressed() {
        super.onBackPressed();
        // Only delete image if one was uploaded
        if (imageUploaded && categoryId > 0) {
            FirebaseUtil.getCategoryImageReference(categoryId + "").delete();
        }
    }
}