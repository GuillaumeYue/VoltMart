package com.example.voltmart.fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.model.OrderItemModel;
import com.example.voltmart.model.ProductModel;
import com.example.voltmart.model.ReviewModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.example.voltmart.utils.WindowInsetsHelper;
import android.widget.LinearLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class OrderDetailsFragment extends Fragment {

    TextView productNameTextView;
    TextView orderIdTextView;
    TextView nameTextView;
    TextView emailTextView;
    TextView phoneTextView;
    TextView addressTextView;
    TextView commentTextView;
    ImageView productImageView;
    LinearLayout productLinearLayout;

    RatingBar ratingBar;
    TextInputEditText titleReviewEditText;
    TextInputEditText reviewEditText;
    Button submitBtn;
    ImageView backBtn;

    OrderItemModel orderItem;
    ProductModel productModel;
    ReviewModel oldReviewModel;
    SweetAlertDialog dialog;

    boolean isNew = true;

    public OrderDetailsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_details, container, false);

        productImageView = view.findViewById(R.id.productImage);
        productNameTextView = view.findViewById(R.id.productName);
        orderIdTextView = view.findViewById(R.id.orderIdTextView);
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        phoneTextView = view.findViewById(R.id.phoneTextView);
        addressTextView = view.findViewById(R.id.addressTextView);
        commentTextView = view.findViewById(R.id.commentTextView);
        productLinearLayout = view.findViewById(R.id.productLinearLayout);

        ratingBar = view.findViewById(R.id.ratingBar);
        titleReviewEditText = view.findViewById(R.id.titleReviewEditText);
        reviewEditText = view.findViewById(R.id.reviewEditText);
        backBtn = view.findViewById(R.id.backBtn);
        submitBtn = view.findViewById(R.id.submitBtn);
        
        LinearLayout topLayout = view.findViewById(R.id.linearLayout);
        if (topLayout != null) {
            WindowInsetsHelper.applyTopWindowInsets(topLayout, 4);
        }

        MainActivity activity = (MainActivity) getActivity();
        activity.hideSearchBar();

        backBtn.setOnClickListener(v -> {
            activity.onBackPressed();
        });

        // Get orderId, productId, and documentId from arguments with null safety
        Bundle args = getArguments();
        if (args == null) {
            android.util.Log.e("OrderDetailsFragment", "Arguments are null, cannot load order details");
            Toast.makeText(activity, "Error: Order information not available", Toast.LENGTH_SHORT).show();
            activity.onBackPressed();
            return view;
        }
        
        int oid = args.getInt("orderId", -1);
        int productId = args.getInt("productId", -1);
        String documentId = args.getString("documentId");
        
        if (oid == -1) {
            android.util.Log.e("OrderDetailsFragment", "OrderId not found in arguments");
            Toast.makeText(activity, "Error: Order ID not found", Toast.LENGTH_SHORT).show();
            activity.onBackPressed();
            return view;
        }

        dialog = new SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);

        dialog.show();

        // Use documentId if available (most reliable), otherwise use orderId + productId
        initProduct(oid, productId, documentId, new FirestoreCallback() {
            @Override
            public void onCallback(OrderItemModel orderItem) {
                Picasso.get().load(orderItem.getImage()).into(productImageView);

                orderIdTextView.setText(orderItem.getOrderId()+"");
                productNameTextView.setText(orderItem.getName());
                nameTextView.setText(orderItem.getFullName());
                emailTextView.setText(orderItem.getEmail());
                phoneTextView.setText(orderItem.getPhoneNumber());
                addressTextView.setText(orderItem.getAddress());
                commentTextView.setText(orderItem.getComments()+" ");

                initReview();
                initSubmitBtn(new FirestoreCallback() {
                    @Override
                    public void onCallback(String productDocId, ProductModel productModel) {
                        submitBtn.setOnClickListener(v -> {
                            float rating = ratingBar.getRating();
                            if (rating == 0){
                                Toast.makeText(activity, "Please select the rating", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            ReviewModel review = new ReviewModel(FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), rating, titleReviewEditText.getText().toString(), reviewEditText.getText().toString(), Timestamp.now());
                            FirebaseUtil.getReviews(orderItem.getProductId()).document(FirebaseAuth.getInstance().getUid()).set(review);

                            if (isNew){
                                int newNoOfRating = productModel.getNoOfRating() + 1;
                                float newRating = (productModel.getRating() * productModel.getNoOfRating() + rating) / newNoOfRating;
//                                DecimalFormat df = new DecimalFormat("#.#");
//                                newRating = Float.parseFloat(df.format(newRating));
                                FirebaseUtil.getProducts().document(productDocId).update("rating", newRating);
                                FirebaseUtil.getProducts().document(productDocId).update("noOfRating", newNoOfRating);
                            } else {
                                float newRating = (productModel.getRating() * productModel.getNoOfRating() - oldReviewModel.getRating() + rating) / productModel.getNoOfRating();
                                FirebaseUtil.getProducts().document(productDocId).update("rating", newRating);
                            }

                            Toast.makeText(activity, "Review saved successfully!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onCallback(OrderItemModel orderItem) {

                    }
                });
            }

            @Override
            public void onCallback(String productDocId, ProductModel productModel) {

            }
        });

        productLinearLayout.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(orderItem.getProductId());
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, fragment).addToBackStack(null).commit();
        });

        return view;
    }

    private void initSubmitBtn(FirestoreCallback callback) {
        FirebaseUtil.getProducts().whereEqualTo("productId", orderItem.getProductId())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            for (QueryDocumentSnapshot document : task.getResult()){
                                productModel = document.toObject(ProductModel.class);
                                String productDocId = document.getId();

                                callback.onCallback(productDocId, productModel);
                            }
                        }
                    }
                });
    }

    private void initReview() {
        FirebaseFirestore.getInstance().collection("reviews").document(orderItem.getProductId()+"").collection("review")
                .document(FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){
                            dialog.dismiss();
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                isNew = false;
                                oldReviewModel = document.toObject(ReviewModel.class);
                                ratingBar.setRating(oldReviewModel.getRating());
                                titleReviewEditText.setText(oldReviewModel.getTitle());
                                reviewEditText.setText(oldReviewModel.getReview());
                                submitBtn.setText("Edit review");
                            }
                        }
                    }
                });
    }

    private void initProduct(int orderId, int productId, String documentId, FirestoreCallback callback) {
        // If documentId is available, use it directly (most reliable)
        if (documentId != null && !documentId.isEmpty()) {
            FirebaseUtil.getOrderItems().document(documentId)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                DocumentSnapshot document = task.getResult();
                                orderItem = document.toObject(OrderItemModel.class);
                                if (orderItem != null) {
                                    callback.onCallback(orderItem);
                                } else {
                                    android.util.Log.e("OrderDetailsFragment", "Failed to parse order item from document");
                                    dialog.dismiss();
                                    Toast.makeText(getActivity(), "Error loading order details", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                android.util.Log.e("OrderDetailsFragment", "Document not found: " + documentId);
                                dialog.dismiss();
                                Toast.makeText(getActivity(), "Order not found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else if (productId > 0) {
            // Fallback: query by orderId AND productId to get the specific item
            FirebaseUtil.getOrderItems()
                    .whereEqualTo("orderId", orderId)
                    .whereEqualTo("productId", productId)
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                QuerySnapshot result = task.getResult();
                                if (!result.isEmpty()) {
                                    // Get the first (and should be only) matching document
                                    for (QueryDocumentSnapshot document : result) {
                                        orderItem = document.toObject(OrderItemModel.class);
                                        if (orderItem != null) {
                                            callback.onCallback(orderItem);
                                        } else {
                                            android.util.Log.e("OrderDetailsFragment", "Failed to parse order item");
                                            dialog.dismiss();
                                            Toast.makeText(getActivity(), "Error loading order details", Toast.LENGTH_SHORT).show();
                                        }
                                        break; // Only process the first document
                                    }
                                } else {
                                    android.util.Log.e("OrderDetailsFragment", "No order found with orderId=" + orderId + ", productId=" + productId);
                                    dialog.dismiss();
                                    Toast.makeText(getActivity(), "Order not found", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                android.util.Log.e("OrderDetailsFragment", "Error querying order", task.getException());
                                dialog.dismiss();
                                Toast.makeText(getActivity(), "Error loading order", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            // Last resort: query by orderId only (may return multiple items, use the first one)
            android.util.Log.w("OrderDetailsFragment", "No documentId or productId provided, using orderId only");
            FirebaseUtil.getOrderItems().whereEqualTo("orderId", orderId)
                    .limit(1) // Limit to 1 result to avoid ambiguity
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                QuerySnapshot result = task.getResult();
                                if (!result.isEmpty()) {
                                    // Get the first document
                                    for (QueryDocumentSnapshot document : result) {
                                        orderItem = document.toObject(OrderItemModel.class);
                                        if (orderItem != null) {
                                            callback.onCallback(orderItem);
                                        } else {
                                            android.util.Log.e("OrderDetailsFragment", "Failed to parse order item");
                                            dialog.dismiss();
                                            Toast.makeText(getActivity(), "Error loading order details", Toast.LENGTH_SHORT).show();
                                        }
                                        break; // Only process the first document
                                    }
                                } else {
                                    android.util.Log.e("OrderDetailsFragment", "No order found with orderId=" + orderId);
                                    dialog.dismiss();
                                    Toast.makeText(getActivity(), "Order not found", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                android.util.Log.e("OrderDetailsFragment", "Error querying order", task.getException());
                                dialog.dismiss();
                                Toast.makeText(getActivity(), "Error loading order", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    public interface FirestoreCallback {
        void onCallback(OrderItemModel orderItem);
        void onCallback(String productDocId, ProductModel productModel);
    }
}