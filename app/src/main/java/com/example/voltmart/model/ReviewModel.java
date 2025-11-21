package com.example.voltmart.model;

import com.google.firebase.Timestamp;

/**
 * 评论数据模型类
 * 用于存储和传递商品评论信息
 */
public class ReviewModel {
    String name;            // 评论者姓名
    float rating;           // 评分（0-5）
    String title;           // 评论标题
    String review;         // 评论内容
    Timestamp timestamp;   // 评论时间戳

    /**
     * 无参构造函数
     * Firestore需要无参构造函数来反序列化数据
     */
    public ReviewModel() {
    }

    /**
     * 全参构造函数
     * 用于创建完整的评论对象
     * @param name 评论者姓名
     * @param rating 评分
     * @param title 评论标题
     * @param review 评论内容
     * @param timestamp 评论时间戳
     */
    public ReviewModel(String name, float rating, String title, String review, Timestamp timestamp) {
        this.name = name;
        this.rating = rating;
        this.title = title;
        this.review = review;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
