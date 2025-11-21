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

/**
 * 订单列表适配器
 * 用于在RecyclerView中显示订单列表
 * 继承自FirestoreRecyclerAdapter，自动同步Firestore数据
 */
public class OrderListAdapter extends FirestoreRecyclerAdapter<OrderItemModel, OrderListAdapter.OrderListViewHolder> {

    private Context context;           // 上下文
    private AppCompatActivity activity; // 活动实例

    /**
     * 构造函数
     * @param options Firestore查询选项
     * @param context 上下文
     */
    public OrderListAdapter(@NonNull FirestoreRecyclerOptions<OrderItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    /**
     * 绑定ViewHolder数据
     * 将订单数据绑定到ViewHolder的UI组件上，并设置点击事件
     * @param holder ViewHolder实例
     * @param position 位置
     * @param model 订单数据模型
     */
    @Override
    protected void onBindViewHolder(@NonNull OrderListViewHolder holder, int position, @NonNull OrderItemModel model) {
        // 设置商品名称
        holder.productName.setText(model.getName());
        // 格式化并显示订单日期
        Timestamp timestamp = model.getTimestamp();
        String time = new SimpleDateFormat("dd MMM yyyy").format(timestamp.toDate());
        holder.orderDate.setText(time);
        // 使用Picasso加载商品图片
        Picasso.get().load(model.getImage()).into(holder.productImage);

        // 设置点击事件：点击订单项查看订单详情
        // 在点击监听器内部创建Fragment，确保每个订单显示正确的详情
        holder.itemView.setOnClickListener(v -> {
            if (activity == null) {
                return;
            }
            
            // 获取此订单项的文档ID
            String documentId = getSnapshots().getSnapshot(position).getId();
            
            // 检查是在Activity上下文（如OrdersListActivity）还是Fragment上下文
            // 如果main_frame_layout存在，说明在Fragment上下文（MainActivity）中
            View mainFrameLayout = activity.findViewById(R.id.main_frame_layout);
            if (mainFrameLayout != null) {
                // Fragment上下文 - 导航到Fragment
                Bundle bundle = new Bundle();
                bundle.putInt("orderId", model.getOrderId());
                bundle.putInt("productId", model.getProductId());
                bundle.putString("documentId", documentId);
                OrderDetailsFragment fragment = new OrderDetailsFragment();
                fragment.setArguments(bundle);
                
                // 如果Fragment未添加，则添加它
                if (!fragment.isAdded()) {
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_frame_layout, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            } else {
                // Activity上下文 - 在Toast中显示订单详情
                android.widget.Toast.makeText(activity, 
                    "Order: " + model.getName() + "\n" +
                    "Date: " + new java.text.SimpleDateFormat("dd MMM yyyy").format(model.getTimestamp().toDate()) + "\n" +
                    "Price: $" + model.getPrice() + "\n" +
                    "Quantity: " + model.getQuantity(),
                    android.widget.Toast.LENGTH_LONG).show();
            }
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
    public OrderListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new OrderListAdapter.OrderListViewHolder(view);
    }

    /**
     * 订单ViewHolder
     * 持有订单列表项的视图引用
     */
    public class OrderListViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;  // 商品图片
        TextView productName;    // 商品名称
        TextView orderDate;      // 订单日期

        /**
         * ViewHolder构造函数
         * 初始化所有UI组件
         * @param itemView 列表项视图
         */
        public OrderListViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImageOrder);
            productName = itemView.findViewById(R.id.nameTextView);
            orderDate = itemView.findViewById(R.id.dateTextView);
        }
    }
}
