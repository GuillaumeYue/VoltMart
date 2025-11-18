package com.example.voltmart.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.activities.SplashActivity;
import com.example.voltmart.adapters.OrderListAdapter;
import com.example.voltmart.model.OrderItemModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.Query;

public class ProfileFragment extends Fragment {
    RecyclerView orderRecyclerView;
    OrderListAdapter orderAdapter;
    LinearLayout logoutBtn;
    TextView userNameTextView;
    ImageView editNameBtn;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_profile, container, false);
        orderRecyclerView = view.findViewById(R.id.orderRecyclerView);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        userNameTextView = view.findViewById(R.id.userNameTextView);
        editNameBtn = view.findViewById(R.id.editNameBtn);

        updateUserNameDisplay();

        // Edit name button click listener
        editNameBtn.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                showEditNameDialog();
            } else {
                Toast.makeText(getActivity(), "Please login to edit your profile", Toast.LENGTH_SHORT).show();
            }
        });
        
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        
        // Only get order products if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            getOrderProducts();
        }

        MainActivity activity = (MainActivity) getActivity();
        activity.hideSearchBar();

        return view;
    }

    private void updateUserNameDisplay() {
        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            
            // Use display name if available, otherwise use email, otherwise use "User"
            String userName = (displayName != null && !displayName.isEmpty()) 
                ? displayName 
                : (email != null ? email.split("@")[0] : "User");
            
            userNameTextView.setText("Hello, " + userName);
            editNameBtn.setVisibility(View.VISIBLE);
        } else {
            userNameTextView.setText("Hello, Guest");
            editNameBtn.setVisibility(View.GONE);
        }
    }

    private void showEditNameDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        // Create dialog with EditText
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Edit Name");

        // Set up the input
        final EditText input = new EditText(getActivity());
        String currentName = user.getDisplayName();
        if (currentName != null && !currentName.isEmpty()) {
            input.setText(currentName);
        }
        input.setHint("Enter your name");
        input.setPadding(50, 20, 50, 20);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(getActivity(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                updateUserName(newName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUserName(String newName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                            updateUserNameDisplay();
                        } else {
                            Toast.makeText(getActivity(), "Failed to update name: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getOrderProducts() {
        // Check if user is authenticated before querying orders
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        
        Query query = FirebaseUtil.getOrderItems().orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<OrderItemModel> options = new FirestoreRecyclerOptions.Builder<OrderItemModel>()
                .setQuery(query, OrderItemModel.class)
                .build();

        orderAdapter = new OrderListAdapter(options, getActivity());
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        orderRecyclerView.setLayoutManager(manager);
        orderRecyclerView.setAdapter(orderAdapter);
        orderAdapter.startListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user name display when fragment resumes
        updateUserNameDisplay();
    }
}