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
        activity.showSearchBar();

        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        searchBar = getActivity().findViewById(R.id.searchBar);

        searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                super.onSearchStateChanged(enabled);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
//                searchTerm = searchBar.getText().toLowerCase().trim();
                initProducts();
                super.onSearchConfirmed(text);

            }

            @Override
            public void onButtonClicked(int buttonCode) {
                super.onButtonClicked(buttonCode);
            }
        });
        initProducts();

        return view;
    }

    void initProducts() {
        searchTerm = searchBar.getText().toLowerCase().trim();
        Query q = FirebaseUtil.getProducts().whereArrayContains("searchKey", searchTerm);
        FirestoreRecyclerOptions<ProductModel> options = new FirestoreRecyclerOptions.Builder<ProductModel>()
                .setQuery(q, ProductModel.class)
                .build();

        searchProductAdapter = new SearchAdapter(options, getActivity());
        productRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        productRecyclerView.setAdapter(searchProductAdapter);
        searchProductAdapter.startListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        initProducts();
    }
}