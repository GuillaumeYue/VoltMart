package com.example.voltmart.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voltmart.R;
import com.example.voltmart.activities.CheckoutActivity;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.adapters.CartAdapter;
import com.example.voltmart.model.CartItemModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * 购物车Fragment
 * 显示用户购物车中的商品列表
 * 功能包括：
 * - 显示购物车商品
 * - 修改商品数量
 * - 删除商品
 * - 计算总价
 * - 跳转到结账页面
 */
public class CartFragment extends Fragment implements CartAdapter.CartAdapterListener {
    // UI组件
    TextView cartPriceTextView;      // 购物车总价显示
    RecyclerView cartRecyclerView;   // 购物车商品列表
    Button continueBtn;              // 继续结账按钮
    Button clearCartBtn;              // 清空购物车按钮
    ImageView backBtn;               // 返回按钮
    ImageView emptyCartImageView;    // 空购物车图片
    CartAdapter cartAdapter;         // 购物车适配器
    int totalPrice = 0;              // 购物车总价

    // 加载状态UI
    ShimmerFrameLayout shimmerFrameLayout; // Shimmer加载效果
    LinearLayout mainLinearLayout;        // 主内容布局

    /**
     * 无参构造函数
     * Fragment需要无参构造函数
     */
    public CartFragment() {
        // Required empty public constructor
    }

    /**
     * 创建Fragment视图
     * 初始化UI组件并加载购物车数据
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        // 初始化UI组件
        cartPriceTextView = view.findViewById(R.id.cartPriceTextView);
        cartRecyclerView = view.findViewById(R.id.cartRecyclerView);
        continueBtn = view.findViewById(R.id.continueBtn);
        clearCartBtn = view.findViewById(R.id.clearCartBtn);
        backBtn = view.findViewById(R.id.backBtn);
        emptyCartImageView = view.findViewById(R.id.emptyCartImageView);
        shimmerFrameLayout = view.findViewById(R.id.shimmerLayout);
        mainLinearLayout = view.findViewById(R.id.mainLinearLayout);

        MainActivity activity = (MainActivity) getActivity();
        activity.hideSearchBar(); // 隐藏搜索栏
        shimmerFrameLayout.startShimmer(); // 启动Shimmer加载效果
        emptyCartImageView.setVisibility(View.INVISIBLE); // 隐藏空购物车图片

        getCartProducts(); // 获取购物车商品

        // 移除可能存在的分隔线装饰
        for (int i = 0; i < cartRecyclerView.getItemDecorationCount(); i++) {
            if (cartRecyclerView.getItemDecorationAt(i) instanceof DividerItemDecoration)
                cartRecyclerView.removeItemDecorationAt(i);
        }

        // 设置继续结账按钮点击事件
        continueBtn.setOnClickListener(v -> {
            if (totalPrice == 0) {
                // 购物车为空时提示用户
                Toast.makeText(activity, "Your cart is empty! Add some product in your cart to proceed.", Toast.LENGTH_SHORT).show();
                return;
            }
            // 跳转到结账页面，传递总价
            Intent intent = new Intent(getActivity(), CheckoutActivity.class);
            intent.putExtra("price", totalPrice);
            startActivity(intent);
        });

        // 设置清空购物车按钮点击事件
        clearCartBtn.setOnClickListener(v -> {
            clearCart();
        });

        // 设置返回按钮点击事件
        backBtn.setOnClickListener(v -> {
            activity.onBackPressed();
        });

        return view;
    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            totalPrice = intent.getIntExtra("totalPrice", 1000);
//            Log.i("Price", totalPrice+"");
            cartPriceTextView.setText("$ " + totalPrice);
        }
    };

    private void getCartProducts() {
        Query query = FirebaseUtil.getCartItems().orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<CartItemModel> options = new FirestoreRecyclerOptions.Builder<CartItemModel>()
                .setQuery(query, CartItemModel.class)
                .build();

        emptyCartImageView.setVisibility(View.INVISIBLE);
        cartAdapter = new CartAdapter(options, getActivity());
        cartAdapter.setCartAdapterListener(this);
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        cartRecyclerView.setLayoutManager(manager);
        cartRecyclerView.setAdapter(cartAdapter);
        cartAdapter.startListening();
    }

    @Override
    public void onCartEmpty() {
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
        if (mainLinearLayout != null) {
            mainLinearLayout.setVisibility(View.VISIBLE);
        }
        if (emptyCartImageView != null) {
            emptyCartImageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCartHasItems() {
        if (emptyCartImageView != null) {
            emptyCartImageView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onItemsLoaded() {
        // Stop shimmer and show main layout when items finish loading
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
        if (mainLinearLayout != null) {
            mainLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    private void clearCart() {
        if (totalPrice == 0) {
            Toast.makeText(getActivity(), "Your cart is already empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        SweetAlertDialog alertDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE);
        alertDialog
                .setTitleText("Clear Cart?")
                .setContentText("Are you sure you want to remove all items from your cart? This action cannot be undone.")
                .setConfirmText("Yes, Clear")
                .setCancelText("Cancel")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                        
                        SweetAlertDialog progressDialog = new SweetAlertDialog(getActivity(), SweetAlertDialog.PROGRESS_TYPE);
                        progressDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                        progressDialog.setTitleText("Clearing cart...");
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        FirebaseUtil.getCartItems().get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    int itemCount = task.getResult().size();
                                    if (itemCount == 0) {
                                        progressDialog.dismissWithAnimation();
                                        Toast.makeText(getActivity(), "Cart is already empty!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    final int[] deletedCount = {0};
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String docId = document.getId();
                                        FirebaseUtil.getCartItems().document(docId)
                                                .delete()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> deleteTask) {
                                                        deletedCount[0]++;
                                                        if (deletedCount[0] == itemCount) {
                                                            progressDialog.dismissWithAnimation();
                                                            new SweetAlertDialog(getActivity(), SweetAlertDialog.SUCCESS_TYPE)
                                                                    .setTitleText("Cart Cleared!")
                                                                    .setContentText("All items have been removed from your cart")
                                                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                                        @Override
                                                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                                            sweetAlertDialog.dismissWithAnimation();
                                                                        }
                                                                    })
                                                                    .show();
                                                            
                                                            MainActivity activity = (MainActivity) getActivity();
                                                            if (activity != null) {
                                                                activity.addOrRemoveBadge();
                                                            }
                                                            
                                                            totalPrice = 0;
                                                            cartPriceTextView.setText("$ 0");
                                                        }
                                                    }
                                                });
                                    }
                                } else {
                                    progressDialog.dismissWithAnimation();
                                    Toast.makeText(getActivity(), "Failed to clear cart. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.cancel();
                    }
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("price"));
        }
        // Restart adapter listening when fragment resumes
        // This is important when returning from checkout after items are deleted
        if (cartAdapter != null) {
            cartAdapter.startListening();
        } else {
            // If adapter is null (e.g., after checkout), recreate it
            getCartProducts();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        }
        if (cartAdapter != null) {
            cartAdapter.stopListening();
        }
    }
}