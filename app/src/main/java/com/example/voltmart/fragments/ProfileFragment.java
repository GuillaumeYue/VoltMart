package com.example.voltmart.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.activities.SplashActivity;
import com.example.voltmart.adapters.OrderListAdapter;
import com.example.voltmart.model.OrderItemModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Query;

public class ProfileFragment extends Fragment {
    RecyclerView orderRecyclerView;
    OrderListAdapter orderAdapter;
    LinearLayout logoutBtn;
    TextView userNameTextView;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        orderRecyclerView = view.findViewById(R.id.orderRecyclerView);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        userNameTextView = view.findViewById(R.id.userNameTextView);

        // 安全地设置用户名称 - 添加空值检查
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // 用户已登录
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userNameTextView.setText("Hello, " + displayName);
            } else {
                // 使用邮箱作为后备
                String email = currentUser.getEmail();
                if (email != null && !email.isEmpty()) {
                    userNameTextView.setText("Hello, " + email.split("@")[0]);
                } else {
                    userNameTextView.setText("Hello, User");
                }
            }

            // 设置退出登录按钮
            logoutBtn.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });

            // 加载订单数据
            getOrderProducts();
        } else {
            // 用户未登录
            userNameTextView.setText("Please Log In");
            logoutBtn.setVisibility(View.GONE); // 隐藏退出按钮
            // 可以添加登录按钮或其他处理
        }

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.hideSearchBar();
        }

        return view;
    }

    private void getOrderProducts() {
        // 检查用户是否登录
        if (!FirebaseUtil.isUserLoggedIn()) {
            return;
        }

        Query query = FirebaseUtil.getOrderItems().orderBy("timestamp", Query.Direction.DESCENDING);

        // 检查是否返回了虚拟集合
        if (query.toString().contains("dummy_collection")) {
            return;
        }

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
    public void onStart() {
        super.onStart();
        if (orderAdapter != null) {
            orderAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (orderAdapter != null) {
            orderAdapter.stopListening();
        }
    }
}