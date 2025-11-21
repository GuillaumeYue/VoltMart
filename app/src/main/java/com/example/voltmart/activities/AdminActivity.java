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

/**
 * 管理员活动页面
 * 提供管理员功能入口，包括：
 * - 添加/修改商品
 * - 添加/修改分类
 * - 添加/修改横幅
 * - 查看订单列表
 * - 查看用户列表
 * - 查看统计数据
 */
public class AdminActivity extends AppCompatActivity {

    // UI组件
    LinearLayout logoutBtn;  // 退出登录按钮
    CardView addProductBtn;      // 添加商品按钮
    CardView modifyProductBtn;   // 修改商品按钮
    CardView addCategoryBtn;     // 添加分类按钮
    CardView modifyCategoryBtn;  // 修改分类按钮
    CardView addBannerBtn;       // 添加横幅按钮
    CardView modifyBannerBtn;    // 修改横幅按钮
    CardView ordersCardView;     // 订单卡片（可点击）
    CardView usersCardView;      // 用户卡片（可点击）
    TextView countOrders;        // 订单数量显示
    TextView usersCount;         // 用户数量显示

    /**
     * 活动创建时的初始化方法
     * 初始化UI组件、设置点击事件、加载统计数据
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 启用边缘到边缘显示
        setContentView(R.layout.activity_admin);

        // 初始化UI组件
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

        getDetails();    // 获取订单统计
        getUserCount();  // 获取用户数量
        
        // 设置订单卡片的点击事件：跳转到订单列表页面
        ordersCardView.setOnClickListener(v -> {
            startActivity(new Intent(this, OrdersListActivity.class));
        });
        
        // 设置用户卡片的点击事件：跳转到用户列表页面
        usersCardView.setOnClickListener(v -> {
            startActivity(new Intent(this, UsersListActivity.class));
        });

        // 设置退出登录按钮点击事件
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // 退出登录
            // 跳转到启动页面，清除所有活动栈
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 设置添加商品按钮点击事件
        addProductBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, AddProductActivity.class));
        });

        // 设置修改商品按钮点击事件
        modifyProductBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, ModifyProductActivity.class));
        });

        // 设置添加分类按钮点击事件
        addCategoryBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, AddCategoryActivity.class));
        });

        // 设置修改分类按钮点击事件
        modifyCategoryBtn.setOnClickListener(v -> startActivity(new Intent(this, ModifyCategoryActivity.class)));
        // 设置添加横幅按钮点击事件
        addBannerBtn.setOnClickListener(v -> startActivity(new Intent(this, AddBannerActivity.class)));
        // 设置修改横幅按钮点击事件
        modifyBannerBtn.setOnClickListener(v -> startActivity(new Intent(this, ModifyBannerActivity.class)));
    }

    /**
     * 获取订单统计详情
     * 从Firebase获取订单数量并显示
     */
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