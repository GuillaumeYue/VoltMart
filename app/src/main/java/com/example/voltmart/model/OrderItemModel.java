package com.example.voltmart.model;

import com.google.firebase.Timestamp;

/**
 * 订单商品数据模型
 * 用于存储订单中的商品信息和用户配送信息
 */
public class OrderItemModel {
    private int orderId;          // 订单ID
    private int productId;        // 商品ID
    private String name;          // 商品名称
    private String image;         // 商品图片URL
    private int price;            // 商品价格
    private int quantity;         // 购买数量
    private Timestamp timestamp;  // 订单时间戳
    private String fullName;      // 收货人姓名
    private String email;          // 收货人邮箱
    private String phoneNumber;    // 收货人电话
    private String address;        // 收货地址
    private String comments;       // 订单备注

    /**
     * 无参构造函数
     * Firestore需要无参构造函数来反序列化数据
     */
    public OrderItemModel() {
    }

    /**
     * 全参构造函数
     * @param orderId 订单ID
     * @param productId 商品ID
     * @param name 商品名称
     * @param image 商品图片URL
     * @param price 商品价格
     * @param quantity 购买数量
     * @param timestamp 订单时间戳
     * @param fullName 收货人姓名
     * @param email 收货人邮箱
     * @param phoneNumber 收货人电话
     * @param address 收货地址
     * @param comments 订单备注
     */
    public OrderItemModel(int orderId, int productId, String name, String image, int price, int quantity, Timestamp timestamp, String fullName, String email, String phoneNumber, String address, String comments) {
        this.orderId = orderId;
        this.productId = productId;
        this.name = name;
        this.image = image;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.comments = comments;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
