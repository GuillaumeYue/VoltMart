package com.example.voltmart.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.fragments.HomeFragment;
import com.example.voltmart.model.CartItemModel;
import com.example.voltmart.model.ProductModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索Fragment - 最简单直接的实现
 */
public class SearchFragment extends Fragment {

    private RecyclerView productRecyclerView;
    private SearchProductAdapter searchAdapter;
    private MaterialSearchBar searchBar;
    private EditText searchEditText;
    
    private static List<ProductModel> cachedProducts = new ArrayList<>();
    private static boolean isLoadingProducts = false;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private TextWatcher textWatcher;
    private androidx.activity.OnBackPressedCallback backPressedCallback;

    public static void updateProductCache(List<ProductModel> products) {
        if (products != null) {
            cachedProducts.clear();
            cachedProducts.addAll(products);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.showSearchBar();
        }

        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        
        searchAdapter = new SearchProductAdapter(new ArrayList<>(), getActivity());
        productRecyclerView.setAdapter(searchAdapter);

        searchBar = getActivity().findViewById(R.id.searchBar);
        
        if (searchBar != null) {
            setupSearchBar();
        }

        if (cachedProducts.isEmpty() && !isLoadingProducts) {
            loadAllProducts();
        }

        return view;
    }

    private void setupSearchBar() {
        if (searchBar == null || getActivity() == null) {
            return;
        }

        // 不覆盖MainActivity的监听器，只设置TextWatcher来监听文本变化
        // MainActivity负责导航，SearchFragment只负责搜索逻辑
        
        // 设置TextWatcher - 使用多个延迟确保UI准备好
        handler.postDelayed(() -> {
            if (searchBar != null && getActivity() != null && isAdded()) {
                searchEditText = findEditText(searchBar);
                if (searchEditText != null) {
                    attachTextWatcher();
                } else {
                    // 再试一次
                    handler.postDelayed(() -> {
                        if (searchBar != null && getActivity() != null && isAdded()) {
                            searchEditText = findEditText(searchBar);
                            if (searchEditText != null) {
                                attachTextWatcher();
                            }
                        }
                    }, 200);
                }
            }
        }, 100);
        
        // 如果搜索栏已经有文本，立即执行搜索
        handler.postDelayed(() -> {
            if (searchBar != null && getActivity() != null && isAdded()) {
                String currentText = searchBar.getText() != null ? searchBar.getText().toString().trim() : "";
                if (!currentText.isEmpty()) {
                    performSearch(currentText);
                }
            }
        }, 300);
    }

    private void attachTextWatcher() {
        if (searchEditText == null || getActivity() == null || !isAdded()) {
            return;
        }
        
        // 移除旧的监听器
        if (textWatcher != null) {
            try {
                searchEditText.removeTextChangedListener(textWatcher);
            } catch (Exception e) {
                // 忽略
            }
        }
        
        // 创建新的监听器
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 取消之前的搜索
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                
                // 执行搜索
                String searchText = s != null ? s.toString().trim() : "";
                searchRunnable = () -> {
                    if (getActivity() != null && isAdded()) {
                        performSearch(searchText);
                    }
                };
                handler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        
        // 添加监听器
        try {
            searchEditText.addTextChangedListener(textWatcher);
        } catch (Exception e) {
            // 忽略
        }
    }

