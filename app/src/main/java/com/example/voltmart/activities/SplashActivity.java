package com.example.voltmart.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.voltmart.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * 启动页面活动
 * 应用启动时显示的欢迎页面
 * 功能包括：
 * - 显示启动动画
 * - 检查用户登录状态
 * - 根据用户类型跳转到相应页面（登录页/主页/管理员页）
 */
public class SplashActivity extends AppCompatActivity {

    LottieAnimationView lottieAnimation; // Lottie动画视图

    /**
     * 活动创建时的初始化方法
     * 显示启动动画并检查用户登录状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 启用边缘到边缘显示
        setContentView(R.layout.activity_splash);

        // 初始化并播放启动动画
        lottieAnimation = findViewById(R.id.lottieAnimationView);
        lottieAnimation.playAnimation();

        // 延迟4秒后检查登录状态并跳转
        // 验证用户是否已登录
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if(currentUser == null){
                    // 用户未登录，跳转到登录页面
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                } else {
                    // 用户已登录，根据邮箱判断用户类型
                    if(FirebaseAuth.getInstance().getCurrentUser().getEmail().equals("alexyuehan@gmail.com"))
                        // 管理员邮箱，跳转到管理员页面
                        startActivity(new Intent(SplashActivity.this, AdminActivity.class));
                    else
                        // 普通用户，跳转到主页面
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }
                finish(); // 结束启动页面
            }
        }, 4000); // 延迟4秒
    }
}