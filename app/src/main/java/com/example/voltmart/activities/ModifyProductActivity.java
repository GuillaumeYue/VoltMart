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
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ModifyProductActivity extends AppCompatActivity {

    LinearLayout detailsLinearLayout;
    TextInputEditText nameEditText, descEditText, specEditText, stockEditText, priceEditText, discountEditText;
    Button imageBtn, modifyProductBtn;
    ImageView backBtn, productImageView;
    TextView removeImageBtn;

    AutoCompleteTextView idDropDown, categoryDropDown;
    ArrayAdapter<String> idAdapter, categoryAdapter;
    ProductModel currProduct;
    String[] categories;
    String category, docId, productImage;
    Uri imageUri;
    int productId;
    Context context = this;
    boolean imageUploaded = true;

    SweetAlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modify_product);

        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        nameEditText = findViewById(R.id.nameEditText);
        categoryDropDown = findViewById(R.id.categoryDropDown);
        descEditText = findViewById(R.id.descriptionEditText);
        specEditText = findViewById(R.id.specificationEditText);
        stockEditText = findViewById(R.id.stockEditText);
        priceEditText = findViewById(R.id.priceEditText);
        discountEditText = findViewById(R.id.discountEditText);
        productImageView = findViewById(R.id.productImageView);

        imageBtn = findViewById(R.id.imageBtn);
        modifyProductBtn = findViewById(R.id.modifyProductBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        imageBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        modifyProductBtn.setOnClickListener(v -> {
            updateToFirebase();
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

        initDropDown(new MyCallback() {
            @Override
            public void onCallback(String[] cate) {
                categoryAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, cate);
                categoryDropDown.setAdapter(categoryAdapter);
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

            @Override
            public void onCallback(List<ProductModel> productsList, List<String> docIdList) {
//                products = productsList;
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
        int size[] = new int[1];

        // Try to load products with orderBy first, fallback to without orderBy if it fails
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
                                    Log.e("ModifyProductActivity", "Error parsing product document", e);
                                }
                            }
                            // Sort by productId manually if needed
                            if (products.size() > 0) {
                                // Sort products by productId
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
                                Log.w("ModifyProductActivity", "No products found to display");
                                Toast.makeText(context, "No products found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // If orderBy fails (e.g., missing index), try without orderBy
                            Log.w("ModifyProductActivity", "orderBy query failed, trying without orderBy", task.getException());
                            loadProductsWithoutOrderBy(myCallback);
                        }
                    }
                });

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
                                    Log.e("ModifyProductActivity", "Error parsing product document", e);
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
                                Log.w("ModifyProductActivity", "No products found to display");
                                Toast.makeText(context, "No products found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("ModifyProductActivity", "Error loading products without orderBy", task.getException());
                            Toast.makeText(context, "Error loading products", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void initProduct(ProductModel p) {
        currProduct = p;
        productId = currProduct.getProductId();

        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Modifying...");
        dialog.setCancelable(false);
//        dialog.show();

        Picasso.get().load(currProduct.getImage()).into(productImageView, new Callback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
            }

            @Override
            public void onError(Exception e) {
            }
        });

        detailsLinearLayout.setVisibility(View.VISIBLE);
        productImageView.setVisibility(View.VISIBLE);
        removeImageBtn.setVisibility(View.VISIBLE);

        nameEditText.setText(currProduct.getName());
        categoryDropDown.setText(currProduct.getCategory());
        descEditText.setText(currProduct.getDescription());
        specEditText.setText(currProduct.getSpecification());
        stockEditText.setText(currProduct.getStock() + "");
        priceEditText.setText(currProduct.getPrice() + "");
        discountEditText.setText(currProduct.getDiscount() + "");
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
                        alertDialog.cancel();
                    }
                }).show();
    }

    private void updateToFirebase() {
        if (!validate())
            return;

        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);
        dialog.show();
        if (imageUri != null) {
            FirebaseUtil.getProductImageReference(productId + "").putFile(imageUri)
                    .addOnCompleteListener(t -> {
                        FirebaseUtil.getProductImageReference(productId + "").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                productImage = uri.toString();
                                FirebaseUtil.getProducts().document(docId).update("image", productImage);
                                updateDataToFirebase();
                                dialog.dismiss();
                                Toast.makeText(context, "Product has been successfully modified!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    });
        } else {
            updateDataToFirebase();
            dialog.dismiss();
            Toast.makeText(context, "Product has been successfully modified!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void updateDataToFirebase() {
        if (!nameEditText.getText().toString().equals(currProduct.getName())) {
            FirebaseUtil.getProducts().document(docId).update("name", nameEditText.getText().toString());
            FirebaseUtil.getProducts().document(docId).update("searchKey", Arrays.asList(currProduct.getName().trim().toLowerCase().split(" ")));
        }
        if (!categoryDropDown.getText().toString().equals(currProduct.getCategory()))
            FirebaseUtil.getProducts().document(docId).update("category", categoryDropDown.getText().toString());
        if (!descEditText.getText().toString().equals(currProduct.getDescription()))
            FirebaseUtil.getProducts().document(docId).update("description", descEditText.getText().toString());
        if (!specEditText.getText().toString().equals(currProduct.getSpecification()))
            FirebaseUtil.getProducts().document(docId).update("specification", specEditText.getText().toString());
        if (!stockEditText.getText().toString().equals(currProduct.getStock() + ""))
            FirebaseUtil.getProducts().document(docId).update("stock", Integer.parseInt(stockEditText.getText().toString()));
        if (!priceEditText.getText().toString().equals(currProduct.getOriginalPrice() + "")) {
            FirebaseUtil.getProducts().document(docId).update("originalPrice", Integer.parseInt(priceEditText.getText().toString()));
            FirebaseUtil.getProducts().document(docId).update("price", Integer.parseInt(priceEditText.getText().toString()) - Integer.parseInt(discountEditText.getText().toString()));
        }
        if (!discountEditText.getText().toString().equals(currProduct.getDiscount() + ""))
            FirebaseUtil.getProducts().document(docId).update("discount", Integer.parseInt(discountEditText.getText().toString()));
    }

    boolean validate() {
        boolean isValid = true;
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
        if (Integer.parseInt(discountEditText.getText().toString()) > Integer.parseInt(priceEditText.getText().toString())) {
            priceEditText.setError("Price should be more than discount");
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
                imageUri = data.getData();
                imageUploaded = true;
                dialog.show();

                Picasso.get().load(imageUri).into(productImageView, new Callback() {
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
        }
    }

    public interface MyCallback {
        void onCallback(String[] categories);

        void onCallback(int[] size);

        void onCallback(List<ProductModel> products, List<String> docIds);
    }
}