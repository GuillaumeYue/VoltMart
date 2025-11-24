package com.example.voltmart.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.fragments.ProductFragment;
import com.example.voltmart.model.ProductModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ProductAdapter extends FirestoreRecyclerAdapter<ProductModel, ProductAdapter.ProductViewHolder> {

    private Context context;
    private AppCompatActivity activity;

    public ProductAdapter(@NonNull FirestoreRecyclerOptions<ProductModel> options, Context context){
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull ProductModel product) {
        Log.d("ProductAdapter", "onBindViewHolder: " + position + " - " + product.getName());

        Picasso.get().load(product.getImage()).into(holder.productImage);
        holder.productLabel.setText(product.getName());
        holder.productPrice.setText("$ "+ product.getPrice());
        holder.originalPrice.setText("$ " + product.getOriginalPrice());
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        int discountPerc = 0;
        if (product.getOriginalPrice() > 0) {
            discountPerc = (int) Math.round((product.getDiscount() * 100.0) / product.getOriginalPrice());
        }
        holder.discountPercentage.setText(discountPerc + "% OFF");

        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(product);
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_frame_layout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_adapter,parent,false);
        activity = (AppCompatActivity) view.getContext();
        return new ProductAdapter.ProductViewHolder(view);
    }

    /**
     * 商品ViewHolder
     * 持有商品列表项的视图引用
     */
    public class ProductViewHolder extends RecyclerView.ViewHolder{
        TextView productLabel;        // 商品名称
        TextView productPrice;        // 现价
        TextView originalPrice;       // 原价
        TextView discountPercentage;  // 折扣百分比
        ImageView productImage;       // 商品图片

        /**
         * ViewHolder构造函数
         * 初始化所有UI组件
         * @param itemView 列表项视图
         */
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productListImage);
            productLabel = itemView.findViewById(R.id.productLabel);
            productPrice = itemView.findViewById(R.id.productPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            discountPercentage = itemView.findViewById(R.id.discountPercentage);
        }
    }
}
