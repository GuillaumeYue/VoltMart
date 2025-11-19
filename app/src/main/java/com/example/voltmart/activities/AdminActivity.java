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
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class AdminActivity extends AppCompatActivity {

    LinearLayout logoutBtn;
    CardView addProductBtn, modifyProductBtn, addCategoryBtn, modifyCategoryBtn, addBannerBtn, modifyBannerBtn;
    CardView ordersCardView, usersCardView;
    TextView countOrders, usersCount;

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
        usersCount = findViewById(R.id.usersCount);
        ordersCardView = findViewById(R.id.ordersCardView);
        usersCardView = findViewById(R.id.usersCardView);

        getDetails();
        getUserCount();
        
        // Set click listeners for the cards
        ordersCardView.setOnClickListener(v -> {
            startActivity(new Intent(this, OrdersListActivity.class));
        });
        
        usersCardView.setOnClickListener(v -> {
            startActivity(new Intent(this, UsersListActivity.class));
        });

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
                            return;
                        }

                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot == null || !snapshot.exists()) {
                            // 文档不存在，给默认值
                            countOrders.setText("0");
                            return;
                        }

                        Object countObj = snapshot.get("countOfOrderedItems");
                        Object priceObj = snapshot.get("priceOfOrders");

                        // 防止空指针，如果是 null，就显示 0
                        String countText = (countObj == null) ? "0" : countObj.toString();

                        countOrders.setText(countText);
                    }
                });
    }

    private void getUserCount() {
        // First try to count from users collection (if it exists)
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnCompleteListener(usersTask -> {
                    if (usersTask.isSuccessful() && usersTask.getResult() != null) {
                        QuerySnapshot usersSnapshot = usersTask.getResult();
                        int userCount = usersSnapshot.size();
                        Log.d("AdminActivity", "Found " + userCount + " users in users collection");
                        if (userCount > 0) {
                            usersCount.setText(String.valueOf(userCount));
                            return;
                        }
                    }
                    
                    // If users collection is empty or doesn't exist, count from orders collection
                    // Each document in the orders collection represents a user who has placed orders
                    Log.d("AdminActivity", "Users collection empty or doesn't exist, counting from orders collection");
                    FirebaseFirestore.getInstance().collection("orders")
                            .get()
                            .addOnCompleteListener(ordersTask -> {
                                if (ordersTask.isSuccessful() && ordersTask.getResult() != null) {
                                    QuerySnapshot ordersSnapshot = ordersTask.getResult();
                                    int userCount = ordersSnapshot.size();
                                    Log.d("AdminActivity", "Found " + userCount + " users from orders collection");
                                    usersCount.setText(String.valueOf(userCount));
                                } else {
                                    Log.e("AdminActivity", "Failed to get user count: " + 
                                        (ordersTask.getException() != null ? ordersTask.getException().getMessage() : "Unknown error"));
                                    usersCount.setText("0");
                                }
                            });
                });
    }

}