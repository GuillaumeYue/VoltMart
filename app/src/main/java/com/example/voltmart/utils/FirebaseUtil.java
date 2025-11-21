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

/**
 * Firebase工具类
 * 提供统一的Firebase Firestore和Storage访问接口
 * 所有方法都是静态方法，可以直接调用
 */
public class FirebaseUtil {
    /**
     * 获取分类集合引用
     * @return 分类集合的引用
     */
    public static CollectionReference getCategories(){
        return FirebaseFirestore.getInstance().collection("categories");
    }

    /**
     * 获取产品集合引用
     * @return 产品集合的引用
     */
    public static CollectionReference getProducts() {
        return FirebaseFirestore.getInstance().collection("products");
    }

    /**
     * 获取横幅集合引用
     * @return 横幅集合的引用
     */
    public static CollectionReference getBanner(){
        return FirebaseFirestore.getInstance().collection("banners");
    }

    /**
     * 获取当前用户的购物车商品集合引用
     * 如果用户未登录，返回一个虚拟集合引用以避免空指针异常
     * @return 当前用户的购物车商品集合引用
     */
    public static CollectionReference getCartItems(){
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // 用户未登录时返回虚拟集合引用，避免空指针异常
            return FirebaseFirestore.getInstance().collection("cart").document("dummy").collection("items");
        }
        // 返回当前用户的购物车集合：cart/{uid}/items
        return FirebaseFirestore.getInstance().collection("cart").document(uid).collection("items");
    }

    /**
     * 获取当前用户的愿望单商品集合引用
     * 如果用户未登录，返回一个虚拟集合引用
     * @return 当前用户的愿望单商品集合引用
     */
    public static CollectionReference getWishlistItems(){
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return FirebaseFirestore.getInstance().collection("wishlists").document("dummy").collection("items");
        }
        // 返回当前用户的愿望单集合：wishlists/{uid}/items
        return FirebaseFirestore.getInstance().collection("wishlists").document(uid).collection("items");
    }

    /**
     * 获取当前用户的订单商品集合引用
     * 如果用户未登录，返回一个虚拟集合引用
     * @return 当前用户的订单商品集合引用
     */
    public static CollectionReference getOrderItems(){
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return FirebaseFirestore.getInstance().collection("orders").document("dummy").collection("items");
        }
        // 返回当前用户的订单集合：orders/{uid}/items
        return FirebaseFirestore.getInstance().collection("orders").document(uid).collection("items");
    }

    /**
     * 获取指定产品的评论集合引用
     * @param pid 产品ID
     * @return 该产品的评论集合引用：reviews/{pid}/review
     */
    public static CollectionReference getReviews(int pid){
        return FirebaseFirestore.getInstance().collection("reviews").document(pid+"").collection("review");
    }

    /**
     * 获取仪表板详情文档引用
     * @return 仪表板详情文档引用：dashboard/details
     */
    public static DocumentReference getDetails(){
        return FirebaseFirestore.getInstance().collection("dashboard").document("details");
    }

    /**
     * 获取产品图片的存储引用
     * @param id 产品ID
     * @return 产品图片的存储路径：product_images/{id}
     */
    public static StorageReference getProductImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("product_images").child(id);
    }

    /**
     * 获取分类图片的存储引用
     * @param id 分类ID
     * @return 分类图片的存储路径：category_images/{id}
     */
    public static StorageReference getCategoryImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("category_images").child(id);
    }

    /**
     * 获取横幅图片的存储引用
     * @param id 横幅ID
     * @return 横幅图片的存储路径：banner_images/{id}
     */
    public static StorageReference getBannerImageReference(String id){
        return FirebaseStorage.getInstance().getReference().child("banner_images").child(id);
    }
}
