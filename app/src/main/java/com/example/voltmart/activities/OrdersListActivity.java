package com.example.voltmart.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.adapters.OrderListAdapter;
import com.example.voltmart.model.OrderItemModel;
import com.example.voltmart.utils.WindowInsetsHelper;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class OrdersListActivity extends AppCompatActivity {

    private RecyclerView ordersRecyclerView;
    private OrderListAdapter orderAdapter;
    private ImageView backBtn;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_orders_list);
        
        LinearLayout topLayout = findViewById(R.id.topHeaderLayout);
        if (topLayout != null) {
            WindowInsetsHelper.applyTopWindowInsets(topLayout, 4);
        }

        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        backBtn = findViewById(R.id.backBtn);
        titleTextView = findViewById(R.id.titleTextView);

        titleTextView.setText("All Orders");

        backBtn.setOnClickListener(v -> onBackPressed());

        // 使用collectionGroup查询获取所有用户的订单项
        // collectionGroup()直接返回Query对象，不是CollectionGroup对象
        Query query = FirebaseFirestore.getInstance().collectionGroup("items")
                .orderBy("timestamp", Query.Direction.DESCENDING); // 按时间戳降序排列

        // 创建FirestoreRecyclerOptions
        FirestoreRecyclerOptions<OrderItemModel> options = new FirestoreRecyclerOptions.Builder<OrderItemModel>()
                .setQuery(query, OrderItemModel.class)
                .build();

        // 创建并设置适配器
        orderAdapter = new OrderListAdapter(options, this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        ordersRecyclerView.setLayoutManager(manager);
        ordersRecyclerView.setAdapter(orderAdapter);
    }

    /**
     * 活动开始时启动适配器监听
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (orderAdapter != null) {
            orderAdapter.startListening(); // 开始监听Firestore数据变化
        }
    }

    /**
     * 活动停止时停止适配器监听
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (orderAdapter != null) {
            orderAdapter.stopListening(); // 停止监听Firestore数据变化
        }
    }
}

