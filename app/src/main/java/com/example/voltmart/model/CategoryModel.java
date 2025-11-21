package com.example.voltmart.model;

/**
 * 分类数据模型类
 * 用于存储和传递商品分类信息
 */
public class CategoryModel {
    private String name;      // 分类名称
    private String icon;      // 分类图标URL
    private String color;     // 分类颜色
    private String brief;     // 分类简介
    private int categoryId;   // 分类唯一ID

    /**
     * 无参构造函数
     * Firestore需要无参构造函数来反序列化数据
     */
    public CategoryModel() {
    }

    /**
     * 全参构造函数
     * 用于创建完整的分类对象
     * @param name 分类名称
     * @param icon 分类图标URL
     * @param color 分类颜色
     * @param brief 分类简介
     * @param categoryId 分类ID
     */
    public CategoryModel(String name, String icon, String color, String brief, int categoryId) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.brief = brief;
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBrief() {
        return brief;
    }

    public void setBrief(String brief) {
        this.brief = brief;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
}
