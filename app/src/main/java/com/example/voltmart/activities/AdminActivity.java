package com.example.voltmart.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.voltmart.R;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class AdminActivity extends AppCompatActivity {

    LinearLayout logoutBtn;
    CardView addProductBtn, modifyProductBtn, addCategoryBtn, modifyCategoryBtn, addBannerBtn, modifyBannerBtn;
    TextView countOrders, priceOrders, usersTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        logoutBtn = findViewById(R.id.logoutBtn);
        addProductBtn = findViewById(R.id.addProductBtn);
        modifyProductBtn = findViewById(R.id.modifyProductBtn);
        addCategoryBtn = findViewById(R.id.addCategoryBtn);
        modifyCategoryBtn = findViewById(R.id.modifyCategoryBtn);
        addBannerBtn = findViewById(R.id.addBannerBtn);
        modifyBannerBtn = findViewById(R.id.modifyBannerBtn);
        countOrders = findViewById(R.id.countOrders);
        priceOrders = findViewById(R.id.priceOrders);
//        usersTextView = findViewById(R.id.usersTextView);

        getDetails();

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        addProductBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, AddProductActivity.class));
        });

        modifyProductBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, ModifyProductActivity.class));
        });

        addCategoryBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, AddCategoryActivity.class));
        });

        modifyCategoryBtn.setOnClickListener(v -> startActivity(new Intent(this, ModifyCategoryActivity.class)));
        addBannerBtn.setOnClickListener(v -> startActivity(new Intent(this, AddBannerActivity.class)));
        modifyBannerBtn.setOnClickListener(v -> startActivity(new Intent(this, ModifyBannerActivity.class)));
    }

    private void getDetails() {
        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.isSuccessful()) {
                            // 读取失败，给默认值，避免崩溃
                            countOrders.setText("0");
                            priceOrders.setText("0");
                            return;
                        }

                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot == null || !snapshot.exists()) {
                            // 文档不存在，给默认值
                            countOrders.setText("0");
                            priceOrders.setText("0");
                            return;
                        }

                        Object countObj = snapshot.get("countOfOrderedItems");
                        Object priceObj = snapshot.get("priceOfOrders");

                        // 防止空指针，如果是 null，就显示 0
                        String countText = (countObj == null) ? "0" : countObj.toString();
                        String priceText = (priceObj == null) ? "0" : priceObj.toString();

                        countOrders.setText(countText);
                        priceOrders.setText(priceText);
                    }
                });
    }

}