    private EditText findEditText(View view) {
        if (view instanceof EditText) {
            return (EditText) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findEditText(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void performSearch(String searchTerm) {
        if (getActivity() == null || !isAdded()) {
            return;
        }
        
        if (productRecyclerView == null) {
            View view = getView();
            if (view != null) {
                productRecyclerView = view.findViewById(R.id.productRecyclerView);
                if (productRecyclerView != null && productRecyclerView.getLayoutManager() == null) {
                    productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                }
            }
        }
        
        if (productRecyclerView == null) {
            return;
        }

        String trimmedTerm = (searchTerm == null) ? "" : searchTerm.trim();

        if (trimmedTerm.isEmpty()) {
            updateResults(new ArrayList<>());
            return;
        }

        if (cachedProducts.isEmpty()) {
            if (!isLoadingProducts) {
                loadAllProducts();
            }
            updateResults(new ArrayList<>());
            return;
        }

        // 执行搜索过滤
        String lowerSearchTerm = trimmedTerm.toLowerCase();
        List<ProductModel> filteredProducts = new ArrayList<>();

        for (ProductModel product : cachedProducts) {
            if (matchesSearch(product, lowerSearchTerm)) {
                filteredProducts.add(product);
            }
        }

        updateResults(filteredProducts);
    }

    private boolean matchesSearch(ProductModel product, String searchTerm) {
        if (product == null || searchTerm == null || searchTerm.isEmpty()) {
            return false;
        }

        String name = product.getName();
        if (name != null && name.toLowerCase().contains(searchTerm)) {
            return true;
        }

        List<String> searchKeys = product.getSearchKey();
        if (searchKeys != null) {
            for (String key : searchKeys) {
                if (key != null && key.toLowerCase().contains(searchTerm)) {
                    return true;
                }
            }
        }

        String category = product.getCategory();
        if (category != null && category.toLowerCase().contains(searchTerm)) {
            return true;
        }

        return false;
    }

    private void updateResults(List<ProductModel> products) {
        if (getActivity() == null || !isAdded()) {
            return;
        }
        
        if (productRecyclerView == null) {
            View view = getView();
            if (view != null) {
                productRecyclerView = view.findViewById(R.id.productRecyclerView);
            }
        }
        
        if (productRecyclerView == null) {
            return;
        }

        if (productRecyclerView.getLayoutManager() == null) {
            productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }

        // 直接更新适配器
        if (searchAdapter == null) {
            searchAdapter = new SearchProductAdapter(products, getActivity());
            productRecyclerView.setAdapter(searchAdapter);
        } else {
            searchAdapter.updateProducts(products);
        }
    }

    private void loadAllProducts() {
        if (isLoadingProducts) {
            return;
        }
        
        isLoadingProducts = true;
        
        FirebaseUtil.getProducts().get()
                .addOnCompleteListener(task -> {
                    isLoadingProducts = false;
                    if (task.isSuccessful() && task.getResult() != null) {
                        cachedProducts.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ProductModel product = document.toObject(ProductModel.class);
                            cachedProducts.add(product);
                        }
                        
                        // 如果搜索栏有文本，自动执行搜索
                        if (getActivity() != null && isAdded() && searchBar != null) {
                            String searchText = searchBar.getText() != null ? searchBar.getText().toString().trim() : "";
                            if (!searchText.isEmpty()) {
                                performSearch(searchText);
                            }
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // 确保RecyclerView和适配器存在
        if (productRecyclerView == null && getView() != null) {
            productRecyclerView = getView().findViewById(R.id.productRecyclerView);
            if (productRecyclerView != null && productRecyclerView.getLayoutManager() == null) {
                productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            }
        }
        
        if (searchAdapter == null && productRecyclerView != null) {
            searchAdapter = new SearchProductAdapter(new ArrayList<>(), getActivity());
            productRecyclerView.setAdapter(searchAdapter);
        }
        
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.showSearchBar();
            
            searchBar = activity.findViewById(R.id.searchBar);
            if (searchBar != null) {
                // 重新设置TextWatcher（每次onResume都重新设置）
                setupSearchBar();
            }
        }

        if (backPressedCallback == null && getActivity() != null) {
            backPressedCallback = new androidx.activity.OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // 只关闭搜索栏，让MainActivity的监听器处理导航
                    if (searchBar != null) {
                        searchBar.closeSearch();
                    }
                }
            };
            getActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (backPressedCallback != null) {
            backPressedCallback.remove();
            backPressedCallback = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        if (backPressedCallback != null) {
            backPressedCallback.remove();
            backPressedCallback = null;
        }
        
        if (searchRunnable != null) {
            handler.removeCallbacks(searchRunnable);
            searchRunnable = null;
        }
        
        if (searchEditText != null && textWatcher != null) {
            try {
                searchEditText.removeTextChangedListener(textWatcher);
            } catch (Exception e) {
                // 忽略
            }
            textWatcher = null;
        }
        
        // 清理所有引用
        searchEditText = null;
        searchBar = null;
        productRecyclerView = null;
        searchAdapter = null;
    }

    private void navigateBackToHome() {
        // 这个方法现在不再使用，导航由MainActivity的监听器处理
        // 保留方法以防其他地方调用
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return;
        }

        // 只关闭搜索栏，让MainActivity的监听器处理导航
        if (searchBar != null) {
            searchBar.closeSearch();
            searchBar.setText("");
        }
    }

    /**
     * 搜索适配器
     */
    private static class SearchProductAdapter extends RecyclerView.Adapter<SearchProductAdapter.ViewHolder> {
        private List<ProductModel> products;
        private android.app.Activity activity;

        public SearchProductAdapter(List<ProductModel> products, android.app.Activity activity) {
            this.products = products != null ? products : new ArrayList<>();
            this.activity = activity;
        }

        public void updateProducts(List<ProductModel> newProducts) {
            this.products.clear();
            if (newProducts != null) {
                this.products.addAll(newProducts);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_search_adapter, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position < 0 || position >= products.size()) {
                return;
            }
            
            ProductModel product = products.get(position);

            holder.productNameTextView.setText(product.getName());
            Picasso.get().load(product.getImage()).into(holder.productImageView);
            holder.productPriceTextView.setText("$ " + product.getPrice());
            holder.originalPrice.setText("$ " + product.getOriginalPrice());
            holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            
            int discountPerc = (product.getDiscount() * 100) / product.getOriginalPrice();
            holder.discountPercentage.setText(discountPerc + "% OFF");

            DecimalFormat df = new DecimalFormat("#.#");
            float rating = Float.parseFloat(df.format(product.getRating()));
            holder.ratingBar.setRating(rating);
            holder.ratingTextView.setText(rating + "");
            holder.noOfRatingTextView.setText("(" + product.getNoOfRating() + ")");

            holder.productLinearLayout.setOnClickListener(v -> {
                Fragment fragment = ProductFragment.newInstance(product);
                if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
                    ((androidx.appcompat.app.AppCompatActivity) activity).getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_frame_layout, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            holder.addToCartBtn.setOnClickListener(v -> addToCart(product));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        private void addToCart(ProductModel product) {
            FirebaseUtil.getProducts().whereEqualTo("productId", product.getProductId())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                            int stock = (int) (long) document.getData().get("stock");

                            FirebaseUtil.getCartItems().whereEqualTo("productId", product.getProductId())
                                    .get()
                                    .addOnCompleteListener(cartTask -> {
                                        if (cartTask.isSuccessful()) {
                                            boolean documentExists = false;
                                            for (QueryDocumentSnapshot cartDoc : cartTask.getResult()) {
                                                documentExists = true;
                                                String docId = cartDoc.getId();
                                                int quantity = (int) (long) cartDoc.getData().get("quantity");
                                                if (quantity < stock) {
                                                    FirebaseUtil.getCartItems().document(docId).update("quantity", quantity + 1)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Toast.makeText(activity, "Added to Cart", Toast.LENGTH_SHORT).show();
                                                                if (activity instanceof MainActivity) {
                                                                    ((MainActivity) activity).addOrRemoveBadge();
                                                                }
                                                            });
                                                } else {
                                                    Toast.makeText(activity, "Max stock available: " + stock, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            if (!documentExists) {
                                                CartItemModel cartItem = new CartItemModel(
                                                        product.getProductId(),
                                                        product.getName(),
                                                        product.getImage(),
                                                        1,
                                                        product.getPrice(),
                                                        product.getOriginalPrice(),
                                                        Timestamp.now()
                                                );
                                                FirebaseUtil.getCartItems().add(cartItem)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(activity, "Added to Cart", Toast.LENGTH_SHORT).show();
                                                            if (activity instanceof MainActivity) {
                                                                ((MainActivity) activity).addOrRemoveBadge();
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    });
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView productNameTextView, productPriceTextView, originalPrice, discountPercentage;
            ImageView productImageView;
            LinearLayout productLinearLayout;
            Button addToCartBtn;
            RatingBar ratingBar;
            TextView ratingTextView, noOfRatingTextView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                productImageView = itemView.findViewById(R.id.productImage);
                productNameTextView = itemView.findViewById(R.id.productName);
                productPriceTextView = itemView.findViewById(R.id.productPrice);
                originalPrice = itemView.findViewById(R.id.originalPrice);
                discountPercentage = itemView.findViewById(R.id.discountPercentage);
                productLinearLayout = itemView.findViewById(R.id.productLinearLayout);
                addToCartBtn = itemView.findViewById(R.id.addToCartBtn);
                ratingBar = itemView.findViewById(R.id.ratingBar);
                ratingTextView = itemView.findViewById(R.id.ratingTextView);
                noOfRatingTextView = itemView.findViewById(R.id.noOfRatingTextView);
            }
        }
    }
}
