package com.example.voltmart.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.adapters.CategoryAdapter;
import com.example.voltmart.adapters.ProductAdapter;
import com.example.voltmart.model.CategoryModel;
import com.example.voltmart.model.ProductModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

/**
 * 首页Fragment
 * 显示应用的主页内容，包括：
 * - 横幅轮播图（Banner）
 * - 商品分类列表
 * - 商品列表
 * 使用Shimmer效果显示加载状态
 */
public class HomeFragment extends Fragment {

    // UI组件
    RecyclerView categoryRecyclerView;  // 分类列表RecyclerView
    RecyclerView productRecyclerView;    // 商品列表RecyclerView
    MaterialSearchBar searchBar;        // 搜索栏
    ImageCarousel carousel;             // 横幅轮播图
    ShimmerFrameLayout shimmerFrameLayout; // Shimmer加载效果布局
    LinearLayout mainLinearLayout;      // 主内容布局
    TextView sortBtn;                    // 排序按钮

    // 适配器
    CategoryAdapter categoryAdapter;  // 分类适配器
    ProductAdapter productAdapter;     // 商品适配器
    
    // 排序状态
    String currentSort = "default";     // 当前排序方式：default, price_asc, price_desc

//    TextView textView;

    /**
     * 无参构造函数
     * Fragment需要无参构造函数
     */
    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * 创建Fragment视图
     * 初始化UI组件并加载数据
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // 填充Fragment布局
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        searchBar = getActivity().findViewById(R.id.searchBar);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        carousel = view.findViewById(R.id.carousel);
        shimmerFrameLayout = view.findViewById(R.id.shimmerLayout);
        mainLinearLayout = view.findViewById(R.id.mainLinearLayout);
        sortBtn = view.findViewById(R.id.sortBtn);

        MainActivity activity = (MainActivity) getActivity();
        activity.showSearchBar(); // 显示搜索栏
        shimmerFrameLayout.startShimmer(); // 启动Shimmer加载效果

        // 设置排序按钮点击事件
        sortBtn.setOnClickListener(v -> showSortDialog());

        // 初始化各个组件
        initCarousel();    // 初始化横幅轮播图
        initCategories();  // 初始化分类列表
        initProducts();    // 初始化商品列表

