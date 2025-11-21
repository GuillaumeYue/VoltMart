package com.example.voltmart.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.fragments.ProductFragment;
import com.example.voltmart.R;
import com.example.voltmart.model.CartItemModel;
import com.example.voltmart.model.ProductModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

/**
 * 搜索适配器
 * 用于在RecyclerView中显示搜索结果商品列表
 * 继承自FirestoreRecyclerAdapter，自动同步Firestore数据
 * 主要用于分类Fragment和搜索结果展示
 */
public class SearchAdapter extends FirestoreRecyclerAdapter<ProductModel, SearchAdapter.SearchViewHolder> {

    private Context context;           // 上下文
    private AppCompatActivity activity; // 活动实例

    /**
     * 构造函数
     * @param options Firestore查询选项
     * @param context 上下文
     */
    public SearchAdapter(@NonNull FirestoreRecyclerOptions<ProductModel> options, Context context) {
        super(options);
        this.context = context;
    }

    /**
     * 绑定ViewHolder数据
     * 将商品数据绑定到ViewHolder的UI组件上
     * @param holder ViewHolder实例
     * @param position 位置
     * @param product 商品数据模型
     */
    @Override
    protected void onBindViewHolder(@NonNull SearchAdapter.SearchViewHolder holder, int position, @NonNull ProductModel product) {
        holder.productNameTextView.setText(product.getName());
        Picasso.get().load(product.getImage()).into(holder.productImageView);
        holder.productPriceTextView.setText("$ "+ product.getPrice());
        holder.originalPrice.setText("$ " + product.getOriginalPrice());
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        int discountPerc = (product.getDiscount() * 100) / product.getOriginalPrice();
        holder.discountPercentage.setText(discountPerc + "% OFF");

        DecimalFormat df = new DecimalFormat("#.#");
        float rating = Float.parseFloat(df.format(product.getRating()));
        holder.ratingBar.setRating(rating);
        holder.ratingTextView.setText(rating + "");
        holder.noOfRatingTextView.setText("(" + product.getNoOfRating() + ")");

        holder.productLinearLayout.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(product);
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, fragment).addToBackStack(null).commit();
        });

        holder.addToCartBtn.setOnClickListener(v -> {
            addToCart(product, new WishlistProductAdapter.MyCallback() {
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
                                                            android.util.Log.e("SearchAdapter", "Failed to update cart item quantity", e);
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
                                                android.util.Log.e("SearchAdapter", "Invalid product data - name: " + productName + ", image: " + productImage + ", price: " + productPrice);
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
                                                        android.util.Log.e("SearchAdapter", "Failed to add item to cart", e);
                                                        Toast.makeText(context, "Failed to add to cart. Please try again.", Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    }
                                }
                            });
                }
            });
        });
    }

    private void addToCart(ProductModel product, WishlistProductAdapter.MyCallback myCallback) {

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

    @NonNull
    @Override
    public SearchAdapter.SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_adapter,parent,false);
        activity = (AppCompatActivity) view.getContext();
        return new SearchViewHolder(view);
    }

    public class SearchViewHolder extends RecyclerView.ViewHolder {

        TextView productNameTextView, productPriceTextView, originalPrice, discountPercentage;
        ImageView productImageView;
        LinearLayout productLinearLayout;
        Button addToCartBtn;
        RatingBar ratingBar;
        TextView ratingTextView, noOfRatingTextView;
        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImage);
            productNameTextView = itemView.findViewById(R.id.productName);
            productPriceTextView = itemView.findViewById(R.id.productPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            discountPercentage =itemView.findViewById(R.id.discountPercentage);
            productLinearLayout = itemView.findViewById(R.id.productLinearLayout);
            addToCartBtn = itemView.findViewById(R.id.addToCartBtn);

            ratingBar = itemView.findViewById(R.id.ratingBar);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
            noOfRatingTextView = itemView.findViewById(R.id.noOfRatingTextView);
        }
    }
}
