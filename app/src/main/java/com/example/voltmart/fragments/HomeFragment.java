package com.example.voltmart.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;

/**
 * HomeFragment – 显示主页内容（无 shimmer，基础控件版）
 */
public class HomeFragment extends Fragment {

    private ProgressBar progressBar;
    private LinearLayout mainLinearLayout;
    private RecyclerView categoryRecyclerView, productRecyclerView;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 绑定视图
        progressBar = view.findViewById(R.id.progressBar);
        mainLinearLayout = view.findViewById(R.id.mainLinearLayout);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        productRecyclerView = view.findViewById(R.id.productRecyclerView);

        // 模拟加载中
        showLoading(true);

        // 这里你可以改成真正的数据库或 API 调用
        view.postDelayed(() -> {
            initCategoryList();
            initProductList();
            showLoading(false);
        }, 1000);

        return view;
    }

    private void initCategoryList() {
        // 用最基础的测试数据
        categoryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        categoryRecyclerView.setAdapter(new SimpleTextAdapter(new String[]{
                "Phone", "Laptop", "Tablet", "Headset"
        }));
    }

    private void initProductList() {
        productRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productRecyclerView.setAdapter(new SimpleTextAdapter(new String[]{
                "iPhone 15", "Galaxy S24", "MacBook Air", "Pixel 9"
        }));
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        mainLinearLayout.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    /**
     * 简单的 Adapter – 只显示文本占位
     */
    private static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
        private final String[] data;

        SimpleTextAdapter(String[] data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 24, 16, 24);
            tv.setTextSize(16);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(data[position]);
        }

        @Override
        public int getItemCount() {
            return data.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }
}