        return view;
    }

    /**
     * 初始化横幅轮播图
     * 从Firebase获取横幅数据并添加到轮播图中
     */
    private void initCarousel() {
        FirebaseUtil.getBanner().orderBy("bannerId").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    // 遍历所有横幅文档，添加到轮播图
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        carousel.addData(new CarouselItem(document.get("bannerImage").toString()));
                    }
                }
            }
        });
    }

    private void initCategories() {
        // First check if categories exist
        FirebaseUtil.getCategories().get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int categoryCount = task.getResult().size();
                            Log.d("HomeFragment", "Categories count = " + categoryCount);
                            
                            if (categoryCount == 0) {
                                Log.w("HomeFragment", "No categories found in database");
                            } else {
                                Log.d("HomeFragment", "Categories found, should be displaying");
                                // Make sure main layout is visible when categories load
                                if (mainLinearLayout != null && mainLinearLayout.getVisibility() != View.VISIBLE) {
                                    shimmerFrameLayout.stopShimmer();
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    mainLinearLayout.setVisibility(View.VISIBLE);
                                    Log.d("HomeFragment", "Made mainLinearLayout visible after categories loaded");
                                    
                                    // Force RecyclerView to layout after parent becomes visible
                                    if (categoryRecyclerView != null) {
                                        categoryRecyclerView.post(() -> {
                                            categoryRecyclerView.requestLayout();
                                            categoryRecyclerView.invalidate();
                                            Log.d("HomeFragment", "Requested layout for categoryRecyclerView after parent became visible");
                                        });
                                    }
                                }
                            }
                        } else {
                            Log.e("HomeFragment", "Error loading categories", task.getException());
                        }
                    }
                });
        
        Query query = FirebaseUtil.getCategories();
        FirestoreRecyclerOptions<CategoryModel> options = new FirestoreRecyclerOptions.Builder<CategoryModel>()
                .setQuery(query, CategoryModel.class)
                .build();

        categoryAdapter = new CategoryAdapter(options, getContext());
        
        // Set up RecyclerView - For NestedScrollView, we need to disable nested scrolling
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        categoryRecyclerView.setLayoutManager(layoutManager);
        categoryRecyclerView.setHasFixedSize(false); // Must be false for wrap_content in NestedScrollView
        categoryRecyclerView.setNestedScrollingEnabled(false); // Critical for NestedScrollView
        
        // Ensure RecyclerView is visible
        categoryRecyclerView.setVisibility(View.VISIBLE);
        categoryRecyclerView.setAdapter(categoryAdapter);
        
        Log.d("HomeFragment", "Category adapter created, starting to listen");
        Log.d("HomeFragment", "RecyclerView visibility: " + (categoryRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
        Log.d("HomeFragment", "RecyclerView adapter: " + (categoryRecyclerView.getAdapter() != null ? "SET" : "NULL"));
        
        categoryAdapter.startListening();
        
        // Wait for data to load, then force layout
        categoryRecyclerView.postDelayed(() -> {
            if (categoryAdapter != null && categoryAdapter.getItemCount() > 0) {
                Log.d("HomeFragment", "Category adapter itemCount: " + categoryAdapter.getItemCount());
                Log.d("HomeFragment", "RecyclerView measured width: " + categoryRecyclerView.getMeasuredWidth() + ", height: " + categoryRecyclerView.getMeasuredHeight());
                
                // Force RecyclerView to measure and layout
                categoryRecyclerView.measure(
                    View.MeasureSpec.makeMeasureSpec(categoryRecyclerView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                categoryRecyclerView.requestLayout();
                
                // Check child count after layout
                categoryRecyclerView.post(() -> {
                    int childCount = categoryRecyclerView.getChildCount();
                    Log.d("HomeFragment", "RecyclerView child count after layout: " + childCount);
                    if (childCount == 0) {
                        Log.e("HomeFragment", "CRITICAL: RecyclerView has NO children despite " + categoryAdapter.getItemCount() + " items!");
                        // Try invalidating and requesting layout again
                        categoryRecyclerView.invalidate();
                        categoryRecyclerView.requestLayout();
                    }
                });
            }
        }, 1000);
    }

    private void initProducts() {
        FirebaseUtil.getProducts()
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        Log.d("HomeFragment", "Products count = " + count);

                        // ✨ 关键三行：关 shimmer / 显示主内容
                        shimmerFrameLayout.stopShimmer();
                        shimmerFrameLayout.setVisibility(View.GONE);
                        mainLinearLayout.setVisibility(View.VISIBLE);
                    }

                });

        loadProductsWithSort();
    }

    private void loadProductsWithSort() {
        Query query = FirebaseUtil.getProducts();
        
        if (currentSort.equals("price_asc")) {
            query = query.orderBy("price", Query.Direction.ASCENDING);
        } else if (currentSort.equals("price_desc")) {
            query = query.orderBy("price", Query.Direction.DESCENDING);
        }
        
        FirestoreRecyclerOptions<ProductModel> options = new FirestoreRecyclerOptions.Builder<ProductModel>()
                .setQuery(query, ProductModel.class)
                .build();

        if (productAdapter != null) {
            productAdapter.stopListening();
        }
        
        productAdapter = new ProductAdapter(options, getContext());
        if (productRecyclerView.getLayoutManager() == null) {
            productRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        productRecyclerView.setAdapter(productAdapter);
        productAdapter.startListening();
    }

    private void showSortDialog() {
        String[] sortOptions = {
            "Default",
            "Price: Low to High",
            "Price: High to Low"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Sort Products");
        builder.setItems(sortOptions, (dialog, which) -> {
            String newSort = "default";
            switch (which) {
                case 0:
                    newSort = "default";
                    break;
                case 1:
                    newSort = "price_asc";
                    break;
                case 2:
                    newSort = "price_desc";
                    break;
            }
            
            if (!newSort.equals(currentSort)) {
                currentSort = newSort;
                loadProductsWithSort();
            }
            dialog.dismiss();
        });
        builder.show();
    }

}
