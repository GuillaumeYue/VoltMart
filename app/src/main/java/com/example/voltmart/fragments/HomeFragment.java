package com.example.voltmart.fragments;

import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    RecyclerView categoryRecyclerView;
    RecyclerView productRecyclerView;
    MaterialSearchBar searchBar;
    ImageCarousel carousel;
    ShimmerFrameLayout shimmerFrameLayout;
    LinearLayout mainLinearLayout;

    CategoryAdapter categoryAdapter;
    ProductAdapter productAdapter;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        searchBar = getActivity().findViewById(R.id.searchBar);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        carousel = view.findViewById(R.id.carousel);
        shimmerFrameLayout = view.findViewById(R.id.shimmerLayout);
        mainLinearLayout = view.findViewById(R.id.mainLinearLayout);

        MainActivity activity = (MainActivity) getActivity();
        activity.showSearchBar();
        shimmerFrameLayout.startShimmer();

        initCarousel();
        initCategories();
        initProducts();

        return view;
    }

    private void initCarousel() {
        FirebaseUtil.getBanner().orderBy("bannerId").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String status = document.getString("status");
                        if (status == null || status.equals("Live")) {
                            carousel.addData(new CarouselItem(document.get("bannerImage").toString()));
                        }
                    }
                }
            }
        });
    }

    private void initCategories() {
        FirebaseUtil.getCategories().get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int enabledCount = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String status = document.getString("status");
                                if (status == null || status.isEmpty() || status.equals("Enabled")) {
                                    enabledCount++;
                                }
                            }
                            Log.d("HomeFragment", "Enabled categories count = " + enabledCount);
                            
                            if (enabledCount == 0) {
                                Log.w("HomeFragment", "No enabled categories found in database");
                            } else {
                                Log.d("HomeFragment", "Enabled categories found, should be displaying");
                                if (mainLinearLayout != null && mainLinearLayout.getVisibility() != View.VISIBLE) {
                                    shimmerFrameLayout.stopShimmer();
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    mainLinearLayout.setVisibility(View.VISIBLE);
                                    Log.d("HomeFragment", "Made mainLinearLayout visible after categories loaded");
                                    
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
        
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        categoryRecyclerView.setLayoutManager(layoutManager);
        categoryRecyclerView.setHasFixedSize(false);
        categoryRecyclerView.setNestedScrollingEnabled(false);
        
        categoryRecyclerView.setVisibility(View.VISIBLE);
        categoryRecyclerView.setAdapter(categoryAdapter);
        
        Log.d("HomeFragment", "Category adapter created, starting to listen");
        Log.d("HomeFragment", "RecyclerView visibility: " + (categoryRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
        Log.d("HomeFragment", "RecyclerView adapter: " + (categoryRecyclerView.getAdapter() != null ? "SET" : "NULL"));
        
        categoryAdapter.startListening();
        
        categoryRecyclerView.postDelayed(() -> {
            if (categoryAdapter != null && categoryAdapter.getItemCount() > 0) {
                Log.d("HomeFragment", "Category adapter itemCount: " + categoryAdapter.getItemCount());
                Log.d("HomeFragment", "RecyclerView measured width: " + categoryRecyclerView.getMeasuredWidth() + ", height: " + categoryRecyclerView.getMeasuredHeight());
                
                categoryRecyclerView.measure(
                    View.MeasureSpec.makeMeasureSpec(categoryRecyclerView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                categoryRecyclerView.requestLayout();
                
                categoryRecyclerView.post(() -> {
                    int childCount = categoryRecyclerView.getChildCount();
                    Log.d("HomeFragment", "RecyclerView child count after layout: " + childCount);
                    if (childCount == 0) {
                        Log.e("HomeFragment", "CRITICAL: RecyclerView has NO children despite " + categoryAdapter.getItemCount() + " items!");
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

                        shimmerFrameLayout.stopShimmer();
                        shimmerFrameLayout.setVisibility(View.GONE);
                        mainLinearLayout.setVisibility(View.VISIBLE);
                        
                        if (task.getResult() != null) {
                            List<ProductModel> products = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ProductModel product = document.toObject(ProductModel.class);
                                products.add(product);
                            }
                            SearchFragment.updateProductCache(products);
                            Log.d("HomeFragment", "Updated SearchFragment cache with " + products.size() + " products");
                        }
                    }

                });

        Query query = FirebaseUtil.getProducts();
        FirestoreRecyclerOptions<ProductModel> options = new FirestoreRecyclerOptions.Builder<ProductModel>()
                .setQuery(query, ProductModel.class)
                .build();

        productAdapter = new ProductAdapter(options, getContext());
        productRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productRecyclerView.setAdapter(productAdapter);
        productAdapter.startListening();
    }

}
