package com.example.voltmart.model;

import java.io.Serializable;
import java.util.List;

/**
 * 产品数据模型类
 * 用于存储和传递产品信息，包括名称、价格、图片、分类等
 * 实现Serializable接口以便在Activity之间传递
 */
public class ProductModel implements Serializable {
    private String name;                    // 产品名称
    private List<String> searchKey;        // 搜索关键词列表，用于产品搜索
    private String image;                  // 产品图片URL
    private String category;               // 产品分类
    private String description;            // 产品描述
    private String specification;          // 产品规格
    private int originalPrice;             // 原价
    private int discount;                  // 折扣金额
    private int price;                      // 现价（原价-折扣）
    private int productId;                 // 产品唯一ID
    private int stock;                     // 库存数量
    private String shareLink;              // 分享链接
    private float rating;                  // 产品评分（0-5）
    private int noOfRating;                // 评分人数

    /**
     * 无参构造函数
     * Firestore需要无参构造函数来反序列化数据
     */
    public ProductModel() {
    }

    /**
     * 全参构造函数
     * 用于创建完整的产品对象
     */
    public ProductModel(String name, List<String> searchKey, String image, String category, String description, String specification, int originalPrice, int discount, int price, int productId, int stock, String shareLink, float rating, int noOfRating) {
        this.name = name;
        this.searchKey = searchKey;
        this.image = image;
        this.category = category;
        this.description = description;
        this.specification = specification;
        this.originalPrice = originalPrice;
        this.discount = discount;
        this.price = price;
        this.productId = productId;
        this.stock = stock;
        this.shareLink = shareLink;
        this.rating = rating;
        this.noOfRating = noOfRating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(List<String> searchKey) {
        this.searchKey = searchKey;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public int getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(int originalPrice) {
        this.originalPrice = originalPrice;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getNoOfRating() {
        return noOfRating;
    }

    public void setNoOfRating(int noOfRating) {
        this.noOfRating = noOfRating;
    }
}
