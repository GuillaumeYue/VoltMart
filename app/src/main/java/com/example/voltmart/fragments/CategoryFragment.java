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

public class CategoryFragment extends Fragment {

    RecyclerView productRecyclerView;
    SearchAdapter searchProductAdapter;
    ImageView backBtn;
    TextView labelTextView;

    String categoryName;
    private android.os.Handler categoryCheckHandler;

    public CategoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_category, container, false);
        labelTextView = view.findViewById(R.id.labelTextView);
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        backBtn = view.findViewById(R.id.backBtn);

        // Get category name from arguments with null safety
        Bundle args = getArguments();
        if (args != null) {
            categoryName = args.getString("categoryName", "Electronics");
        } else {
            categoryName = "Electronics";
        }

        labelTextView.setText(categoryName);
        getProducts(categoryName);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.hideSearchBar();

            backBtn.setOnClickListener(v -> {
                if (activity != null) {
                    activity.onBackPressed();
                }
            });
        }
        return view;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up handler and adapter
        if (categoryCheckHandler != null) {
            categoryCheckHandler.removeCallbacksAndMessages(null);
        }
        if (searchProductAdapter != null) {
            searchProductAdapter.stopListening();
        }
    }

    private void getProducts(String categoryName){
        if (getActivity() == null) {
            return;
        }
        
        // Prepare all variations to try
        String normalizedCategory = normalizeCategoryName(categoryName);
        String lowerCategory = categoryName.toLowerCase().trim();
        
        // Build list of category variations to try (remove duplicates)
        java.util.List<String> categoriesToTry = new java.util.ArrayList<>();
        categoriesToTry.add(categoryName); // Original
        if (!categoriesToTry.contains(lowerCategory)) {
            categoriesToTry.add(lowerCategory);
        }
        if (!categoriesToTry.contains(normalizedCategory)) {
            categoriesToTry.add(normalizedCategory);
        }
        
        // Also try with space variations for multi-word categories
        if (categoryName.contains(" ")) {
            // Try with hyphen
            String withHyphen = categoryName.toLowerCase().replace(" ", "-");
            if (!categoriesToTry.contains(withHyphen)) {
                categoriesToTry.add(withHyphen);
            }
            // Try with underscore
            String withUnderscore = categoryName.toLowerCase().replace(" ", "_");
            if (!categoriesToTry.contains(withUnderscore)) {
                categoriesToTry.add(withUnderscore);
            }
        }
        
        android.util.Log.d("CategoryFragment", "Searching products for category: " + categoryName);
        android.util.Log.d("CategoryFragment", "Will try variations: " + categoriesToTry);
        
        // Try all variations sequentially
        tryCategoryVariations(categoriesToTry, 0);
    }
    
    /**
     * Tries category variations sequentially until one works
     */
    private void tryCategoryVariations(java.util.List<String> categoriesToTry, int currentIndex) {
        if (getActivity() == null || currentIndex >= categoriesToTry.size()) {
            android.util.Log.w("CategoryFragment", "No products found after trying all " + categoriesToTry.size() + " variations");
            return;
        }
        
        String categoryToTry = categoriesToTry.get(currentIndex);
        android.util.Log.d("CategoryFragment", "Trying category variation [" + (currentIndex + 1) + "/" + categoriesToTry.size() + "]: " + categoryToTry);
        
        // Stop previous adapter if exists and clean up handler
        if (searchProductAdapter != null) {
            searchProductAdapter.stopListening();
        }
        if (categoryCheckHandler != null) {
            categoryCheckHandler.removeCallbacksAndMessages(null);
        }
        
        // Create query for this category variation
        Query query = FirebaseUtil.getProducts().whereEqualTo("category", categoryToTry);
        FirestoreRecyclerOptions<ProductModel> options = new FirestoreRecyclerOptions.Builder<ProductModel>()
                .setQuery(query, ProductModel.class)
                .build();
        
        searchProductAdapter = new SearchAdapter(options, getActivity());
        productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        productRecyclerView.setAdapter(searchProductAdapter);
        searchProductAdapter.startListening();
        
        // Register observer to check if products were found
        final int index = currentIndex;
        final boolean[] foundProducts = {false};
        // Use instance handler for cleanup
        if (categoryCheckHandler == null) {
            categoryCheckHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        final android.os.Handler handler = categoryCheckHandler;
        
        // Set a timeout to check if products were found after a delay
        Runnable checkTimeout = new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) {
                    return;
                }
                if (!foundProducts[0] && searchProductAdapter != null) {
                    int itemCount = searchProductAdapter.getItemCount();
                    android.util.Log.d("CategoryFragment", "Timeout check: itemCount = " + itemCount + " for category: " + categoryToTry);
                    if (itemCount == 0) {
                        if (index < categoriesToTry.size() - 1) {
                            // No products found, try next variation
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
            }
        };
        
        // Check after 1 second to give the query time to complete (reduced for faster response)
        handler.postDelayed(checkTimeout, 1000);
        
        searchProductAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if (itemCount > 0 && !foundProducts[0]) {
                    foundProducts[0] = true;
                    handler.removeCallbacks(checkTimeout); // Cancel timeout since we found products
                    android.util.Log.d("CategoryFragment", "Products loaded: " + itemCount + " items with category: " + categoryToTry);
                }
            }
            
            @Override
            public void onChanged() {
                super.onChanged();
                int itemCount = searchProductAdapter.getItemCount();
                android.util.Log.d("CategoryFragment", "Adapter changed, itemCount: " + itemCount + " for category: " + categoryToTry);
                
                if (itemCount > 0 && !foundProducts[0]) {
                    foundProducts[0] = true;
                    handler.removeCallbacks(checkTimeout); // Cancel timeout since we found products
                    android.util.Log.d("CategoryFragment", "Successfully found " + itemCount + " products with category: " + categoryToTry);
                }
                // Let the timeout handle trying the next variation to avoid conflicts
            }
        });
    }
    
    /**
     * Normalizes category name to match product category values in Firebase
     * Examples:
     * "Smart Phone" -> "phones"
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
        
        // "Laptop" -> "laptop"
        if (lower.equals("laptop") || lower.equals("laptops")) {
            return "laptop";
        }
        
        // "Gaming PC" -> try "gamingpc" and "gaming pc"
        if (lower.contains("gaming") && lower.contains("pc")) {
            // Try both variations
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
        
        // For other categories, try both with and without spaces
        // First try: lowercase with spaces removed
        String noSpaces = lower.replaceAll("\\s+", "");
        // This will be tried first in the query
        
        // Return normalized version (lowercase, no spaces) as default
        return noSpaces;
    }
}