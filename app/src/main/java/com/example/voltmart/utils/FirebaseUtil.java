package com.example.voltmart.utils;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.voltmart.activities.MainActivity;
import com.example.voltmart.model.CartItemModel;
import com.example.voltmart.model.ProductModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseUtil {
    public static CollectionReference getCategories(){
        return FirebaseFirestore.getInstance().collection("categories");
    }

    public static CollectionReference getProducts() {
        return FirebaseFirestore.getInstance().collection("products");
    }

    public static CollectionReference getBanner(){
        return FirebaseFirestore.getInstance().collection("banners");
    }

    public static CollectionReference getCartItems(){
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // Return a dummy collection reference that won't be used if user is not logged in
            return FirebaseFirestore.getInstance().collection("cart").document("dummy").collection("items");
        }
        return FirebaseFirestore.getInstance().collection("cart").document(uid).collection("items");
    }

    public static CollectionReference getWishlistItems(){
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return FirebaseFirestore.getInstance().collection("wishlists").document("dummy").collection("items");
        }
        return FirebaseFirestore.getInstance().collection("wishlists").document(uid).collection("items");
    }

    public static CollectionReference getOrderItems(){
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return FirebaseFirestore.getInstance().collection("orders").document("dummy").collection("items");
        }
        return FirebaseFirestore.getInstance().collection("orders").document(uid).collection("items");
    }

    public static CollectionReference getReviews(int pid){
        return FirebaseFirestore.getInstance().collection("reviews").document(pid+"").collection("review");
    }

    public static DocumentReference getDetails(){
        return FirebaseFirestore.getInstance().collection("dashboard").document("details");
    }

    public static StorageReference getProductImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("product_images").child(id);
    }

    public static StorageReference getCategoryImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("category_images").child(id);
    }

    public static StorageReference getBannerImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("banner_images").child(id);
    }
}
