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

    private void getProducts(String categoryName){
        if (getActivity() == null) {
            return;
        }
        
        // Normalize category name to match product category values
        // Products use lowercase, no spaces (e.g., "phones")
        // Categories use formatted names (e.g., "Smart Phone")
        String normalizedCategory = normalizeCategoryName(categoryName);
        
        android.util.Log.d("CategoryFragment", "Searching products for category: " + categoryName + " (normalized: " + normalizedCategory + ")");
        
        // Try exact match first
        Query query = FirebaseUtil.getProducts().whereEqualTo("category", normalizedCategory);
        FirestoreRecyclerOptions<ProductModel> options = new FirestoreRecyclerOptions.Builder<ProductModel>()
                .setQuery(query, ProductModel.class)
                .build();

        searchProductAdapter = new SearchAdapter(options, getActivity());
        productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        productRecyclerView.setAdapter(searchProductAdapter);
        searchProductAdapter.startListening();
        
        // Log if no products found
        searchProductAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                android.util.Log.d("CategoryFragment", "Products loaded: " + itemCount + " items");
            }
            
            @Override
            public void onChanged() {
                super.onChanged();
                android.util.Log.d("CategoryFragment", "Adapter changed, itemCount: " + searchProductAdapter.getItemCount());
                if (searchProductAdapter.getItemCount() == 0) {
                    android.util.Log.w("CategoryFragment", "No products found for category: " + categoryName + " (normalized: " + normalizedCategory + ")");
                }
            }
        });
    }
    
    /**
     * Normalizes category name to match product category values in Firebase
     * Examples:
     * "Smart Phone" -> "phones"
     * "Laptop" -> "laptop"
     * "phones" -> "phones" (already normalized)
     */
    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return "";
        }
        
        // Convert to lowercase and remove spaces
        String normalized = categoryName.toLowerCase().trim().replaceAll("\\s+", "");
        
        // Handle common mappings
        // "Smart Phone" -> "phones"
        if (normalized.contains("smart") && normalized.contains("phone")) {
            return "phones";
        }
        if (normalized.equals("phone") || normalized.equals("phones") || normalized.equals("smartphone")) {
            return "phones";
        }
        
        // "Laptop" -> "laptop"
        if (normalized.equals("laptop") || normalized.equals("laptops")) {
            return "laptop";
        }
        
        // Return normalized version (lowercase, no spaces)
        return normalized;
    }
}