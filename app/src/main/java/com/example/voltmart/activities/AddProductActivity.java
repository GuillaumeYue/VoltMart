package com.example.voltmart.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.voltmart.model.ProductModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * 添加商品活动页面
 * 管理员添加新商品到系统
 * 功能包括：
 * - 输入商品信息（ID、名称、分类、描述、规格、库存、价格、折扣）
 * - 上传商品图片
 * - 生成商品分享链接
 * - 保存商品到Firebase
 */
public class AddProductActivity extends AppCompatActivity {

    // UI组件 - 输入框
    TextInputEditText idEditText;        // 商品ID输入框
    TextInputEditText nameEditText;      // 商品名称输入框
    TextInputEditText descEditText;      // 商品描述输入框
    TextInputEditText specEditText;      // 商品规格输入框
    TextInputEditText stockEditText;     // 库存输入框
    TextInputEditText priceEditText;     // 价格输入框
    TextInputEditText discountEditText;  // 折扣输入框
    Button imageBtn;                     // 选择图片按钮
    Button addProductBtn;                // 添加商品按钮
    ImageView backBtn;                    // 返回按钮
    ImageView productImageView;          // 商品图片预览
    TextView removeImageBtn;              // 移除图片按钮

    // UI组件 - 分类下拉框
    AutoCompleteTextView categoryDropDown; // 分类下拉选择框
    ArrayAdapter<String> arrayAdapter;     // 分类适配器
    String[] categories;                   // 分类数组

    // 数据
    String category;          // 选中的分类
    String productImage;      // 商品图片URL
    String shareLink;         // 商品分享链接
    String productName;       // 商品名称
    int productId = 1;        // 商品ID（初始化为默认值1）
    Context context = this;   // 上下文
    boolean imageUploaded = false; // 图片是否已上传

