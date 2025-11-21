package com.example.voltmart.fragments;

import static com.example.voltmart.utils.FirebaseUtil.getProducts;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.adapters.SearchAdapter;
import com.example.voltmart.model.ProductModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

/**
 * 分类商品Fragment
 * 显示指定分类下的所有商品
 * 功能包括：
 * - 根据分类名称查询商品
 * - 支持多种分类名称格式（原始、小写、规范化等）
 * - 自动尝试不同的分类名称变体直到找到商品
 */
public class CategoryFragment extends Fragment {

    // UI组件
    RecyclerView productRecyclerView;  // 商品列表RecyclerView
    SearchAdapter searchProductAdapter; // 商品适配器
    ImageView backBtn;                 // 返回按钮
    TextView labelTextView;            // 分类名称标签

    // 数据
    String categoryName;               // 分类名称
    private android.os.Handler categoryCheckHandler; // 分类检查Handler（用于超时检查）

    /**
     * 无参构造函数
     * Fragment需要无参构造函数
     */
    public CategoryFragment() {
        // Required empty public constructor
    }


    /**
     * 创建Fragment视图
     * 初始化UI组件并加载分类商品
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 填充Fragment布局
        View view =  inflater.inflate(R.layout.fragment_category, container, false);
        labelTextView = view.findViewById(R.id.labelTextView);
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        backBtn = view.findViewById(R.id.backBtn);

        // 从参数中获取分类名称（带空值安全检查）
        Bundle args = getArguments();
        if (args != null) {
            categoryName = args.getString("categoryName", "Electronics");
        } else {
            categoryName = "Electronics"; // 默认分类
        }

        labelTextView.setText(categoryName); // 显示分类名称
        getProducts(categoryName); // 获取该分类的商品

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.hideSearchBar(); // 隐藏搜索栏

            // 设置返回按钮点击事件
            backBtn.setOnClickListener(v -> {
                if (activity != null) {
                    activity.onBackPressed();
                }
            });
        }
        return view;
    }
    
    /**
     * Fragment视图销毁时的清理方法
     * 清理Handler和停止适配器监听
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理Handler和适配器
        if (categoryCheckHandler != null) {
            categoryCheckHandler.removeCallbacksAndMessages(null); // 移除所有回调
        }
        if (searchProductAdapter != null) {
            searchProductAdapter.stopListening(); // 停止适配器监听
        }
    }

    /**
     * 获取指定分类的商品
     * 尝试多种分类名称格式，直到找到商品
     * @param categoryName 分类名称
     */
    private void getProducts(String categoryName){
        if (getActivity() == null) {
            return;
        }
        
        String normalizedCategory = normalizeCategoryName(categoryName);
        String lowerCategory = categoryName.toLowerCase().trim();
        
        java.util.List<String> categoriesToTry = new java.util.ArrayList<>();
        
        String lower = categoryName.toLowerCase().trim();
        if (lower.contains("gaming") && lower.contains("mouse")) {
            categoriesToTry.add(categoryName);
            categoriesToTry.add("Gaming Mouse");
            categoriesToTry.add("gaming mouse");
            categoriesToTry.add("GamingMouse");
            categoriesToTry.add("gamingmouse");
            categoriesToTry.add("gaming-mouse");
            categoriesToTry.add("gaming_mouse");
            categoriesToTry.add("Gaming-Mouse");
            categoriesToTry.add("Gaming_Mouse");
            categoriesToTry.add("Mouse");
            categoriesToTry.add("mouse");
        } else {
            if (normalizedCategory != null && !normalizedCategory.isEmpty()) {
                categoriesToTry.add(normalizedCategory);
            }
            
            if (!categoriesToTry.contains(lowerCategory)) {
                categoriesToTry.add(lowerCategory);
            }
            
            if (!categoriesToTry.contains(categoryName)) {
                categoriesToTry.add(categoryName);
            }
            
            if (categoryName.contains(" ")) {
                String withHyphen = categoryName.toLowerCase().replace(" ", "-");
                if (!categoriesToTry.contains(withHyphen)) {
                    categoriesToTry.add(withHyphen);
                }
                String withUnderscore = categoryName.toLowerCase().replace(" ", "_");
                if (!categoriesToTry.contains(withUnderscore)) {
                    categoriesToTry.add(withUnderscore);
                }
                String camelCase = toCamelCase(categoryName);
                if (!categoriesToTry.contains(camelCase)) {
                    categoriesToTry.add(camelCase);
                }
            }
        }
        
        android.util.Log.d("CategoryFragment", "Searching products for category: " + categoryName);
        android.util.Log.d("CategoryFragment", "Will try variations: " + categoriesToTry);
        
        tryCategoryVariations(categoriesToTry, 0);
    }
    
