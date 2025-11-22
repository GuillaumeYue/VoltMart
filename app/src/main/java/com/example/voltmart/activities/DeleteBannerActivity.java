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
import com.example.voltmart.model.BannerModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * 删除横幅活动页面
 * 管理员删除横幅
 */
public class DeleteBannerActivity extends AppCompatActivity {

    LinearLayout detailsLinearLayout;
    TextView descTextView, statusTextView;
    Button deleteBannerBtn;
    ImageView backBtn, bannerImageView;

    AutoCompleteTextView idDropDown;
    ArrayAdapter<String> idAdapter;
    BannerModel currBanner;
    String docId;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_banner);

        detailsLinearLayout = findViewById(R.id.detailsLinearLayout);
        idDropDown = findViewById(R.id.idDropDown);
        descTextView = findViewById(R.id.descriptionTextView);
        statusTextView = findViewById(R.id.statusTextView);
        bannerImageView = findViewById(R.id.bannerImageView);

        deleteBannerBtn = findViewById(R.id.deleteBannerBtn);
        backBtn = findViewById(R.id.backBtn);

        deleteBannerBtn.setOnClickListener(v -> {
            deleteFromFirebase();
        });

        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });

        initDropDown(new MyCallback() {
            @Override
            public void onCallback(List<BannerModel> bannerList, List<String> docIdList) {
                String[] ids = new String[bannerList.size()];
                for (int i = 0; i < bannerList.size(); i++)
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
                            List<BannerModel> banners = new ArrayList<>();
                            List<String> docIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                banners.add(document.toObject(BannerModel.class));
                                docIds.add(document.getId());
                            }
                            myCallback.onCallback(banners, docIds);
                        }
                    }
                });
    }

    private void initBanner(BannerModel model) {
        currBanner = model;

        Picasso.get().load(currBanner.getBannerImage()).into(bannerImageView);

        detailsLinearLayout.setVisibility(View.VISIBLE);
        bannerImageView.setVisibility(View.VISIBLE);

        descTextView.setText("Description: " + currBanner.getDescription());
        statusTextView.setText("Status: " + (currBanner.getStatus() != null ? currBanner.getStatus() : "Not Live"));
    }

    private void deleteFromFirebase() {
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(context, "Please select a banner to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        SweetAlertDialog confirmDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        confirmDialog
                .setTitleText("Are you sure?")
                .setContentText("This action cannot be undone! Banner ID: " + (currBanner != null ? currBanner.getBannerId() : ""))
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

                        // Delete banner image from storage using bannerId
                        if (currBanner != null) {
                            try {
                                FirebaseUtil.getBannerImageReference(String.valueOf(currBanner.getBannerId()))
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("DeleteBanner", "Image deleted successfully");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("DeleteBanner", "Failed to delete image", e);
                                            // Continue with document deletion even if image deletion fails
                                        });
                            } catch (Exception e) {
                                Log.e("DeleteBanner", "Error deleting image", e);
                            }
                        }

                        // Delete banner document from Firestore
                        FirebaseUtil.getBanner().document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Banner deleted successfully!", Toast.LENGTH_SHORT).show();
                                    
                                    // Clear UI
                                    detailsLinearLayout.setVisibility(View.GONE);
                                    bannerImageView.setVisibility(View.GONE);
                                    idDropDown.setText("");
                                    docId = null;
                                    currBanner = null;
                                    
                                    // Refresh dropdown
                                    initDropDown(new MyCallback() {
                                        @Override
                                        public void onCallback(List<BannerModel> bannerList, List<String> docIdList) {
                                            String[] ids = new String[bannerList.size()];
                                            for (int i = 0; i < bannerList.size(); i++)
                                                ids[i] = Integer.toString(bannerList.get(i).getBannerId());

                                            idAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, ids);
                                            idDropDown.setAdapter(idAdapter);
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Failed to delete banner: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("DeleteBanner", "Error deleting banner", e);
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
        void onCallback(List<BannerModel> banners, List<String> docIds);
    }
}