    SweetAlertDialog dialog;  // 进度对话框


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_product);

        idEditText = findViewById(R.id.idEditText);
        nameEditText = findViewById(R.id.nameEditText);
        categoryDropDown = findViewById(R.id.categoryDropDown);
        descEditText = findViewById(R.id.descriptionEditText);
        specEditText = findViewById(R.id.specificationEditText);
        stockEditText = findViewById(R.id.stockEditText);
        priceEditText = findViewById(R.id.priceEditText);
        discountEditText = findViewById(R.id.discountEditText);
        productImageView = findViewById(R.id.productImageView);

        imageBtn = findViewById(R.id.imageBtn);
        addProductBtn = findViewById(R.id.addProductBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        // Set initial productId value immediately
        idEditText.setText(productId + "");

        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            Object lastProductIdObj = document.get("lastProductId");
                            if (lastProductIdObj != null) {
                                try {
                                    productId = Integer.parseInt(lastProductIdObj.toString()) + 1;
                                    idEditText.setText(productId + "");
                                } catch (NumberFormatException e) {
                                    Log.e("AddProductActivity", "Error parsing lastProductId", e);
                                    productId = 1; // Default to 1 if parsing fails
                                    idEditText.setText(productId + "");
                                }
                            } else {
                                // If lastProductId doesn't exist, start from 1
                                productId = 1;
                                idEditText.setText(productId + "");
                            }
                        } else {
                            // If document doesn't exist or task failed, start from 1
                            productId = 1;
                            idEditText.setText(productId + "");
                        }
                    }
                });

        imageBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        addProductBtn.setOnClickListener(v -> {
            generateDynamicLink();
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

    private void getCategories(MyCallback myCallback) {
        int size[] = new int[1];

        FirebaseUtil.getCategories().orderBy("name")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            size[0] = task.getResult().size();
                        }
                        myCallback.onCallback(size);
                    }
                });
        categories = new String[size[0]];

        FirebaseUtil.getCategories().orderBy("name")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int i = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                categories[i] = ((String) document.getData().get("name"));
                                Log.i("Category", categories[i]);
                                i++;
                            }
                            myCallback.onCallback(categories);
                        }
                    }
                });
    }

    private void addToFirebase() {
        productName = nameEditText.getText().toString();
        List<String> sk = Arrays.asList(productName.trim().toLowerCase().split(" "));
        String desc = descEditText.getText().toString();
        String spec = specEditText.getText().toString();
        
        // Validate and parse price
        String priceStr = priceEditText.getText().toString().trim();
        if (priceStr.isEmpty()) {
            Toast.makeText(this, "Price is required", Toast.LENGTH_SHORT).show();
            return;
        }
        int price;
        try {
            price = Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price value", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate and parse discount
        String discountStr = discountEditText.getText().toString().trim();
        if (discountStr.isEmpty()) {
            Toast.makeText(this, "Discount is required", Toast.LENGTH_SHORT).show();
            return;
        }
        int discount;
        try {
            discount = Integer.parseInt(discountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid discount value", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate and parse stock
        String stockStr = stockEditText.getText().toString().trim();
        if (stockStr.isEmpty()) {
            Toast.makeText(this, "Stock is required", Toast.LENGTH_SHORT).show();
            return;
        }
        int stock;
        try {
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid stock value", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get productId from idEditText if available, otherwise use the initialized value
        String idStr = idEditText.getText().toString().trim();
        if (!idStr.isEmpty()) {
            try {
                productId = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                Log.e("AddProductActivity", "Error parsing productId from EditText", e);
                // Keep the existing productId value
            }
        }

        ProductModel model = new ProductModel(productName, sk, productImage, category, desc, spec, price, discount, price - discount, productId, stock, shareLink, 0f, 0);
//        Log.i("Link2", shareLink);
        FirebaseUtil.getProducts().add(model)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Use set with merge to create document if it doesn't exist
                        Map<String, Object> detailsMap = new HashMap<>();
                        detailsMap.put("lastProductId", productId);
                        FirebaseUtil.getDetails().set(detailsMap, SetOptions.merge())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(AddProductActivity.this, "Product has been added successfully!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            // Even if updating details fails, product was added successfully
                                            Toast.makeText(AddProductActivity.this, "Product has been added successfully!", Toast.LENGTH_SHORT).show();
                                            Log.e("AddProductActivity", "Failed to update lastProductId", task.getException());
                                            finish();
                                        }
                                    }
                                });
                    }
                });
    }

    private void generateDynamicLink() {
//        Log.i("Function", "Function called");
        if (!validate())
            return;

        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://www.example.com/?product_id=" + productId))
                .setDomainUriPrefix("https://voltmart.page.link")
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder("com.example.voltmart").build())
                .setSocialMetaTagParameters(new DynamicLink.SocialMetaTagParameters.Builder()
                        .setTitle(productName)
                        .setImageUrl(Uri.parse(productImage))
                        .build())
                .buildShortDynamicLink(ShortDynamicLink.Suffix.SHORT)
                .addOnCompleteListener(this, new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if (task.isSuccessful()) {
                            Uri shortLink = task.getResult().getShortLink();
                            shareLink = shortLink.toString();
//                            Log.i("Link True", shareLink);

                            addToFirebase();
                        } else {
                            // If dynamic link generation fails, still add the product with a fallback link
                            Exception exception = task.getException();
                            if (exception != null) {
                                exception.printStackTrace();
                            }
                            Log.w("AddProductActivity", "Dynamic link generation failed, adding product with fallback link", exception);
                            // Use a fallback share link
                            shareLink = "https://www.example.com/?product_id=" + productId;
                            addToFirebase();
                        }
                    }
                });
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
        if (categoryDropDown.getText().toString().trim().length() == 0) {
            categoryDropDown.setError("Category is required");
            isValid = false;
        }
        if (descEditText.getText().toString().trim().length() == 0) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        if (stockEditText.getText().toString().trim().length() == 0) {
            stockEditText.setError("Stock is required");
            isValid = false;
        }
        if (priceEditText.getText().toString().trim().length() == 0) {
            priceEditText.setError("Price is required");
            isValid = false;
        }

        if (!imageUploaded) {
            Toast.makeText(context, "Image is not selected", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
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
                        imageUploaded = false;
                        productImageView.setImageDrawable(null);
                        productImageView.setVisibility(View.GONE);
                        removeImageBtn.setVisibility(View.GONE);

                        FirebaseUtil.getProductImageReference(productId + "").delete();
                        alertDialog.dismiss();
                    }
                }).show();
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

                productId = Integer.parseInt(idEditText.getText().toString());
                FirebaseUtil.getProductImageReference(productId + "").putFile(imageUri)
                        .addOnCompleteListener(t -> {
                            imageUploaded = true;

                            FirebaseUtil.getProductImageReference(productId + "").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    productImage = uri.toString();

                                    Picasso.get().load(uri).into(productImageView, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            dialog.dismiss();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                        }
                                    });
                                    productImageView.setVisibility(View.VISIBLE);
                                    removeImageBtn.setVisibility(View.VISIBLE);
                                }
                            });
                        });
            }
        }
    }

    public interface MyCallback {
        void onCallback(String[] categories);

        void onCallback(int[] size);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        FirebaseUtil.getProductImageReference(productId + "").delete();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getCategories(new MyCallback() {
            @Override
            public void onCallback(String[] cate) {
                arrayAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, cate);
                categoryDropDown.setAdapter(arrayAdapter);
                categoryDropDown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        category = adapterView.getItemAtPosition(i).toString();
                    }
                });
            }

            @Override
            public void onCallback(int[] size) {
                categories = new String[size[0]];
            }
        });
    }

}