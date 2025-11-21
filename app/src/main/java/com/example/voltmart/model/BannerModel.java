package com.example.voltmart.model;

/**
 * 横幅数据模型类
 * 用于存储和传递首页横幅信息
 */
public class BannerModel {

    int bannerId;              // 横幅唯一ID
    String bannerImage;        // 横幅图片URL
    String description;        // 横幅描述
    String status;             // 横幅状态（"Live"或"Not Live"）

    /**
     * 无参构造函数
     * Firestore需要无参构造函数来反序列化数据
     */
    public BannerModel() {
    }

    /**
     * 全参构造函数
     * 用于创建完整的横幅对象
     * @param bannerId 横幅ID
     * @param bannerImage 横幅图片URL
     * @param description 横幅描述
     * @param status 横幅状态
     */
    public BannerModel(int bannerId, String bannerImage, String description, String status) {
        this.bannerId = bannerId;
        this.bannerImage = bannerImage;
        this.description = description;
        this.status = status;
    }

    public int getBannerId() {
        return bannerId;
    }

    public void setBannerId(int bannerId) {
        this.bannerId = bannerId;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
