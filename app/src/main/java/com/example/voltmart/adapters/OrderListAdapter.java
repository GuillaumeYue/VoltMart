package com.example.voltmart.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.fragments.OrderDetailsFragment;
import com.example.voltmart.model.OrderItemModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;

public class OrderListAdapter extends FirestoreRecyclerAdapter<OrderItemModel, OrderListAdapter.OrderListViewHolder> {

    private Context context;
    private AppCompatActivity activity;

    public OrderListAdapter(@NonNull FirestoreRecyclerOptions<OrderItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderListViewHolder holder, int position, @NonNull OrderItemModel model) {
        holder.productName.setText(model.getName());
        Timestamp timestamp = model.getTimestamp();
        String time = new SimpleDateFormat("dd MMM yyyy").format(timestamp.toDate());
        holder.orderDate.setText(time);
        Picasso.get().load(model.getImage()).into(holder.productImage);

        // Create fragment with correct orderId and productId inside click listener to ensure each order shows correct details
        holder.itemView.setOnClickListener(v -> {
            if (activity == null) {
                return;
            }
            
            // Get the document ID for this specific order item
            String documentId = getSnapshots().getSnapshot(position).getId();
            
            // Check if we're in an Activity context (like OrdersListActivity) or Fragment context
            // If main_frame_layout exists, we're in a Fragment context (MainActivity)
            View mainFrameLayout = activity.findViewById(R.id.main_frame_layout);
            if (mainFrameLayout != null) {
                // Fragment context - navigate to fragment
                Bundle bundle = new Bundle();
                bundle.putInt("orderId", model.getOrderId());
                bundle.putInt("productId", model.getProductId());
                bundle.putString("documentId", documentId);
                OrderDetailsFragment fragment = new OrderDetailsFragment();
                fragment.setArguments(bundle);
                
                if (!fragment.isAdded()) {
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_frame_layout, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            } else {
                // Activity context - show order details in a dialog or Toast
                android.widget.Toast.makeText(activity, 
                    "Order: " + model.getName() + "\n" +
                    "Date: " + new java.text.SimpleDateFormat("dd MMM yyyy").format(model.getTimestamp().toDate()) + "\n" +
                    "Price: $" + model.getPrice() + "\n" +
                    "Quantity: " + model.getQuantity(),
                    android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    @NonNull
    @Override
    public OrderListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new OrderListAdapter.OrderListViewHolder(view);
    }

    public class OrderListViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, orderDate;

        public OrderListViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImageOrder);
            productName = itemView.findViewById(R.id.nameTextView);
            orderDate = itemView.findViewById(R.id.dateTextView);
        }
    }
}
