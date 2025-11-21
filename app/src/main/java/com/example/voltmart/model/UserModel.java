package com.example.voltmart.model;

/**
 * 用户数据模型类
 * 用于存储和传递用户信息
 */
public class UserModel {
    private String uid;          // 用户唯一ID（Firebase Auth UID）
    private String email;        // 用户邮箱
    private String displayName;  // 用户显示名称
    private String phoneNumber;  // 用户电话号码

    /**
     * 无参构造函数
     * Firestore需要无参构造函数来反序列化数据
     */
    public UserModel() {
    }

    /**
     * 全参构造函数
     * 用于创建完整的用户对象
     * @param uid 用户ID
     * @param email 用户邮箱
     * @param displayName 用户显示名称
     * @param phoneNumber 用户电话号码
     */
    public UserModel(String uid, String email, String displayName, String phoneNumber) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}


