package com.example.voltmart.model;

import com.google.firebase.Timestamp;

/**
 * 购物车商品数据模型
 * 用于存储购物车中的商品信息
 */
public class CartItemModel {
    String name;              // 商品名称
    String image;             // 商品图片URL
    int productId;            // 商品ID
    int quantity;            // 购买数量
    int price;               // 现价
    int originalPrice;       // 原价
    Timestamp timestamp;     // 添加到购物车的时间戳

    /**
     * 无参构造函数
     * Firestore需要无参构造函数来反序列化数据
     */
    public CartItemModel() {
    }

    /**
     * 全参构造函数
     * @param productId 商品ID
     * @param name 商品名称
     * @param image 商品图片URL
     * @param quantity 购买数量
     * @param price 现价
     * @param originalPrice 原价
     * @param timestamp 添加到购物车的时间戳
     */
    public CartItemModel( int productId, String name, String image, int quantity, int price, int originalPrice, Timestamp timestamp) {
        this.name = name;
        this.image = image;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.originalPrice = originalPrice;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(int originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
