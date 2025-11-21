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
 * 搜索Fragment
 * 提供商品搜索功能
 * 功能包括：
 * - 实时搜索（用户输入时自动搜索）
 * - 灵活的关键词匹配（不需要精确匹配）
 * - 从内存中过滤商品（提高性能）
 * - 支持搜索商品名称、关键词、分类、描述等
 */
public class SearchFragment extends Fragment {

    // UI组件
    private RecyclerView productRecyclerView;  // 商品列表RecyclerView
    private SearchProductAdapter searchAdapter; // 搜索适配器
    private MaterialSearchBar searchBar;        // 搜索栏

    // 数据
    private List<ProductModel> allProducts = new ArrayList<>(); // 所有商品列表（从Firestore加载一次）

    // 状态管理
    private androidx.activity.OnBackPressedCallback backPressedCallback; // 返回按钮回调
    private String pendingSearchTerm = null;    // 待执行的搜索词（如果商品未加载完成时存储）
    private Handler searchHandler = new Handler(Looper.getMainLooper()); // 搜索处理Handler
    private Runnable searchRunnable;             // 搜索任务（用于防抖）

    /**
     * 创建Fragment视图
     * 初始化UI组件、设置搜索监听器、加载商品数据
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.showSearchBar(); // 显示搜索栏
        }

        // 初始化RecyclerView
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        searchBar = getActivity().findViewById(R.id.searchBar);

        if (searchBar != null) {
            // SearchFragment只处理搜索执行，不覆盖MainActivity的导航逻辑
            // 设置实时搜索（用户输入时自动搜索）
            setupRealTimeSearch();
        }

        // Fragment创建时加载所有商品（只加载一次）
        loadAllProducts();

        return view;
    }

    /**
     * 从Firestore加载所有商品
     * 只加载一次，后续搜索在内存中进行
     */
    private void loadAllProducts() {
        FirebaseUtil.getProducts().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && getActivity() != null) {
                        allProducts.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ProductModel product = document.toObject(ProductModel.class);
                            allProducts.add(product);
                        }
                        android.util.Log.d("SearchFragment", "Loaded " + allProducts.size() + " products");
                        
