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

/**
 * 商品列表适配器
 * 用于在RecyclerView中显示商品列表
 * 继承自FirestoreRecyclerAdapter，自动同步Firestore数据
 */
public class ProductAdapter extends FirestoreRecyclerAdapter<ProductModel, ProductAdapter.ProductViewHolder> {

    private Context context;           // 上下文
    private AppCompatActivity activity; // 活动实例

    /**
     * 构造函数
     * @param options Firestore查询选项
     * @param context 上下文
     */
    public ProductAdapter(@NonNull FirestoreRecyclerOptions<ProductModel> options, Context context){
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
    protected void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull ProductModel product) {
        Log.d("ProductAdapter", "onBindViewHolder: " + position + " - " + product.getName());

        // 使用Picasso加载商品图片
        Picasso.get().load(product.getImage()).into(holder.productImage);
        // 设置商品名称
        holder.productLabel.setText(product.getName());
        // 设置现价
        holder.productPrice.setText("$ "+ product.getPrice());
        // 设置原价并添加删除线
        holder.originalPrice.setText("$ " + product.getOriginalPrice());
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        // 计算折扣百分比并显示（使用Math.round进行四舍五入，提高精度）
        int discountPerc = 0;
        if (product.getOriginalPrice() > 0) {
            discountPerc = (int) Math.round((product.getDiscount() * 100.0) / product.getOriginalPrice());
        }
        holder.discountPercentage.setText(discountPerc + "% OFF");

        // 设置点击事件：点击商品跳转到商品详情页面
        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(product);
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_frame_layout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }



    /**
     * 创建ViewHolder
     * 当RecyclerView需要新的ViewHolder时调用
     * @param parent 父ViewGroup
     * @param viewType 视图类型
     * @return 新的ViewHolder实例
     */
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
