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

public class UsersListAdapter extends FirestoreRecyclerAdapter<UserModel, UsersListAdapter.UsersListViewHolder> {

    private Context context;
    private AppCompatActivity activity;

    public UsersListAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull UsersListViewHolder holder, int position, @NonNull UserModel model) {
        String displayName = model.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "User";
        }
        holder.userName.setText(displayName);
        holder.userEmail.setText(model.getEmail() != null ? model.getEmail() : "No email");
        
        String phone = model.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            holder.userPhone.setText(phone);
            holder.userPhone.setVisibility(View.VISIBLE);
        } else {
            holder.userPhone.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public UsersListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new UsersListViewHolder(view);
    }

    public class UsersListViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView userEmail;
        TextView userPhone;

        public UsersListViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userNameTextView);
            userEmail = itemView.findViewById(R.id.userEmailTextView);
            userPhone = itemView.findViewById(R.id.userPhoneTextView);
        }
    }
}


