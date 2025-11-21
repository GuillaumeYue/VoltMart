package com.example.voltmart.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.adapters.WishlistProductAdapter;
import com.example.voltmart.model.CartItemModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

/**
 * 愿望单Fragment
 * 显示用户添加到愿望单的商品列表
 * 功能包括：
 * - 显示愿望单商品
 * - 从愿望单移除商品
 * - 将愿望单商品添加到购物车
 */
public class WishlistFragment extends Fragment {

    // UI组件
    RecyclerView productRecyclerView;  // 愿望单商品列表RecyclerView
    WishlistProductAdapter productAdapter; // 愿望单商品适配器
    ImageView backBtn;                 // 返回按钮

    /**
     * 无参构造函数
     * Fragment需要无参构造函数
     */
    public WishlistFragment() {
        // Required empty public constructor
    }

    /**
     * 创建Fragment视图
     * 初始化UI组件并加载愿望单商品
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishlist, container, false);

        MainActivity activity = (MainActivity) getActivity();
        activity.hideSearchBar(); // 隐藏搜索栏

        // 初始化UI组件
        productRecyclerView = view.findViewById(R.id.wishlistRecyclerView);
        backBtn = view.findViewById(R.id.backBtn);
        // 设置返回按钮点击事件
        backBtn.setOnClickListener(v -> {
            activity.onBackPressed();
        });

        initProducts(); // 初始化愿望单商品

        return view;
    }

    /**
     * 初始化愿望单商品
     * 从Firebase加载用户的愿望单商品并显示
     */
    private void initProducts() {
        Query query = FirebaseUtil.getWishlistItems().orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<CartItemModel> options = new FirestoreRecyclerOptions.Builder<CartItemModel>()
                .setQuery(query, CartItemModel.class)
                .build();

        productAdapter = new WishlistProductAdapter(options, getActivity());
        productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        productRecyclerView.setAdapter(productAdapter);
        productAdapter.startListening();
    }
}