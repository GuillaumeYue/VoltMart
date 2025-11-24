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
import com.example.voltmart.model.ProductModel;
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
 * 删除产品活动页面
 * 管理员删除产品
 */
public class DeleteProductActivity extends AppCompatActivity {

    LinearLayout detailsLinearLayout;
    TextView nameTextView, categoryTextView, descTextView, specTextView, stockTextView, priceTextView, discountTextView;
    Button deleteProductBtn;
    ImageView backBtn, productImageView;

    AutoCompleteTextView idDropDown;
    ArrayAdapter<String> idAdapter;
    ProductModel currProduct;
    String docId;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_product);
        
        LinearLayout topLayout = findViewById(R.id.topHeaderLayout);
        if (topLayout != null) {
            WindowInsetsHelper.applyTopWindowInsets(topLayout, 4);
        }

        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        nameTextView = findViewById(R.id.nameTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        descTextView = findViewById(R.id.descriptionTextView);
        specTextView = findViewById(R.id.specificationTextView);
        stockTextView = findViewById(R.id.stockTextView);
        priceTextView = findViewById(R.id.priceTextView);
        discountTextView = findViewById(R.id.discountTextView);
        productImageView = findViewById(R.id.productImageView);

        deleteProductBtn = findViewById(R.id.deleteProductBtn);
        backBtn = findViewById(R.id.backBtn);

        deleteProductBtn.setOnClickListener(v -> {
            deleteFromFirebase();
        });

        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });

        initDropDown(new MyCallback() {
            @Override
            public void onCallback(List<ProductModel> productsList, List<String> docIdList) {
                String[] ids = new String[productsList.size()];
                for (int i = 0; i < productsList.size(); i++)
                    ids[i] = Integer.toString(productsList.get(i).getProductId());

                idAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, ids);
                idDropDown.setAdapter(idAdapter);
                idDropDown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        docId = docIdList.get(i);
                        initProduct(productsList.get(i));
                    }
                });
            }
        });
    }

    private void initDropDown(MyCallback myCallback) {
        FirebaseUtil.getProducts().orderBy("productId")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<ProductModel> products = new ArrayList<>();
                            List<String> docIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    ProductModel product = document.toObject(ProductModel.class);
                                    if (product != null && product.getProductId() > 0) {
                                        products.add(product);
                                        docIds.add(document.getId());
                                    }
                                } catch (Exception e) {
                                    Log.e("DeleteProductActivity", "Error parsing product document", e);
                                }
                            }
                            // Sort by productId manually
                            if (products.size() > 0) {
                                for (int i = 0; i < products.size() - 1; i++) {
                                    for (int j = i + 1; j < products.size(); j++) {
                                        if (products.get(i).getProductId() > products.get(j).getProductId()) {
                                            ProductModel temp = products.get(i);
                                            products.set(i, products.get(j));
                                            products.set(j, temp);
                                            String tempDocId = docIds.get(i);
                                            docIds.set(i, docIds.get(j));
                                            docIds.set(j, tempDocId);
                                        }
                                    }
                                }
                                myCallback.onCallback(products, docIds);
                            } else {
                                Toast.makeText(context, "No products found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Fallback without orderBy
                            loadProductsWithoutOrderBy(myCallback);
                        }
                    }
                });
    }

    private void loadProductsWithoutOrderBy(MyCallback myCallback) {
        FirebaseUtil.getProducts()
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<ProductModel> products = new ArrayList<>();
                            List<String> docIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    ProductModel product = document.toObject(ProductModel.class);
                                    if (product != null && product.getProductId() > 0) {
                                        products.add(product);
                                        docIds.add(document.getId());
                                    }
                                } catch (Exception e) {
                                    Log.e("DeleteProductActivity", "Error parsing product document", e);
                                }
                            }
                            // Sort products by productId manually
                            if (products.size() > 0) {
                                for (int i = 0; i < products.size() - 1; i++) {
                                    for (int j = i + 1; j < products.size(); j++) {
                                        if (products.get(i).getProductId() > products.get(j).getProductId()) {
                                            ProductModel temp = products.get(i);
                                            products.set(i, products.get(j));
                                            products.set(j, temp);
                                            String tempDocId = docIds.get(i);
                                            docIds.set(i, docIds.get(j));
                                            docIds.set(j, tempDocId);
                                        }
                                    }
                                }
                                myCallback.onCallback(products, docIds);
                            } else {
                                Toast.makeText(context, "No products found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void initProduct(ProductModel p) {
        currProduct = p;

        Picasso.get().load(currProduct.getImage()).into(productImageView);

        detailsLinearLayout.setVisibility(View.VISIBLE);
        productImageView.setVisibility(View.VISIBLE);

        nameTextView.setText("Name: " + currProduct.getName());
        categoryTextView.setText("Category: " + currProduct.getCategory());
        descTextView.setText("Description: " + currProduct.getDescription());
        specTextView.setText("Specification: " + currProduct.getSpecification());
        stockTextView.setText("Stock: " + currProduct.getStock());
        priceTextView.setText("Price: $" + currProduct.getPrice());
        discountTextView.setText("Discount: $" + currProduct.getDiscount());
    }

    private void deleteFromFirebase() {
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(context, "Please select a product to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        SweetAlertDialog confirmDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        confirmDialog
                .setTitleText("Are you sure?")
                .setContentText("This action cannot be undone! Product: " + (currProduct != null ? currProduct.getName() : ""))
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

                        // Delete product image from storage using productId
                        if (currProduct != null) {
                            try {
                                FirebaseUtil.getProductImageReference(String.valueOf(currProduct.getProductId()))
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("DeleteProduct", "Image deleted successfully");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("DeleteProduct", "Failed to delete image", e);
                                            // Continue with document deletion even if image deletion fails
                                        });
                            } catch (Exception e) {
                                Log.e("DeleteProduct", "Error deleting image", e);
                            }
                        }

                        // Delete product document from Firestore
                        FirebaseUtil.getProducts().document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Product deleted successfully!", Toast.LENGTH_SHORT).show();
                                    
                                    // Clear UI
                                    detailsLinearLayout.setVisibility(View.GONE);
                                    productImageView.setVisibility(View.GONE);
                                    idDropDown.setText("");
                                    docId = null;
                                    currProduct = null;
                                    
                                    // Refresh dropdown
                                    initDropDown(new MyCallback() {
                                        @Override
                                        public void onCallback(List<ProductModel> productsList, List<String> docIdList) {
                                            String[] ids = new String[productsList.size()];
                                            for (int i = 0; i < productsList.size(); i++)
                                                ids[i] = Integer.toString(productsList.get(i).getProductId());

                                            idAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, ids);
                                            idDropDown.setAdapter(idAdapter);
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Failed to delete product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("DeleteProduct", "Error deleting product", e);
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
        void onCallback(List<ProductModel> products, List<String> docIds);
    }
}

