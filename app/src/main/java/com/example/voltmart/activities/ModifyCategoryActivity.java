package com.example.voltmart.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.voltmart.model.CategoryModel;
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
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ModifyCategoryActivity extends AppCompatActivity {

    LinearLayout detailsLinearLayout;
    TextInputEditText nameEditText, descEditText, colorEditText;
    Button imageBtn, modifyCategoryBtn;
    ImageView backBtn, categoryImageView;
    TextView removeImageBtn;

    AutoCompleteTextView idDropDown, statusDropDown;
    ArrayAdapter<String> idAdapter, statusAdapter;
    CategoryModel currCategory;
    String docId, categoryImage, status;
    Uri imageUri;
    int categoryId;
    Context context = this;
    boolean imageUploaded = true;

    SweetAlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modify_category);

        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        nameEditText = findViewById(R.id.nameEditText);
        descEditText = findViewById(R.id.descriptionEditText);
        colorEditText = findViewById(R.id.colorEditText);
        statusDropDown = findViewById(R.id.statusDropDown);

        categoryImageView = findViewById(R.id.categoryImageView);
        imageBtn = findViewById(R.id.imageBtn);
        modifyCategoryBtn = findViewById(R.id.modifyCategoryBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        imageBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        modifyCategoryBtn.setOnClickListener(v -> {
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
            public void onCallback(List<CategoryModel> categoriesList, List<String> docIdList) {
                String[] ids = new String[categoriesList.size()];
                for (int i=0; i<categoriesList.size(); i++)
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
                            int i = 0;
                            List<CategoryModel> categories = new ArrayList<>();
                            List<String> docIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                categories.add(document.toObject(CategoryModel.class));
                                docIds.add(document.getId());
                                i++;
                            }
                            myCallback.onCallback(categories, docIds);
                        }
                    }
                });
    }

    private void initCategory(CategoryModel model) {
        currCategory = model;
        categoryId = currCategory.getCategoryId();

        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Modifying...");
        dialog.setCancelable(false);
//        dialog.show();

        Picasso.get().load(currCategory.getIcon()).into(categoryImageView, new Callback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
            }
            @Override
            public void onError(Exception e) {
            }
        });

        statusAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, new String[]{"Enabled", "Disabled"});
        statusDropDown.setAdapter(statusAdapter);
        statusDropDown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                status = adapterView.getItemAtPosition(i).toString();
            }
        });
        
        // Add text change listener to capture status changes
        statusDropDown.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    status = s.toString();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        detailsLinearLayout.setVisibility(View.VISIBLE);
        categoryImageView.setVisibility(View.VISIBLE);
        removeImageBtn.setVisibility(View.VISIBLE);

        nameEditText.setText(currCategory.getName());
        descEditText.setText(currCategory.getBrief());
        colorEditText.setText(currCategory.getColor());
        
        // Set the status dropdown text and initialize the status variable
        String currentStatus = currCategory.getStatus();
        if (currentStatus != null && !currentStatus.isEmpty()) {
            statusDropDown.setText(currentStatus, false); // false = don't filter
            status = currentStatus; // Initialize status variable with current value
        } else {
            // Default to "Enabled" if status is null or empty
            status = "Enabled";
            statusDropDown.setText(status, false);
        }
    }

    private void updateToFirebase(){
        if (!validate())
            return;

        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);
        dialog.show();
        if (imageUri != null ) {
            FirebaseUtil.getCategoryImageReference(categoryId + "").putFile(imageUri)
                    .addOnCompleteListener(t -> {
                        FirebaseUtil.getCategoryImageReference(categoryId + "").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                categoryImage = uri.toString();
                                FirebaseUtil.getCategories().document(docId).update("icon", categoryImage);
                                updateDataToFirebase();
                                dialog.dismiss();
                                Toast.makeText(context, "Category has been modified successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    });
        }
        else {
            updateDataToFirebase();
            dialog.dismiss();
            Toast.makeText(context, "Category has been modified successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void updateDataToFirebase() {
        if (!nameEditText.getText().toString().equals(currCategory.getName()))
            FirebaseUtil.getCategories().document(docId).update("name", nameEditText.getText().toString());
        if (!descEditText.getText().toString().equals(currCategory.getBrief()))
            FirebaseUtil.getCategories().document(docId).update("brief", descEditText.getText().toString());
        if (!colorEditText.getText().toString().equals(currCategory.getColor()))
            FirebaseUtil.getCategories().document(docId).update("color", colorEditText.getText().toString());
        
        // Get the current status from dropdown text, or use the status variable if set
        String newStatus = statusDropDown.getText().toString().trim();
        if (newStatus.isEmpty() && status != null) {
            newStatus = status; // Fallback to status variable if dropdown text is empty
        }
        
        // Only update if status has changed
        String currentStatus = currCategory.getStatus() != null ? currCategory.getStatus() : "Enabled";
        if (!newStatus.isEmpty() && !newStatus.equals(currentStatus)) {
            FirebaseUtil.getCategories().document(docId).update("status", newStatus);
        }
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
                        categoryImageView.setImageDrawable(null);
                        categoryImageView.setVisibility(View.GONE);
                        removeImageBtn.setVisibility(View.GONE);
                        alertDialog.cancel();
                    }
                }).show();
    }

    boolean validate() {
        boolean isValid = true;
        if (nameEditText.getText().toString().trim().length() == 0) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (descEditText.getText().toString().trim().length() == 0) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        if (colorEditText.getText().toString().trim().length() == 0) {
            colorEditText.setError("Color is required");
            isValid = false;
        }
        if (colorEditText.getText().toString().charAt(0) != '#') {
            colorEditText.setError("Color should be HEX value");
            isValid = false;
        }
        if (statusDropDown.getText().toString().trim().length() == 0) {
            statusDropDown.setError("Status is required");
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

                Picasso.get().load(imageUri).into(categoryImageView, new Callback() {
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
        }
    }

    public interface MyCallback {
        void onCallback(List<CategoryModel> categories, List<String> docIds);
    }
}