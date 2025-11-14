package com.example.voltmart.utils;

import android.util.Log;
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

    // 检查用户是否登录
    public static boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    // 获取当前用户ID，带安全检查
    public static String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    public static CollectionReference getCategories(){
        return FirebaseFirestore.getInstance().collection("categories");
    }

    public static CollectionReference getProducts() {
        return FirebaseFirestore.getInstance().collection("products");
    }

    public static CollectionReference getBanner(){
        return FirebaseFirestore.getInstance().collection("banners");
    }

    // 修改getCartItems方法，添加空值检查
    public static CollectionReference getCartItems(){
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w("FirebaseUtil", "User not logged in, cannot access cart items");
            // 返回一个虚拟的集合引用，避免崩溃
            return FirebaseFirestore.getInstance().collection("dummy_collection");
        }
        return FirebaseFirestore.getInstance().collection("cart")
                .document(userId)
                .collection("items");
    }

    // 同样修改其他需要用户ID的方法
    public static CollectionReference getWishlistItems(){
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w("FirebaseUtil", "User not logged in, cannot access wishlist items");
            return FirebaseFirestore.getInstance().collection("dummy_collection");
        }
        return FirebaseFirestore.getInstance().collection("wishlists")
                .document(userId)
                .collection("items");
    }

    public static CollectionReference getOrderItems(){
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w("FirebaseUtil", "User not logged in, cannot access order items");
            return FirebaseFirestore.getInstance().collection("dummy_collection");
        }
        return FirebaseFirestore.getInstance().collection("orders")
                .document(userId)
                .collection("items");
    }

    public static CollectionReference getReviews(int pid){
        return FirebaseFirestore.getInstance().collection("reviews")
                .document(pid+"")
                .collection("review");
    }

    public static DocumentReference getDetails(){
        return FirebaseFirestore.getInstance().collection("dashboard")
                .document("details");
    }

    public static StorageReference getProductImageReference(String id){
        return FirebaseStorage.getInstance().getReference()
                .child("product_images")
                .child(id);
    }

    public static StorageReference getCategoryImageReference(String id){
        return FirebaseStorage.getInstance().getReference()
                .child("category_images")
                .child(id);
    }

    public static StorageReference getBannerImageReference(String id){
        return FirebaseStorage.getInstance().getReference()
                .child("banner_images")
                .child(id);
    }
}