package com.example.voltmart.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.fragments.CategoryFragment;
import com.example.voltmart.model.CategoryModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Picasso;

/**
 * 分类适配器
 * 用于在RecyclerView中显示商品分类列表
 * 继承自FirestoreRecyclerAdapter，自动同步Firestore数据
 */
public class CategoryAdapter extends FirestoreRecyclerAdapter<CategoryModel, CategoryAdapter.CategoryViewHolder> {

    private Context context;           // 上下文
    private AppCompatActivity activity; // 活动实例

    /**
     * 构造函数
     * @param options Firestore查询选项
     * @param context 上下文
     */
    public CategoryAdapter(@NonNull FirestoreRecyclerOptions<CategoryModel> options, Context context){
        super(options);
        this.context = context;
    }

    /**
     * 绑定ViewHolder数据
     * 将分类数据绑定到ViewHolder的UI组件上，并设置点击事件
     * 只显示status为"Enabled"或status为null的分类（兼容旧数据）
     * @param categoryViewHolder ViewHolder实例
     * @param i 位置
     * @param categoryModel 分类数据模型
     */
    @Override
    protected void onBindViewHolder(@NonNull CategoryViewHolder categoryViewHolder, int i, @NonNull CategoryModel categoryModel) {
        // 检查status：只显示"Enabled"或null（旧数据）的分类
        String status = categoryModel.getStatus();
        if (status != null && status.equals("Disabled")) {
            // 隐藏已禁用的分类：设置高度为0，宽度为0，并隐藏
            ViewGroup.LayoutParams params = categoryViewHolder.itemView.getLayoutParams();
            if (params != null) {
                params.height = 0;
                params.width = 0;
                categoryViewHolder.itemView.setLayoutParams(params);
            }
            categoryViewHolder.itemView.setVisibility(View.GONE);
            return;
        }
        
        // 显示分类项：确保可见并恢复布局参数
        categoryViewHolder.itemView.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams params = categoryViewHolder.itemView.getLayoutParams();
        if (params != null) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            categoryViewHolder.itemView.setLayoutParams(params);
        }
        
        // 设置分类名称
        categoryViewHolder.categoryLabel.setText(categoryModel.getName());
        // 使用Picasso加载分类图标
        Picasso.get().load(categoryModel.getIcon()).into(categoryViewHolder.categoryImage);
        
        // 设置点击事件：点击分类跳转到该分类的商品列表页面
        categoryViewHolder.itemView.setOnClickListener(v -> {
            AppCompatActivity activity = (AppCompatActivity) context;
            CategoryFragment categoryFragment = new CategoryFragment();
            // 创建Bundle传递分类名称
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putString("categoryName", categoryModel.getName());
            categoryFragment.setArguments(bundle);
            
            // 替换Fragment并加入返回栈
            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_frame_layout, categoryFragment);
            transaction.addToBackStack(null);
            transaction.commit();
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
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_adapter,parent,false);
        activity = (AppCompatActivity) view.getContext();
        return new CategoryViewHolder(view);
    }

    /**
     * 分类ViewHolder
     * 持有分类列表项的视图引用
     */
    public class CategoryViewHolder extends RecyclerView.ViewHolder{
        TextView categoryLabel;  // 分类名称
        ImageView categoryImage; // 分类图标

        /**
         * ViewHolder构造函数
         * 初始化所有UI组件
         * @param itemView 列表项视图
         */
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImage);
            categoryLabel = itemView.findViewById(R.id.categoryLabel);
        }
    }

}


