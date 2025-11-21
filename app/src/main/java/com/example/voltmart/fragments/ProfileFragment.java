package com.example.voltmart.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.activities.SplashActivity;
import com.example.voltmart.adapters.OrderListAdapter;
import com.example.voltmart.model.OrderItemModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.Query;

/**
 * 个人资料Fragment
 * 显示用户个人信息和订单历史
 * 功能包括：
 * - 显示用户名称
 * - 编辑用户名称
 * - 显示订单历史
 * - 退出登录
 */
public class ProfileFragment extends Fragment {
    // UI组件
    RecyclerView orderRecyclerView;  // 订单列表RecyclerView
    OrderListAdapter orderAdapter;   // 订单列表适配器
    LinearLayout logoutBtn;          // 退出登录按钮
    TextView userNameTextView;       // 用户名称显示
    ImageView editNameBtn;           // 编辑名称按钮

    /**
     * 无参构造函数
     * Fragment需要无参构造函数
     */
    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * 创建Fragment视图
     * 初始化UI组件并加载用户信息和订单数据
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_profile, container, false);
        // 初始化UI组件
        orderRecyclerView = view.findViewById(R.id.orderRecyclerView);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        userNameTextView = view.findViewById(R.id.userNameTextView);
        editNameBtn = view.findViewById(R.id.editNameBtn);

        updateUserNameDisplay(); // 更新用户名称显示

        // 设置编辑名称按钮点击事件
        editNameBtn.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                showEditNameDialog(); // 显示编辑名称对话框
            } else {
                // 用户未登录，提示需要登录
                Toast.makeText(getActivity(), "Please login to edit your profile", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置退出登录按钮点击事件
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // 退出登录
            // 跳转到启动页面，清除所有活动栈
            Intent intent = new Intent(getActivity(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        
        // 只有用户已登录时才获取订单数据
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            getOrderProducts(); // 获取订单列表
        }

        MainActivity activity = (MainActivity) getActivity();
        activity.hideSearchBar(); // 隐藏搜索栏

        return view;
    }

    /**
     * 更新用户名称显示
     * 根据用户登录状态显示不同的名称
     */
    private void updateUserNameDisplay() {
        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            
            // Use display name if available, otherwise use email, otherwise use "User"
            String userName = (displayName != null && !displayName.isEmpty()) 
                ? displayName 
                : (email != null ? email.split("@")[0] : "User");
            
            userNameTextView.setText("Hello, " + userName);
            editNameBtn.setVisibility(View.VISIBLE);
        } else {
            userNameTextView.setText("Hello, Guest");
            editNameBtn.setVisibility(View.GONE);
        }
    }

    private void showEditNameDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        // Create dialog with EditText
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Edit Name");

        // Set up the input
        final EditText input = new EditText(getActivity());
        String currentName = user.getDisplayName();
        if (currentName != null && !currentName.isEmpty()) {
            input.setText(currentName);
        }
        input.setHint("Enter your name");
        input.setPadding(50, 20, 50, 20);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(getActivity(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                updateUserName(newName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUserName(String newName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                            updateUserNameDisplay();
                        } else {
                            Toast.makeText(getActivity(), "Failed to update name: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getOrderProducts() {
        // Check if user is authenticated before querying orders
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        
        Query query = FirebaseUtil.getOrderItems().orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<OrderItemModel> options = new FirestoreRecyclerOptions.Builder<OrderItemModel>()
                .setQuery(query, OrderItemModel.class)
                .build();

        orderAdapter = new OrderListAdapter(options, getActivity());
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        orderRecyclerView.setLayoutManager(manager);
        orderRecyclerView.setAdapter(orderAdapter);
        orderAdapter.startListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user name display when fragment resumes
        updateUserNameDisplay();
    }
}