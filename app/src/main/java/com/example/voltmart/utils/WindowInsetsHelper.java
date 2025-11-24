package com.example.voltmart.utils;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * WindowInsets处理工具类
 * 用于处理EdgeToEdge模式下的系统栏遮挡问题
 */
public class WindowInsetsHelper {
    
    /**
     * 为顶部容器添加系统状态栏的padding
     * @param view 需要处理的View（通常是顶部容器LinearLayout）
     * @param additionalPaddingDp 额外的padding值（dp），默认0
     */
    public static void applyTopWindowInsets(View view, float additionalPaddingDp) {
        if (view == null) return;
        
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int additionalPadding = (int) (additionalPaddingDp * v.getResources().getDisplayMetrics().density);
            
            v.setPadding(
                v.getPaddingLeft(),
                systemBars.top + additionalPadding,
                v.getPaddingRight(),
                v.getPaddingBottom()
            );
            return insets;
        });
    }
    
    /**
     * 为顶部容器添加系统状态栏的padding（默认额外padding为0）
     * @param view 需要处理的View
     */
    public static void applyTopWindowInsets(View view) {
        applyTopWindowInsets(view, 0);
    }
}

