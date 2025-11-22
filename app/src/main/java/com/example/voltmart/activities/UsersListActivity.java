package com.example.voltmart.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.adapters.UsersListAdapter;
import com.example.voltmart.model.UserModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * 用户列表活动页面
 * 管理员查看所有注册用户列表
 * 从Firestore的users集合中获取用户数据
 */
public class UsersListActivity extends AppCompatActivity {

    // UI组件
    private RecyclerView usersRecyclerView; // 用户列表RecyclerView
    private UsersListAdapter usersAdapter;   // 用户列表适配器
    private ImageView backBtn;               // 返回按钮
    private TextView titleTextView;          // 标题文本

    /**
     * 活动创建时的初始化方法
     * 初始化UI组件并加载所有用户
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 启用边缘到边缘显示
        setContentView(R.layout.activity_users_list);

        // 初始化UI组件
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        backBtn = findViewById(R.id.backBtn);
        titleTextView = findViewById(R.id.titleTextView);

        titleTextView.setText("All Users"); // 设置标题

        // 设置返回按钮点击事件
        backBtn.setOnClickListener(v -> onBackPressed());

        // 从users集合查询所有用户
        Query usersQuery = FirebaseUtil.getUsers();
        
        // 创建FirestoreRecyclerOptions，不使用orderBy以防email字段不存在或缺少索引
        FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(usersQuery, UserModel.class)
                .build();

        // 创建并设置适配器
        usersAdapter = new UsersListAdapter(options, this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        usersRecyclerView.setLayoutManager(manager);
        usersRecyclerView.setAdapter(usersAdapter);
        
        // 检查users集合是否有数据，如果没有则记录日志
        FirebaseUtil.getUsers()
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int userCount = task.getResult().size();
                        if (userCount == 0) {
                            Log.d("UsersListActivity", "Users collection is empty");
                        } else {
                            Log.d("UsersListActivity", "Found " + userCount + " user(s) in collection");
                        }
                    } else {
                        Log.e("UsersListActivity", "Error checking users collection", task.getException());
                    }
                });
    }

    /**
     * 活动开始时启动适配器监听
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (usersAdapter != null) {
            usersAdapter.startListening(); // 开始监听Firestore数据变化
        }
    }

    /**
     * 活动停止时停止适配器监听
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (usersAdapter != null) {
            usersAdapter.stopListening(); // 停止监听Firestore数据变化
        }
    }
}

