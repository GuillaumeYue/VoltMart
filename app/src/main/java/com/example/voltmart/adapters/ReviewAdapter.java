package com.example.voltmart.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.model.ReviewModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;

/**
 * 评论适配器
 * 用于在RecyclerView中显示商品评论列表
 * 继承自FirestoreRecyclerAdapter，自动同步Firestore数据
 */
public class ReviewAdapter extends FirestoreRecyclerAdapter<ReviewModel, ReviewAdapter.ReviewViewHolder> {

    private Context context;           // 上下文
    private AppCompatActivity activity; // 活动实例
    float totalRating = 0;             // 总评分（用于计算平均分）
    boolean gotRating = false;         // 是否已获取评分

    /**
     * 构造函数
     * @param options Firestore查询选项
     * @param context 上下文
     */
    public ReviewAdapter(@NonNull FirestoreRecyclerOptions<ReviewModel> options, Context context){
        super(options);
        this.context = context;
    }

    /**
     * 绑定ViewHolder数据
     * 将评论数据绑定到ViewHolder的UI组件上
     * @param holder ViewHolder实例
     * @param position 位置
     * @param model 评论数据模型
     */
    @Override
    protected void onBindViewHolder(@NonNull ReviewViewHolder holder, int position, @NonNull ReviewModel model) {
        // 设置评论者姓名
        holder.nameTextView.setText(model.getName());
        // 格式化并显示评论日期
        Timestamp timestamp = model.getTimestamp();
        String date = new SimpleDateFormat("dd MMMM yyyy").format(timestamp.toDate());
        holder.dateTextView.setText(date);
        // 设置评分
        holder.ratingBar.setRating(model.getRating());
        // 设置评论标题
        holder.titleTextView.setText(model.getTitle());
        // 设置评论内容
        holder.reviewTextView.setText(model.getReview());
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
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review_adapter,parent,false);
        activity = (AppCompatActivity) view.getContext();
        return new ReviewViewHolder(view);
    }

    /**
     * 评论ViewHolder
     * 持有评论列表项的视图引用
     */
    public class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;    // 评论者姓名
        TextView dateTextView;    // 评论日期
        TextView titleTextView;   // 评论标题
        TextView reviewTextView;  // 评论内容
        RatingBar ratingBar;      // 评分条

        /**
         * ViewHolder构造函数
         * 初始化所有UI组件
         * @param itemView 列表项视图
         */
        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            reviewTextView = itemView.findViewById(R.id.reviewTextView);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }
}
