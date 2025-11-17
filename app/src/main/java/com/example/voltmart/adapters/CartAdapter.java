package com.example.voltmart.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.model.CartItemModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class CartAdapter extends FirestoreRecyclerAdapter<CartItemModel, CartAdapter.CartViewHolder> {

    private Context context;
    private AppCompatActivity activity;
    //    private ArrayList<ProductModel> products;
    final int[] stock = new int[1];
    int totalPrice = 0;
    boolean gotSum = false;
    int count;

    // Callback interface for fragment communication
    public interface CartAdapterListener {
        void onCartEmpty();
        void onCartHasItems();
        void onItemsLoaded(); // Called when items finish loading (to stop shimmer)
    }

    private CartAdapterListener listener;

    @Override
    protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull CartItemModel model) {
        // Validate position to prevent IndexOutOfBoundsException
        // Check both getItemCount() and getSnapshots().size() for extra safety
        int itemCount = getItemCount();
        int snapshotSize = getSnapshots().size();
        
        if (position < 0 || position >= itemCount || position >= snapshotSize) {
            Log.e("CartAdapter", "Invalid position: " + position + ", itemCount: " + itemCount + ", snapshotSize: " + snapshotSize);
            // Hide the view holder to prevent crashes
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }

        // Ensure the view is visible and has proper layout params
        holder.itemView.setVisibility(View.VISIBLE);
        if (holder.itemView.getLayoutParams() != null && holder.itemView.getLayoutParams().height == 0) {
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // Check if model has valid data, if not, delete the invalid item
        String itemName = model.getName();
        String itemImage = model.getImage();
        int itemPrice = model.getPrice();
        int itemQuantity = model.getQuantity();
        
        if (itemName == null || itemName.trim().isEmpty() || 
            itemPrice < 0 || itemQuantity <= 0) {
            // Invalid cart item, delete it asynchronously to avoid RecyclerView inconsistency
            try {
                if (position >= 0 && position < getSnapshots().size()) {
                    String docId = getSnapshots().getSnapshot(position).getId();
                    Log.w("CartAdapter", "Deleting invalid cart item: " + docId);
                    // Post deletion to avoid modifying data during binding
                    holder.itemView.post(() -> {
                        FirebaseUtil.getCartItems().document(docId).delete()
                                .addOnSuccessListener(aVoid -> Log.i("CartAdapter", "Deleted invalid cart item: " + docId))
                                .addOnFailureListener(e -> Log.e("CartAdapter", "Failed to delete invalid cart item: " + docId, e));
                    });
                }
            } catch (Exception e) {
                Log.e("CartAdapter", "Error deleting invalid cart item", e);
            }
            // Hide this view holder's item
            holder.itemView.setVisibility(View.GONE);
            return;
        }

        if (position == 0 && !gotSum) {
            calculateTotalPrice();
        }

        // Safely bind data with try-catch to prevent crashes during rapid data changes
        try {
            holder.productName.setText(model.getName());
            holder.singleProductPrice.setText("$ " + model.getPrice());
            holder.productPrice.setText("$ " + model.getPrice() * model.getQuantity());
            holder.originalPrice.setText("$ " + model.getOriginalPrice());
            holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.productQuantity.setText(model.getQuantity() + "");
        } catch (Exception e) {
            Log.e("CartAdapter", "Error binding cart item data at position " + position, e);
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        
        Picasso.get().load(model.getImage()).into(holder.productCartImage, new Callback() {
            @Override
            public void onSuccess() {
                // When the last item's image loads, notify the fragment to stop shimmer
                int position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position == getSnapshots().size()-1) {
                    if (listener != null) {
                        listener.onItemsLoaded();
                    }
                }
            }
            @Override
            public void onError(Exception e) {
                // Even on error, if this is the last item, stop shimmer
                int position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position == getSnapshots().size()-1) {
                    if (listener != null) {
                        listener.onItemsLoaded();
                    }
                }
            }
        });

        holder.plusBtn.setOnClickListener(v -> {
            // Disable button temporarily to prevent rapid clicks
            holder.plusBtn.setEnabled(false);
            holder.minusBtn.setEnabled(false);
            // Post to next frame to ensure RecyclerView has finished current layout
            holder.itemView.post(() -> {
                changeQuantity(model, true);
                // Re-enable buttons after a short delay
                holder.itemView.postDelayed(() -> {
                    holder.plusBtn.setEnabled(true);
                    holder.minusBtn.setEnabled(true);
                }, 300);
            });
        });
        holder.minusBtn.setOnClickListener(v -> {
            // Disable button temporarily to prevent rapid clicks
            holder.plusBtn.setEnabled(false);
            holder.minusBtn.setEnabled(false);
            // Post to next frame to ensure RecyclerView has finished current layout
            holder.itemView.post(() -> {
                changeQuantity(model, false);
                // Re-enable buttons after a short delay
                holder.itemView.postDelayed(() -> {
                    holder.plusBtn.setEnabled(true);
                    holder.minusBtn.setEnabled(true);
                }, 300);
            });
        });
    }


    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new CartAdapter.CartViewHolder(view);
    }

    private void calculateTotalPrice() {
        gotSum = true;
//        Toast.makeText(context, "Hi", Toast.LENGTH_SHORT).show();
        for (CartItemModel model : getSnapshots()) {
            totalPrice += model.getPrice() * model.getQuantity();
//            Log.i("Check", model.getPrice() +" "+ model.getQuantity());
        }
//        Toast.makeText(context, totalPrice+"", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("price");
        intent.putExtra("totalPrice", totalPrice);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public CartAdapter(@NonNull FirestoreRecyclerOptions<CartItemModel> options, Context context) {
        super(options);
        count = options.getSnapshots().size();
        this.context = context;
        // Enable stable IDs to help RecyclerView track items during rapid changes
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        // Provide stable IDs based on document ID to help RecyclerView track items
        // This prevents crashes when items are deleted during checkout
        try {
            if (position >= 0 && position < getSnapshots().size()) {
                String docId = getSnapshots().getSnapshot(position).getId();
                return docId.hashCode();
            }
        } catch (Exception e) {
            Log.e("CartAdapter", "Error getting item ID at position " + position, e);
        }
        return super.getItemId(position);
    }

    public void setCartAdapterListener(CartAdapterListener listener) {
        this.listener = listener;
    }
    private void changeQuantity(CartItemModel model, boolean plus) {
        // Validate model to prevent crashes
        if (model == null || model.getProductId() <= 0) {
            Log.e("CartAdapter", "Invalid model in changeQuantity");
            return;
        }

        FirebaseUtil.getProducts().whereEqualTo("productId", model.getProductId())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Object stockObj = document.getData().get("stock");
                                    if (stockObj != null) {
                                        stock[0] = (int) (long) stockObj;
                                    }
                                } catch (Exception e) {
                                    Log.e("CartAdapter", "Error getting stock", e);
                                    stock[0] = 0;
                                }
                            }
                        }
                    }
                });

        FirebaseUtil.getCartItems().whereEqualTo("productId", model.getProductId())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    String docId = document.getId();
                                    Object quantityObj = document.getData().get("quantity");
                                    if (quantityObj == null) {
                                        Log.e("CartAdapter", "Quantity is null for document: " + docId);
                                        continue;
                                    }
                                    
                                    int quantity = (int) (long) quantityObj;
                                    
                                    if (plus) {
                                        if (quantity < stock[0]) {
                                            // Update quantity - Firestore will trigger adapter update
                                            FirebaseUtil.getCartItems().document(docId).update("quantity", quantity + 1)
                                                    .addOnFailureListener(e -> {
                                                        Log.e("CartAdapter", "Failed to update quantity", e);
                                                        Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT).show();
                                                    });
                                            totalPrice += model.getPrice();
                                        } else {
                                            Toast.makeText(context, "Max stock available: " + stock[0], Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        totalPrice -= model.getPrice();
                                        if (quantity > 1) {
                                            // Update quantity - Firestore will trigger adapter update
                                            FirebaseUtil.getCartItems().document(docId).update("quantity", quantity - 1)
                                                    .addOnFailureListener(e -> {
                                                        Log.e("CartAdapter", "Failed to update quantity", e);
                                                        Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT).show();
                                                    });
                                        } else {
                                            // Delete item - Firestore will trigger adapter update
                                            FirebaseUtil.getCartItems().document(docId)
                                                    .delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            // Update badge when item is deleted
                                                            if (context instanceof MainActivity) {
                                                                ((MainActivity) context).addOrRemoveBadge();
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                    
                                    if (context instanceof MainActivity) {
                                        MainActivity activity = (MainActivity) context;
                                        activity.addOrRemoveBadge();

                                        Intent intent = new Intent("price");
                                        intent.putExtra("totalPrice", totalPrice);
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                    }
                                } catch (Exception e) {
                                    Log.e("CartAdapter", "Error updating quantity", e);
                                    Toast.makeText(context, "Error updating quantity", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Log.e("CartAdapter", "Failed to get cart items", task.getException());
                            Toast.makeText(context, "Failed to update cart", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        // Reset gotSum when data changes
        gotSum = false;
        totalPrice = 0;

        // Update badge count when cart data changes
        if (context instanceof MainActivity) {
            ((MainActivity) context).addOrRemoveBadge();
        }

        if (getItemCount() == 0){
            // Use callback if available, otherwise try to access views with null checks
            if (listener != null) {
                listener.onCartEmpty();
            } else if (context instanceof Activity) {
                Activity activity = (Activity) context;
                ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
                if (shimmerLayout != null) {
                    shimmerLayout.stopShimmer();
                    shimmerLayout.setVisibility(View.GONE);
                }
                View mainLayout = activity.findViewById(R.id.mainLinearLayout);
                if (mainLayout != null) {
                    mainLayout.setVisibility(View.VISIBLE);
                }
                View emptyCartView = activity.findViewById(R.id.emptyCartImageView);
                if (emptyCartView != null) {
                    emptyCartView.setVisibility(View.VISIBLE);
                }
            }
        }
        else {
            // Use callback if available, otherwise try to access views with null checks
            if (listener != null) {
                listener.onCartHasItems();
                // Also notify that items are loaded to stop shimmer
                listener.onItemsLoaded();
            } else if (context instanceof Activity) {
                Activity activity = (Activity) context;
                View emptyCartView = activity.findViewById(R.id.emptyCartImageView);
                if (emptyCartView != null) {
                    emptyCartView.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    public class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, singleProductPrice, productQuantity, minusBtn, plusBtn, originalPrice;
        ImageView productCartImage;
//        ViewGroup viewGroup;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.nameTextView);
            singleProductPrice = itemView.findViewById(R.id.priceTextView1);
            productPrice = itemView.findViewById(R.id.priceTextView);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            productQuantity = itemView.findViewById(R.id.quantityTextView);
            productCartImage = itemView.findViewById(R.id.productImageCart);
            minusBtn = itemView.findViewById(R.id.minusBtn);
            plusBtn = itemView.findViewById(R.id.plusBtn);
//            viewGroup = itemView.findViewById(android.R.id.content);
        }
    }
}
