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

public class ReviewAdapter extends FirestoreRecyclerAdapter<ReviewModel, ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private AppCompatActivity activity;
    float totalRating = 0;
    boolean gotRating = false;

    public ReviewAdapter(@NonNull FirestoreRecyclerOptions<ReviewModel> options, Context context){
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ReviewViewHolder holder, int position, @NonNull ReviewModel model) {

        holder.nameTextView.setText(model.getName());
        Timestamp timestamp = model.getTimestamp();
        String date = new SimpleDateFormat("dd MMMM yyyy").format(timestamp.toDate());
        holder.dateTextView.setText(date);
        holder.ratingBar.setRating(model.getRating());
        holder.titleTextView.setText(model.getTitle());
        holder.reviewTextView.setText(model.getReview());
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review_adapter,parent,false);
        activity = (AppCompatActivity) view.getContext();
        return new ReviewViewHolder(view);
    }

    public class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, dateTextView, titleTextView, reviewTextView;
        RatingBar ratingBar;

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
