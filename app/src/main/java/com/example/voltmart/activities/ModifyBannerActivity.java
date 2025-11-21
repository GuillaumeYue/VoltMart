package com.example.voltmart.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.text.TextWatcher;
import android.text.Editable;
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
import com.example.voltmart.model.BannerModel;
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

/**
 * 修改横幅活动页面
 * 管理员修改现有横幅的信息
 * 功能包括：
 * - 选择要修改的横幅
 * - 修改横幅图片
 * - 修改横幅描述
 * - 修改横幅状态（Live/Not Live）
 * - 更新横幅到Firebase
 */
public class ModifyBannerActivity extends AppCompatActivity {

    // UI组件
    LinearLayout detailsLinearLayout;    // 详情布局
    TextInputEditText descEditText;      // 描述输入框
    Button imageBtn;                     // 选择图片按钮
    Button modifyBannerBtn;              // 修改横幅按钮
    ImageView backBtn;                   // 返回按钮
    ImageView bannerImageView;           // 横幅图片预览
    TextView removeImageBtn;              // 移除图片按钮

    // UI组件 - 下拉框
    AutoCompleteTextView idDropDown;     // 横幅ID下拉选择框
    AutoCompleteTextView statusDropDown; // 状态下拉选择框（Live/Not Live）
    ArrayAdapter<String> idAdapter;      // ID适配器
    ArrayAdapter<String> statusAdapter;  // 状态适配器

    // 数据
    BannerModel currBanner;  // 当前横幅数据模型
    String status;           // 横幅状态
    String docId;            // 横幅文档ID
    String bannerImage;      // 横幅图片URL
    Uri imageUri;            // 图片URI
    int bannerId;            // 横幅ID
    Context context = this;  // 上下文
    boolean imageUploaded = true; // 图片是否已上传（默认为true，因为可能不修改图片）

    SweetAlertDialog dialog; // 进度对话框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modify_banner);

        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        descEditText = findViewById(R.id.descriptionEditText);
        statusDropDown = findViewById(R.id.statusDropDown);

        bannerImageView = findViewById(R.id.bannerImageView);
        imageBtn = findViewById(R.id.imageBtn);
        modifyBannerBtn = findViewById(R.id.modifyBannerBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        imageBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        modifyBannerBtn.setOnClickListener(v -> {
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
            public void onCallback(List<BannerModel> bannerList, List<String> docIdList) {
                String[] ids = new String[bannerList.size()];
                for (int i=0; i<bannerList.size(); i++)
                    ids[i] = Integer.toString(bannerList.get(i).getBannerId());

                idAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, ids);
                idDropDown.setAdapter(idAdapter);
                idDropDown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        docId = docIdList.get(i);
                        initBanner(bannerList.get(i));
                    }
                });
            }
        });
    }

    private void initDropDown(MyCallback myCallback) {
        FirebaseUtil.getBanner().orderBy("bannerId")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int i = 0;
                            List<BannerModel> banners = new ArrayList<>();
                            List<String> docIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                banners.add(document.toObject(BannerModel.class));
                                docIds.add(document.getId());
                                i++;
                            }
                            myCallback.onCallback(banners, docIds);
                        }
                    }
                });
    }

    private void initBanner(BannerModel model) {
        currBanner = model;
        bannerId = currBanner.getBannerId();

        SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Modifying...");
        dialog.setCancelable(false);
//        dialog.show();

        Picasso.get().load(currBanner.getBannerImage()).into(bannerImageView, new Callback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
            }
            @Override
            public void onError(Exception e) {
            }
        });

        statusAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, new String[]{"Live", "Not Live"});
        statusDropDown.setAdapter(statusAdapter);
        statusDropDown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                status = adapterView.getItemAtPosition(i).toString();
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
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
        bannerImageView.setVisibility(View.VISIBLE);
        removeImageBtn.setVisibility(View.VISIBLE);

        descEditText.setText(currBanner.getDescription());
        // Set the status dropdown text and initialize the status variable
        String currentStatus = currBanner.getStatus();
        if (currentStatus != null && !currentStatus.isEmpty()) {
            statusDropDown.setText(currentStatus, false); // false = don't filter
            status = currentStatus; // Initialize status variable with current value
        } else {
            // Default to "Not Live" if status is null or empty
            status = "Not Live";
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
            FirebaseUtil.getBannerImageReference(bannerId + "").putFile(imageUri)
                    .addOnCompleteListener(t -> {
                        FirebaseUtil.getBannerImageReference(bannerId + "").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                bannerImage = uri.toString();
                                FirebaseUtil.getBanner().document(docId).update("bannerImage", bannerImage);
                                updateDataToFirebase();
                                dialog.dismiss();
                                Toast.makeText(context, "Banner has been modified successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    });
        }
        else {
            updateDataToFirebase();
            dialog.dismiss();
            Toast.makeText(context, "Banner has been modified successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void updateDataToFirebase() {
        if (!descEditText.getText().toString().equals(currBanner.getDescription()))
            FirebaseUtil.getBanner().document(docId).update("description", descEditText.getText().toString());
        
        // Get the current status from dropdown text, or use the status variable if set
        String newStatus = statusDropDown.getText().toString().trim();
        if (newStatus.isEmpty() && status != null) {
            newStatus = status; // Fallback to status variable if dropdown text is empty
        }
        
        // Only update if status has changed
        if (!newStatus.isEmpty() && !newStatus.equals(currBanner.getStatus())) {
            FirebaseUtil.getBanner().document(docId).update("status", newStatus);
            Log.d("ModifyBanner", "Status updated from '" + currBanner.getStatus() + "' to '" + newStatus + "'");
        }
    }

    private boolean validate() {
        boolean isValid = true;
        if (statusDropDown.getText().toString().trim().length() == 0) {
            statusDropDown.setError("Status is required");
            isValid = false;
        }
        if (descEditText.getText().toString().trim().length() == 0) {
            descEditText.setError("Description is required");
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
                        bannerImageView.setImageDrawable(null);
                        bannerImageView.setVisibility(View.GONE);
                        removeImageBtn.setVisibility(View.GONE);
                        alertDialog.cancel();
                    }
                }).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (data != null && data.getData() != null) {
                imageUri = data.getData();
                imageUploaded = true;
                dialog.show();

                Picasso.get().load(imageUri).into(bannerImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                    }
                    @Override
                    public void onError(Exception e) {
                    }
                });
                bannerImageView.setVisibility(View.VISIBLE);
                removeImageBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    public interface MyCallback {
        void onCallback(List<BannerModel> banners, List<String> docIds);
    }
}