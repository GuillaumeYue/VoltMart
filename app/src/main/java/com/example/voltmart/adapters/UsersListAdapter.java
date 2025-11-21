package com.example.voltmart.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voltmart.R;
import com.example.voltmart.model.UserModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

/**
 * 用户列表适配器
 * 用于在RecyclerView中显示用户列表
 * 继承自FirestoreRecyclerAdapter，自动同步Firestore数据
 */
public class UsersListAdapter extends FirestoreRecyclerAdapter<UserModel, UsersListAdapter.UsersListViewHolder> {

    private Context context;           // 上下文
    private AppCompatActivity activity; // 活动实例

    /**
     * 构造函数
     * @param options Firestore查询选项
     * @param context 上下文
     */
    public UsersListAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context) {
        super(options);
        this.context = context;
    }

    /**
     * 绑定ViewHolder数据
     * 将用户数据绑定到ViewHolder的UI组件上
     * @param holder ViewHolder实例
     * @param position 位置
     * @param model 用户数据模型
     */
    @Override
    protected void onBindViewHolder(@NonNull UsersListViewHolder holder, int position, @NonNull UserModel model) {
        // 设置用户名称，如果为空则显示"User"
        String displayName = model.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "User";
        }
        holder.userName.setText(displayName);
        // 设置用户邮箱，如果为空则显示"No email"
        holder.userEmail.setText(model.getEmail() != null ? model.getEmail() : "No email");
        
        // 设置用户电话，如果有则显示，否则隐藏
        String phone = model.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            holder.userPhone.setText(phone);
            holder.userPhone.setVisibility(View.VISIBLE);
        } else {
            holder.userPhone.setVisibility(View.GONE);
        }
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
    public UsersListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new UsersListViewHolder(view);
    }

    /**
     * 用户ViewHolder
     * 持有用户列表项的视图引用
     */
    public class UsersListViewHolder extends RecyclerView.ViewHolder {
        TextView userName;   // 用户名称
        TextView userEmail;  // 用户邮箱
        TextView userPhone;  // 用户电话

        /**
         * ViewHolder构造函数
         * 初始化所有UI组件
         * @param itemView 列表项视图
         */
        public UsersListViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userNameTextView);
            userEmail = itemView.findViewById(R.id.userEmailTextView);
            userPhone = itemView.findViewById(R.id.userPhoneTextView);
        }
    }
}


