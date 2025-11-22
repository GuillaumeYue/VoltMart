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
import com.example.voltmart.model.UserModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashSet;
import java.util.Set;

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
    CardView deleteProductBtn;   // 删除产品按钮
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
        deleteProductBtn = findViewById(R.id.deleteProductBtn);
        countOrders = findViewById(R.id.countOrders);
        usersCount = findViewById(R.id.usersCount);
        ordersCardView = findViewById(R.id.ordersCardView);
        usersCardView = findViewById(R.id.usersCardView);

        getDetails();    // 获取订单统计
        getUserCount();  // 获取用户数量
        
        // 自动同步用户：从orders、cart、wishlists等集合中提取用户信息
        syncUsersFromCollections();
        
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
        
        // 设置删除产品按钮点击事件
        deleteProductBtn.setOnClickListener(v -> startActivity(new Intent(this, DeleteProductActivity.class)));
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

    /**
     * 获取用户数量
     * 从Firebase的users集合中获取所有用户并统计数量
     */
    private void getUserCount() {
        FirebaseUtil.getUsers()
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int userCount = task.getResult().size();
                            Log.d("AdminActivity", "Found " + userCount + " users in users collection");
                            usersCount.setText(String.valueOf(userCount));
                        } else {
                            Log.e("AdminActivity", "Failed to get user count: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                            usersCount.setText("0");
                        }
                    }
                });
    }

    /**
     * 从orders、cart、wishlists等集合中同步用户信息到Firestore
     * 提取所有唯一的用户ID，并为每个用户创建或更新Firestore记录
     */
    private void syncUsersFromCollections() {
        Set<String> userIds = new HashSet<>();
        
        // 首先确保当前登录的管理员被添加到集合中
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userIds.add(currentUser.getUid());
            Log.d("AdminActivity", "Added current logged-in user: " + currentUser.getUid());
        }
        
        // 1. 从orders集合中提取用户ID（orders/{uid}/items）
        FirebaseFirestore.getInstance().collection("orders")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int ordersCount = task.getResult().size();
                            Log.d("AdminActivity", "Orders collection size: " + ordersCount);
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String uid = document.getId();
                                if (uid != null && !uid.equals("dummy")) {
                                    userIds.add(uid);
                                    Log.d("AdminActivity", "Found user from orders: " + uid);
                                }
                            }
                            Log.d("AdminActivity", "Found " + userIds.size() + " users from orders collection");
                            
                            // 2. 从cart集合中提取用户ID（cart/{uid}/items）
                            FirebaseFirestore.getInstance().collection("cart")
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful() && task.getResult() != null) {
                                                int cartCount = task.getResult().size();
                                                Log.d("AdminActivity", "Cart collection size: " + cartCount);
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    String uid = document.getId();
                                                    if (uid != null && !uid.equals("dummy")) {
                                                        userIds.add(uid);
                                                        Log.d("AdminActivity", "Found user from cart: " + uid);
                                                    }
                                                }
                                                Log.d("AdminActivity", "Total unique users found so far: " + userIds.size());
                                                
                                                // 3. 从wishlists集合中提取用户ID（wishlists/{uid}/items）
                                                FirebaseFirestore.getInstance().collection("wishlists")
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful() && task.getResult() != null) {
                                                                    int wishlistsCount = task.getResult().size();
                                                                    Log.d("AdminActivity", "Wishlists collection size: " + wishlistsCount);
                                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                                        String uid = document.getId();
                                                                        if (uid != null && !uid.equals("dummy")) {
                                                                            userIds.add(uid);
                                                                            Log.d("AdminActivity", "Found user from wishlists: " + uid);
                                                                        }
                                                                    }
                                                                    Log.d("AdminActivity", "Final unique users count: " + userIds.size());
                                                                    
                                                                    // 4. 为每个用户ID创建或更新Firestore记录
                                                                    syncUsersToFirestore(userIds);
                                                                } else {
                                                                    Log.e("AdminActivity", "Error getting wishlists", task.getException());
                                                                    // 即使wishlists查询失败，也继续同步已找到的用户
                                                                    syncUsersToFirestore(userIds);
                                                                }
                                                            }
                                                        });
                                            } else {
                                                Log.e("AdminActivity", "Error getting cart", task.getException());
                                                // 即使cart查询失败，也继续同步已找到的用户
                                                FirebaseFirestore.getInstance().collection("wishlists")
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful() && task.getResult() != null) {
                                                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                                                        String uid = document.getId();
                                                                        if (uid != null && !uid.equals("dummy")) {
                                                                            userIds.add(uid);
                                                                        }
                                                                    }
                                                                    syncUsersToFirestore(userIds);
                                                                } else {
                                                                    syncUsersToFirestore(userIds);
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        } else {
                            Log.e("AdminActivity", "Error getting orders", task.getException());
                            // 即使orders查询失败，也继续同步当前用户
                            syncUsersToFirestore(userIds);
                        }
                    }
                });
    }

    /**
     * 为每个用户ID创建或更新Firestore用户记录
     * @param userIds 用户ID集合
     */
    private void syncUsersToFirestore(Set<String> userIds) {
        Log.d("AdminActivity", "syncUsersToFirestore called with " + userIds.size() + " users");
        if (userIds.isEmpty()) {
            Log.d("AdminActivity", "No users to sync, but ensuring current user is synced");
            // 即使集合为空，也确保当前登录用户被同步
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userIds.add(currentUser.getUid());
                Log.d("AdminActivity", "Added current user to sync: " + currentUser.getUid());
            } else {
                Log.w("AdminActivity", "No current user found and no users to sync");
                return;
            }
        }
        
        int[] syncedCount = {0};
        int[] totalCount = {userIds.size()};
        
        for (String uid : userIds) {
            // 检查用户是否已存在于Firestore中
            FirebaseUtil.getUsers().document(uid).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                // 如果用户不存在，创建用户记录
                                if (document == null || !document.exists()) {
                                    // 尝试从orders中获取用户信息（email, name等）
                                    FirebaseFirestore.getInstance().collection("orders").document(uid)
                                            .collection("items").limit(1).get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> orderTask) {
                                                    String email = "";
                                                    String displayName = "";
                                                    String phoneNumber = "";
                                                    
                                                    // 从订单中提取用户信息
                                                    if (orderTask.isSuccessful() && orderTask.getResult() != null 
                                                            && !orderTask.getResult().isEmpty()) {
                                                        QueryDocumentSnapshot orderDoc = (QueryDocumentSnapshot) orderTask.getResult().getDocuments().get(0);
                                                        email = orderDoc.getString("email");
                                                        displayName = orderDoc.getString("fullName");
                                                        phoneNumber = orderDoc.getString("phoneNumber");
                                                    }
                                                    
                                                    // 如果从订单中无法获取email，尝试从当前登录用户获取
                                                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                                    if (currentUser != null && currentUser.getUid().equals(uid)) {
                                                        if (email == null || email.isEmpty()) {
                                                            email = currentUser.getEmail();
                                                        }
                                                        if (displayName == null || displayName.isEmpty()) {
                                                            displayName = currentUser.getDisplayName();
                                                        }
                                                        if (phoneNumber == null || phoneNumber.isEmpty()) {
                                                            phoneNumber = currentUser.getPhoneNumber();
                                                        }
                                                        Log.d("AdminActivity", "Using current user info for uid: " + uid + ", email: " + email);
                                                    }
                                                    
                                                    // 创建用户模型
                                                    UserModel userModel = new UserModel(
                                                            uid,
                                                            email != null ? email : "",
                                                            displayName != null ? displayName : "",
                                                            phoneNumber != null ? phoneNumber : ""
                                                    );
                                                    
                                                    // 保存到Firestore
                                                    FirebaseUtil.getUsers().document(uid).set(userModel)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    syncedCount[0]++;
                                                                    if (task.isSuccessful()) {
                                                                        Log.d("AdminActivity", "Synced user: " + uid);
                                                                    } else {
                                                                        Log.e("AdminActivity", "Failed to sync user: " + uid, task.getException());
                                                                    }
                                                                    
                                                                    // 如果所有用户都处理完成，更新用户数显示
                                                                    if (syncedCount[0] >= totalCount[0]) {
                                                                        Log.d("AdminActivity", "User sync completed. Synced " + syncedCount[0] + " users");
                                                                        getUserCount(); // 刷新用户数显示
                                                                        if (syncedCount[0] > 0) {
                                                                            Toast.makeText(AdminActivity.this, 
                                                                                    "Synced " + syncedCount[0] + " user(s) from collections", 
                                                                                    Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                }
                                            });
                                } else {
                                    // 用户已存在，跳过
                                    syncedCount[0]++;
                                    if (syncedCount[0] >= totalCount[0]) {
                                        getUserCount(); // 刷新用户数显示
                                    }
                                }
                            }
                        }
                    });
        }
    }

}