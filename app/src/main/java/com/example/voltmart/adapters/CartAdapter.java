package com.example.voltmart.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
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

    @Override
    protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull CartItemModel model) {
        if (activity != null) {
            View emptyCartImageView = activity.findViewById(R.id.emptyCartImageView);
            if (emptyCartImageView != null) {
                emptyCartImageView.setVisibility(View.INVISIBLE);
            }
        }
        if (position == 0 && !gotSum) {
            calculateTotalPrice();
        }

        holder.productName.setText(model.getName());
        holder.singleProductPrice.setText("$ " + model.getPrice());
        holder.productPrice.setText("$ " + model.getPrice() * model.getQuantity());
        holder.originalPrice.setText("$ " + model.getOriginalPrice());
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.productQuantity.setText(model.getQuantity() + "");
        Picasso.get().load(model.getImage()).into(holder.productCartImage, new Callback() {
            @Override
            public void onSuccess() {
                if (holder.getBindingAdapterPosition() == getSnapshots().size()-1 && activity != null) {
                    ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
                    if (shimmerLayout != null) {
                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);
                    }
                    View mainLinearLayout = activity.findViewById(R.id.mainLinearLayout);
                    if (mainLinearLayout != null) {
                        mainLinearLayout.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onError(Exception e) {

            }
        });

        holder.plusBtn.setOnClickListener(v -> {
            changeQuantity(model, true);
        });
        holder.minusBtn.setOnClickListener(v -> {
            changeQuantity(model, false);
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
    }
    private void changeQuantity(CartItemModel model, boolean plus) {
        FirebaseUtil.getProducts().whereEqualTo("productId", model.getProductId())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                stock[0] = (int) (long) document.getData().get("stock");
                            }
                        }
                    }
                });

        FirebaseUtil.getCartItems().whereEqualTo("productId", model.getProductId())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String docId = document.getId();
                                int quantity = (int) (long) document.getData().get("quantity");
                                if (plus) {
                                    if (quantity < stock[0]) {
                                        FirebaseUtil.getCartItems().document(docId).update("quantity", quantity + 1);
                                        totalPrice += model.getPrice();
                                    } else
                                        Toast.makeText(context, "Max stock available: " + stock[0], Toast.LENGTH_SHORT).show();
                                } else {
                                    totalPrice -= model.getPrice();
                                    if (quantity > 1)
                                        FirebaseUtil.getCartItems().document(docId).update("quantity", quantity - 1);
                                    else
                                        FirebaseUtil.getCartItems().document(docId)
                                                .delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                    }
                                                });
                                }
                                MainActivity activity = (MainActivity) context;
                                activity.addOrRemoveBadge();

                                Intent intent = new Intent("price");
                                intent.putExtra("totalPrice", totalPrice);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            }
                        }
                    }
                });
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        if (context == null) {
            return;
        }
        
        try {
            Activity activity = (Activity) context;
            if (activity == null) {
                return;
            }
            
            if (getItemCount() == 0){
                ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
                if (shimmerLayout != null) {
                    shimmerLayout.stopShimmer();
                    shimmerLayout.setVisibility(View.GONE);
                }
                
                View mainLinearLayout = activity.findViewById(R.id.mainLinearLayout);
                if (mainLinearLayout != null) {
                    mainLinearLayout.setVisibility(View.VISIBLE);
                }
                
                View emptyCartImageView = activity.findViewById(R.id.emptyCartImageView);
                if (emptyCartImageView != null) {
                    emptyCartImageView.setVisibility(View.VISIBLE);
                }
            }
            else {
                View emptyCartImageView = activity.findViewById(R.id.emptyCartImageView);
                if (emptyCartImageView != null) {
                    emptyCartImageView.setVisibility(View.INVISIBLE);
                }
            }
        } catch (Exception e) {
            // Silently handle if views are not found
            e.printStackTrace();
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