                        // If there's a pending search, perform it now
                        if (pendingSearchTerm != null && !pendingSearchTerm.isEmpty()) {
                            String searchToPerform = pendingSearchTerm;
                            pendingSearchTerm = null;
                            performSearch(searchToPerform);
                        } else {
                            // Don't show all products initially - wait for user to search
                            updateAdapter(new ArrayList<>());
                        }
                    }
                });
    }

    /**
     * Perform search with flexible keyword matching
     */
    public void performSearch(String searchTerm) {
        if (productRecyclerView == null || getActivity() == null) {
            return;
        }

        android.util.Log.d("SearchFragment", "Searching for: " + searchTerm + ", products loaded: " + allProducts.size());

        // If products haven't loaded yet, load them first and store the search term
        if (allProducts.isEmpty()) {
            android.util.Log.d("SearchFragment", "Products not loaded yet, loading now...");
            pendingSearchTerm = searchTerm;
            loadAllProducts();
            return;
        }

        if (searchTerm == null || searchTerm.isEmpty()) {
            // Show empty list if search is empty (don't show all products)
            updateAdapter(new ArrayList<>());
            return;
        }

        // Filter products with flexible matching
        List<ProductModel> filteredProducts = new ArrayList<>();
        String lowerSearchTerm = searchTerm.toLowerCase().trim();

        for (ProductModel product : allProducts) {
            if (matchesSearch(product, lowerSearchTerm)) {
                filteredProducts.add(product);
            }
        }

        android.util.Log.d("SearchFragment", "Found " + filteredProducts.size() + " matching products out of " + allProducts.size());
        updateAdapter(filteredProducts);
    }

    /**
     * Check if product matches search term (very flexible matching)
     */
    private boolean matchesSearch(ProductModel product, String searchTerm) {
        if (product == null || searchTerm == null || searchTerm.isEmpty()) {
            return true;
        }

        String lowerSearchTerm = searchTerm.toLowerCase().trim();
        String normalizedSearch = lowerSearchTerm.replaceAll("\\s+", "");

        // Check product name (most important)
        String name = product.getName();
        if (name != null && !name.isEmpty()) {
            String lowerName = name.toLowerCase().trim();
            String normalizedName = lowerName.replaceAll("\\s+", "");
            
            // Exact match (after normalization)
            if (lowerName.equals(lowerSearchTerm) || normalizedName.equals(normalizedSearch)) {
                return true;
            }
            
            // Direct contains check (most common case) - handles "logitech" matching "Logitech G502"
            if (lowerName.contains(lowerSearchTerm)) {
                return true;
            }
            
            // Normalized contains check (for "iphone" matching "iPhone 17 Pro Max")
            if (normalizedName.contains(normalizedSearch)) {
                return true;
            }
            
            // Check if search term is contained in name (handles multi-word searches)
            // e.g., "logitech g502" should match "Logitech G502"
            if (lowerSearchTerm.contains(lowerName) || lowerName.contains(lowerSearchTerm)) {
                return true;
            }
            
            // Word-by-word matching for partial matches
            String[] nameWords = lowerName.split("\\s+");
            String[] searchWords = lowerSearchTerm.split("\\s+");
            
            // If search has multiple words, check if all search words appear in name
            if (searchWords.length > 1) {
                boolean allWordsFound = true;
                for (String searchWord : searchWords) {
                    if (searchWord.length() > 0) {
                        boolean wordFound = false;
                        for (String nameWord : nameWords) {
                            if (nameWord.contains(searchWord) || searchWord.contains(nameWord)) {
                                wordFound = true;
                                break;
                            }
                        }
                        if (!wordFound) {
                            allWordsFound = false;
                            break;
                        }
                    }
                }
                if (allWordsFound) {
                    return true;
                }
            }
            
            // Check individual words for single-word or partial searches
            for (String word : nameWords) {
                if (word.length() > 0) {
                    // Check if word starts with search term or search term starts with word
                    if (word.startsWith(lowerSearchTerm) || lowerSearchTerm.startsWith(word)) {
                        return true;
                    }
                    // Check if word contains search term (for partial matches)
                    if (word.contains(lowerSearchTerm) || lowerSearchTerm.contains(word)) {
                        return true;
                    }
                }
            }
        }

        // Check searchKey array
        List<String> searchKeys = product.getSearchKey();
        if (searchKeys != null) {
            for (String key : searchKeys) {
                if (key != null && !key.isEmpty()) {
                    String lowerKey = key.toLowerCase().trim();
                    String normalizedKey = lowerKey.replaceAll("\\s+", "");
                    if (lowerKey.equals(lowerSearchTerm) || normalizedKey.equals(normalizedSearch)) {
                        return true;
                    }
                    if (lowerKey.contains(lowerSearchTerm) || normalizedKey.contains(normalizedSearch)) {
                        return true;
                    }
                }
            }
        }

        // Check category
        String category = product.getCategory();
        if (category != null && !category.isEmpty()) {
            String lowerCategory = category.toLowerCase().trim();
            String normalizedCategory = lowerCategory.replaceAll("\\s+", "");
            if (lowerCategory.equals(lowerSearchTerm) || normalizedCategory.equals(normalizedSearch)) {
                return true;
            }
            if (lowerCategory.contains(lowerSearchTerm) || normalizedCategory.contains(normalizedSearch)) {
                return true;
            }
        }

        // Check description
        String description = product.getDescription();
        if (description != null && !description.isEmpty()) {
            String lowerDesc = description.toLowerCase().trim();
            if (lowerDesc.contains(lowerSearchTerm)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Update RecyclerView adapter with filtered products
     */
    private void updateAdapter(List<ProductModel> products) {
        if (productRecyclerView == null || getActivity() == null) {
            return;
        }

        // Ensure layout manager is set
        if (productRecyclerView.getLayoutManager() == null) {
            productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }

        searchAdapter = new SearchProductAdapter(products, getActivity());
        productRecyclerView.setAdapter(searchAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Re-initialize searchBar if needed
        if (searchBar == null && getActivity() instanceof MainActivity) {
            searchBar = getActivity().findViewById(R.id.searchBar);
            // Setup real-time search if searchBar was just initialized
            if (searchBar != null) {
                setupRealTimeSearch();
            }
        }

        // Enable back button
        if (backPressedCallback == null && getActivity() != null) {
            backPressedCallback = new androidx.activity.OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.navigateBackToHome();
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
        // Cancel any pending search
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
            searchRunnable = null;
        }
    }

    /**
     * Setup real-time search as user types
     */
    private void setupRealTimeSearch() {
        if (searchBar == null) {
            return;
        }
        
        // Get the EditText from MaterialSearchBar using reflection or findViewWithTag
        // MaterialSearchBar typically has an EditText as a child
        View searchEditText = searchBar.findViewById(android.R.id.text1);
        if (searchEditText == null) {
            // Try to find EditText in the search bar
            searchEditText = findEditTextInView(searchBar);
        }
        
        if (searchEditText instanceof EditText) {
            EditText editText = (EditText) searchEditText;
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Cancel previous search
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    
                    // Perform search after a short delay (debounce)
                    String searchText = s != null ? s.toString().trim() : "";
                    searchRunnable = () -> performSearch(searchText);
                    searchHandler.postDelayed(searchRunnable, 300); // 300ms delay for better performance
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } else {
            // Fallback: Use onSearchConfirmed only
            android.util.Log.w("SearchFragment", "Could not find EditText in MaterialSearchBar for real-time search");
        }
    }
    
    /**
     * Recursively find EditText in a view hierarchy
     */
    private EditText findEditTextInView(View view) {
        if (view instanceof EditText) {
            return (EditText) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findEditTextInView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }


    /**
     * Simple RecyclerView adapter for search results
     */
    private static class SearchProductAdapter extends RecyclerView.Adapter<SearchProductAdapter.ViewHolder> {
        private List<ProductModel> products;
        private android.app.Activity activity;

        public SearchProductAdapter(List<ProductModel> products, android.app.Activity activity) {
            this.products = products;
            this.activity = activity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_search_adapter, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
