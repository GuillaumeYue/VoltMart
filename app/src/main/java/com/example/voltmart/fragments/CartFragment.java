package com.example.voltmart.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voltmart.R;
import com.example.voltmart.activities.CheckoutActivity;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.adapters.CartAdapter;
import com.example.voltmart.model.CartItemModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class CartFragment extends Fragment implements CartAdapter.CartAdapterListener {
    TextView cartPriceTextView;
    RecyclerView cartRecyclerView;
    Button continueBtn;
    ImageView backBtn, emptyCartImageView;
    CartAdapter cartAdapter;
    int totalPrice = 0;

    ShimmerFrameLayout shimmerFrameLayout;
    LinearLayout mainLinearLayout;

    public CartFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        cartPriceTextView = view.findViewById(R.id.cartPriceTextView);
        cartRecyclerView = view.findViewById(R.id.cartRecyclerView);
        continueBtn = view.findViewById(R.id.continueBtn);
        backBtn = view.findViewById(R.id.backBtn);
        emptyCartImageView = view.findViewById(R.id.emptyCartImageView);
        shimmerFrameLayout = view.findViewById(R.id.shimmerLayout);
        mainLinearLayout = view.findViewById(R.id.mainLinearLayout);

        MainActivity activity = (MainActivity) getActivity();
        activity.hideSearchBar();
        shimmerFrameLayout.startShimmer();
        emptyCartImageView.setVisibility(View.INVISIBLE);

        getCartProducts();

        for (int i = 0; i < cartRecyclerView.getItemDecorationCount(); i++) {
            if (cartRecyclerView.getItemDecorationAt(i) instanceof DividerItemDecoration)
                cartRecyclerView.removeItemDecorationAt(i);
        }

        continueBtn.setOnClickListener(v -> {
            if (totalPrice == 0) {
                Toast.makeText(activity, "Your cart is empty! Add some product in your cart to proceed.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), CheckoutActivity.class);
            intent.putExtra("price", totalPrice);
            startActivity(intent);
        });

        backBtn.setOnClickListener(v -> {
            activity.onBackPressed();
        });

        return view;
    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            totalPrice = intent.getIntExtra("totalPrice", 1000);
//            Log.i("Price", totalPrice+"");
            cartPriceTextView.setText("$ " + totalPrice);
        }
    };

    private void getCartProducts() {
        Query query = FirebaseUtil.getCartItems().orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<CartItemModel> options = new FirestoreRecyclerOptions.Builder<CartItemModel>()
                .setQuery(query, CartItemModel.class)
                .build();

        emptyCartImageView.setVisibility(View.INVISIBLE);
        cartAdapter = new CartAdapter(options, getActivity());
        cartAdapter.setCartAdapterListener(this);
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        cartRecyclerView.setLayoutManager(manager);
        cartRecyclerView.setAdapter(cartAdapter);
        cartAdapter.startListening();
    }

    @Override
    public void onCartEmpty() {
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
        if (mainLinearLayout != null) {
            mainLinearLayout.setVisibility(View.VISIBLE);
        }
        if (emptyCartImageView != null) {
            emptyCartImageView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCartHasItems() {
        if (emptyCartImageView != null) {
            emptyCartImageView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onItemsLoaded() {
        // Stop shimmer and show main layout when items finish loading
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
        if (mainLinearLayout != null) {
            mainLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("price"));
        }
        // Restart adapter listening when fragment resumes
        // This is important when returning from checkout after items are deleted
        if (cartAdapter != null) {
            cartAdapter.startListening();
        } else {
            // If adapter is null (e.g., after checkout), recreate it
            getCartProducts();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        }
        if (cartAdapter != null) {
            cartAdapter.stopListening();
        }
    }
}