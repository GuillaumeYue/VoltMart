package com.example.voltmart.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.fragments.ProductFragment;
import com.example.voltmart.model.CartItemModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

/**
 * 愿望单商品适配器
 * 用于在RecyclerView中显示愿望单商品列表
 * 继承自FirestoreRecyclerAdapter，自动同步Firestore数据
 * 功能包括：
 * - 显示愿望单商品
 * - 从愿望单移除商品
 * - 将愿望单商品添加到购物车
 */
public class WishlistProductAdapter extends FirestoreRecyclerAdapter<CartItemModel, WishlistProductAdapter.WishlistProductViewHolder> {
    private Context context;           // 上下文
    private AppCompatActivity activity; // 活动实例

    /**
     * 构造函数
     * @param options Firestore查询选项
     * @param context 上下文
     */
    public WishlistProductAdapter(@NonNull FirestoreRecyclerOptions<CartItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    /**
     * 绑定ViewHolder数据
     * 将愿望单商品数据绑定到ViewHolder的UI组件上
     * @param holder ViewHolder实例
     * @param position 位置
     * @param product 愿望单商品数据模型
     */
    @Override
    protected void onBindViewHolder(@NonNull WishlistProductAdapter.WishlistProductViewHolder holder, int position, @NonNull CartItemModel product) {
        holder.productNameTextView.setText(product.getName());
        Picasso.get().load(product.getImage()).into(holder.productImageView);
        holder.productPriceTextView.setText("$ "+ product.getPrice());
        holder.originalPrice.setText("$ " + product.getOriginalPrice());
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        // 计算折扣百分比并显示（使用Math.round进行四舍五入，提高精度）
        int discountPerc = 0;
        if (product.getOriginalPrice() > 0) {
            discountPerc = (int) Math.round((product.getOriginalPrice() - product.getPrice()) * 100.0 / product.getOriginalPrice());
        }
        holder.discountPercentage.setText(discountPerc + "% OFF");

        holder.productLinearLayout.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(product.getProductId());
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, fragment).addToBackStack(null).commit();
        });

        holder.addToCartBtn.setOnClickListener(v -> {
            addToCart(product, new MyCallback() {
                @Override
                public void onCallback(int stock) {
                    FirebaseUtil.getCartItems().whereEqualTo("productId", product.getProductId())
                            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        boolean documentExists = false;
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            documentExists = true;
                                            String docId = document.getId();
                                            int quantity = (int) (long) document.getData().get("quantity");
                                            if (quantity < stock) {
                                                FirebaseUtil.getCartItems().document(docId).update("quantity", quantity + 1)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show();
                                                            MainActivity activity = (MainActivity) context;
                                                            if (activity != null) {
                                                                activity.addOrRemoveBadge();
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            android.util.Log.e("WishlistProductAdapter", "Failed to update cart item quantity", e);
                                                            Toast.makeText(context, "Failed to add to cart. Please try again.", Toast.LENGTH_SHORT).show();
                                                        });
                                            } else
                                                Toast.makeText(context, "Max stock available: " + stock, Toast.LENGTH_SHORT).show();
                                        }
                                        if (!documentExists) {
                                            // Validate product data before adding to cart
                                            String productName = product.getName();
                                            String productImage = product.getImage();
                                            int productPrice = product.getPrice();
                                            
                                            if (productName == null || productName.trim().isEmpty() || productPrice < 0) {
                                                android.util.Log.e("WishlistProductAdapter", "Invalid product data - name: " + productName + ", image: " + productImage + ", price: " + productPrice);
                                                Toast.makeText(context, "Error: Product data is invalid. Please try again.", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            
                                            // Use placeholder image if product image is null or empty
                                            String cartImage = (productImage == null || productImage.trim().isEmpty()) 
                                                    ? "https://via.placeholder.com/300" 
                                                    : productImage;
                                            
                                            CartItemModel cartItem = new CartItemModel(product.getProductId(), product.getName(), cartImage, 1, product.getPrice(), product.getOriginalPrice(), Timestamp.now());
                                            FirebaseUtil.getCartItems().add(cartItem)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show();
                                                        MainActivity activity = (MainActivity) context;
                                                        if (activity != null) {
                                                            activity.addOrRemoveBadge();
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        android.util.Log.e("WishlistProductAdapter", "Failed to add item to cart", e);
                                                        Toast.makeText(context, "Failed to add to cart. Please try again.", Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    }
                                }
                            });
                }
            });
        });

        holder.removeWishlistBtn.setOnClickListener(v -> {
            FirebaseUtil.getWishlistItems().whereEqualTo("productId", product.getProductId())
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String docId = document.getId();

                                    FirebaseUtil.getWishlistItems().document(docId).delete()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                }
                                            });
                                }
                            }
                        }
                    });
        });
    }

    @NonNull
    @Override
    public WishlistProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wishlist_adapter,parent,false);
        activity = (AppCompatActivity) view.getContext();
        return new WishlistProductViewHolder(view);
    }

    private void addToCart(CartItemModel product, MyCallback myCallback) {
        FirebaseUtil.getProducts().whereEqualTo("productId", product.getProductId())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                int stock = (int) (long) document.getData().get("stock");
                                myCallback.onCallback(stock);
                            }
                        }
                    }
                });
    }

    public class WishlistProductViewHolder extends RecyclerView.ViewHolder{
        TextView productNameTextView, productPriceTextView, originalPrice, discountPercentage;
        ImageView productImageView;
        LinearLayout productLinearLayout;
        Button addToCartBtn, removeWishlistBtn;

        public WishlistProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImage);
            productNameTextView = itemView.findViewById(R.id.productName);
            productPriceTextView = itemView.findViewById(R.id.productPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            discountPercentage =itemView.findViewById(R.id.discountPercentage);
            productLinearLayout = itemView.findViewById(R.id.productLinearLayout);
            addToCartBtn = itemView.findViewById(R.id.addToCartBtn);
            removeWishlistBtn = itemView.findViewById(R.id.removeWishlistBtn);
        }
    }

    public interface MyCallback {
        void onCallback(int stock);
    }
}
