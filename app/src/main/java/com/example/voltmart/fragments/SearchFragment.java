package com.example.voltmart.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.adapters.SearchAdapter;
import com.example.voltmart.fragments.HomeFragment;
import com.example.voltmart.model.ProductModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;

public class SearchFragment extends Fragment {

    RecyclerView productRecyclerView;
    SearchAdapter searchProductAdapter;
    String searchTerm;

    MaterialSearchBar searchBar;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.showSearchBar();
        }

        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        searchBar = getActivity().findViewById(R.id.searchBar);
        
        // Initialize RecyclerView with empty adapter first to prevent "No adapter attached" warnings
        if (productRecyclerView != null) {
            productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }

        if (searchBar != null) {
            searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
                @Override
                public void onSearchStateChanged(boolean enabled) {
                    super.onSearchStateChanged(enabled);
                    android.util.Log.d("SearchFragment", "onSearchStateChanged: enabled=" + enabled);
                    // When search is closed (disabled), navigate back to home
                    if (!enabled && activity != null) {
                        android.util.Log.d("SearchFragment", "Search closed, navigating back to home");
                        navigateBackToHome();
                    }
                }

                @Override
                public void onSearchConfirmed(CharSequence text) {
                    android.util.Log.d("SearchFragment", "onSearchConfirmed: " + text);
                    // Use the text parameter directly, or fallback to searchBar text
                    String searchText = "";
                    if (text != null && text.length() > 0) {
                        searchText = text.toString().toLowerCase().trim();
                    } else if (searchBar != null && searchBar.getText() != null) {
                        searchText = searchBar.getText().toString().toLowerCase().trim();
                    }
                    
                    android.util.Log.d("SearchFragment", "Search text extracted: " + searchText);
                    
                    if (!searchText.isEmpty()) {
                        searchTerm = searchText;
                        initProducts();
                    } else {
                        android.util.Log.w("SearchFragment", "Search text is empty, showing all products");
                        // Show all products if search is empty
                        initProducts();
                    }
                    super.onSearchConfirmed(text);
                }

                @Override
                public void onButtonClicked(int buttonCode) {
                    android.util.Log.d("SearchFragment", "onButtonClicked: buttonCode=" + buttonCode);
                    // Handle close/back button click - navigate back to home
                    if (activity != null) {
                        android.util.Log.d("SearchFragment", "Close button clicked, navigating back to home");
                        navigateBackToHome();
                    }
                    super.onButtonClicked(buttonCode);
                }
            });
        }
        
        // Don't initialize products here - wait for search confirmation
        // This prevents showing all products when fragment is first created

        return view;
    }

    void initProducts() {
        if (getActivity() == null || productRecyclerView == null) {
            android.util.Log.w("SearchFragment", "Cannot init products: activity or recyclerview is null");
            return;
        }
        
        // Stop previous adapter if exists
        if (searchProductAdapter != null) {
            searchProductAdapter.stopListening();
            searchProductAdapter = null;
        }
        
        // Get search term - use stored searchTerm if available, otherwise get from searchBar
        if (searchTerm == null || searchTerm.isEmpty()) {
            if (searchBar != null && searchBar.getText() != null) {
                searchTerm = searchBar.getText().toString().toLowerCase().trim();
            } else {
                searchTerm = "";
            }
        }
        
        android.util.Log.d("SearchFragment", "initProducts: searchTerm=" + searchTerm);
        
        Query q;
        if (searchTerm == null || searchTerm.isEmpty()) {
            android.util.Log.d("SearchFragment", "Search term is empty, showing all products");
            // If search term is empty, show all products
            q = FirebaseUtil.getProducts();
        } else {
            // Search for products matching the search term
            android.util.Log.d("SearchFragment", "Searching for: " + searchTerm);
            q = FirebaseUtil.getProducts().whereArrayContains("searchKey", searchTerm);
        }
        
        FirestoreRecyclerOptions<ProductModel> options = new FirestoreRecyclerOptions.Builder<ProductModel>()
                .setQuery(q, ProductModel.class)
                .build();
        
        searchProductAdapter = new SearchAdapter(options, getActivity());
        
        // Ensure layout manager is set
        if (productRecyclerView.getLayoutManager() == null) {
            productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        
        productRecyclerView.setAdapter(searchProductAdapter);
        searchProductAdapter.startListening();
        
        android.util.Log.d("SearchFragment", "Adapter started listening, itemCount: " + searchProductAdapter.getItemCount());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only reinitialize if search bar has text
        if (searchBar != null && searchBar.getText() != null && !searchBar.getText().toString().trim().isEmpty()) {
            initProducts();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Stop listening when fragment is paused
        if (searchProductAdapter != null) {
            searchProductAdapter.stopListening();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop listening and clear adapter when view is destroyed
        if (searchProductAdapter != null) {
            searchProductAdapter.stopListening();
            searchProductAdapter = null;
        }
    }
    
    /**
     * Navigate back to home fragment
     */
    private void navigateBackToHome() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return;
        }
        
        // Stop listening to search adapter before navigating
        if (searchProductAdapter != null) {
            searchProductAdapter.stopListening();
        }
        
        // Clear search bar text
        if (searchBar != null) {
            searchBar.setText("");
        }
        
        // Try to pop back stack first (this will return to previous fragment)
        if (activity.getSupportFragmentManager().getBackStackEntryCount() > 0) {
            android.util.Log.d("SearchFragment", "Popping back stack");
            activity.getSupportFragmentManager().popBackStack();
        } else {
            // If no back stack, navigate to home fragment directly
            android.util.Log.d("SearchFragment", "No back stack, replacing with home fragment");
            HomeFragment homeFragment = new HomeFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_frame_layout, homeFragment, "home")
                    .commit();
        }
    }
}