    /**
     * Tries category variations sequentially until one works
     */
    private void tryCategoryVariations(java.util.List<String> categoriesToTry, int currentIndex) {
        try {
            if (getActivity() == null || currentIndex >= categoriesToTry.size()) {
                android.util.Log.w("CategoryFragment", "No products found after trying all " + categoriesToTry.size() + " variations");
                return;
            }
            
            String categoryToTry = categoriesToTry.get(currentIndex);
            android.util.Log.d("CategoryFragment", "Trying category variation [" + (currentIndex + 1) + "/" + categoriesToTry.size() + "]: " + categoryToTry);
            
            if (searchProductAdapter != null) {
                try {
                    searchProductAdapter.stopListening();
                } catch (Exception e) {
                    android.util.Log.e("CategoryFragment", "Error stopping adapter", e);
                }
            }
            if (categoryCheckHandler != null) {
                categoryCheckHandler.removeCallbacksAndMessages(null);
            }
            
            Query query = FirebaseUtil.getProducts().whereEqualTo("category", categoryToTry);
            FirestoreRecyclerOptions<ProductModel> options = new FirestoreRecyclerOptions.Builder<ProductModel>()
                    .setQuery(query, ProductModel.class)
                    .build();
            
            searchProductAdapter = new SearchAdapter(options, getActivity());
            if (productRecyclerView.getLayoutManager() == null) {
                productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            }
            productRecyclerView.setAdapter(searchProductAdapter);
            searchProductAdapter.startListening();
            
            final int index = currentIndex;
            final boolean[] foundProducts = {false};
            if (categoryCheckHandler == null) {
                categoryCheckHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            }
            final android.os.Handler handler = categoryCheckHandler;
            
            Runnable checkTimeout = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (getActivity() == null) {
                            return;
                        }
                        if (!foundProducts[0] && searchProductAdapter != null) {
                            int itemCount = searchProductAdapter.getItemCount();
                            android.util.Log.d("CategoryFragment", "Timeout check: itemCount = " + itemCount + " for category: " + categoryToTry);
                            if (itemCount == 0) {
                                if (index < categoriesToTry.size() - 1) {
                                    android.util.Log.w("CategoryFragment", "No products found for: " + categoryToTry + " after timeout, trying next variation...");
                                    tryCategoryVariations(categoriesToTry, index + 1);
                                } else {
                                    android.util.Log.w("CategoryFragment", "No products found after trying all " + categoriesToTry.size() + " variations");
                                }
                            } else {
                                foundProducts[0] = true;
                                android.util.Log.d("CategoryFragment", "Successfully found " + itemCount + " products with category: " + categoryToTry);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CategoryFragment", "Error in timeout check", e);
                    }
                }
            };
            
            handler.postDelayed(checkTimeout, 500);
            
            searchProductAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    try {
                        super.onItemRangeInserted(positionStart, itemCount);
                        if (itemCount > 0 && !foundProducts[0]) {
                            foundProducts[0] = true;
                            handler.removeCallbacks(checkTimeout);
                            android.util.Log.d("CategoryFragment", "Products loaded: " + itemCount + " items with category: " + categoryToTry);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CategoryFragment", "Error in onItemRangeInserted", e);
                    }
                }
                
                @Override
                public void onChanged() {
                    try {
                        super.onChanged();
                        int itemCount = searchProductAdapter.getItemCount();
                        android.util.Log.d("CategoryFragment", "Adapter changed, itemCount: " + itemCount + " for category: " + categoryToTry);
                        
                        if (itemCount > 0 && !foundProducts[0]) {
                            foundProducts[0] = true;
                            handler.removeCallbacks(checkTimeout);
                            android.util.Log.d("CategoryFragment", "Successfully found " + itemCount + " products with category: " + categoryToTry);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CategoryFragment", "Error in onChanged", e);
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("CategoryFragment", "Error in tryCategoryVariations", e);
            if (currentIndex < categoriesToTry.size() - 1) {
                tryCategoryVariations(categoriesToTry, currentIndex + 1);
            }
        }
    }
    
    /**
     * Normalizes category name to match product category values in Firebase
     * Examples:
     * "Smart Phone" -> "phones"
     * "Gaming Mouse" -> "gaming mouse" or "gamingmouse"
     * "Laptop" -> "laptop"
     * "Gaming PC" -> "gamingpc" or "gaming pc"
     * "TV" -> "tv"
     * "Headset" -> "headset"
     */
    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return "";
        }
        
        // Convert to lowercase and trim
        String lower = categoryName.toLowerCase().trim();
        
        // Handle common mappings with special cases
        // "Smart Phone" -> "phones"
        if (lower.contains("smart") && lower.contains("phone")) {
            return "phones";
        }
        if (lower.equals("phone") || lower.equals("phones") || lower.equals("smartphone")) {
            return "phones";
        }
        
        if (lower.contains("gaming") && lower.contains("mouse")) {
            return lower;
        }
        if (lower.equals("gamingmouse") || lower.equals("gaming-mouse") || lower.equals("gaming_mouse")) {
            return lower;
        }
        
        // "Laptop" -> "laptop"
        if (lower.equals("laptop") || lower.equals("laptops")) {
            return "laptop";
        }
        
        // "Gaming PC" -> try "gamingpc" and "gaming pc"
        if (lower.contains("gaming") && lower.contains("pc")) {
            return "gamingpc"; // Most likely format
        }
        if (lower.equals("gamingpc") || lower.equals("gaming pc")) {
            return "gamingpc";
        }
        
        // "Headset" -> "headset"
        if (lower.equals("headset") || lower.equals("headsets")) {
            return "headset";
        }
        
        // "TV" -> "tv"
        if (lower.equals("tv") || lower.equals("television") || lower.equals("televisions")) {
            return "tv";
        }
        
        return lower;
    }
    
    /**
     * Converts a string to camelCase
     * Example: "Gaming Mouse" -> "GamingMouse"
     */
    private String toCamelCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] words = text.split("\\s+");
        StringBuilder camelCase = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() > 0) {
                if (i == 0) {
                    camelCase.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
                } else {
                    camelCase.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
                }
            }
        }
        return camelCase.toString();
    }
}