package com.example.voltmart.activities;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.voltmart.R;
import com.example.voltmart.fragments.CartFragment;
import com.example.voltmart.fragments.HomeFragment;
import com.example.voltmart.fragments.ProductFragment;
import com.example.voltmart.fragments.ProfileFragment;
import com.example.voltmart.fragments.SearchFragment;
import com.example.voltmart.fragments.WishlistFragment;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;

/**
 * 主活动页面
 * 应用程序的主入口，管理底部导航和各个Fragment的切换
 * 功能包括：
 * - 底部导航栏管理（首页、购物车、愿望单、个人资料）
 * - 搜索功能
 * - 购物车徽章显示
 * - 深度链接处理
 */
public class MainActivity extends AppCompatActivity {

    // UI组件
    BottomNavigationView bottomNavigationView;  // 底部导航栏
    LinearLayout searchLinearLayout;            // 搜索栏布局
    MaterialSearchBar searchBar;                // 搜索栏

    // Fragment实例
    HomeFragment homeFragment;      // 首页Fragment
    CartFragment cartFragment;      // 购物车Fragment
    SearchFragment searchFragment;  // 搜索Fragment
    WishlistFragment wishlistFragment; // 愿望单Fragment
    ProfileFragment profileFragment;   // 个人资料Fragment

    // Fragment管理
    FragmentManager fm;           // Fragment管理器
    FragmentTransaction transaction; // Fragment事务

    /**
     * 活动创建时的初始化方法
     * 初始化所有Fragment、设置底部导航、搜索栏和深度链接处理
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 启用边缘到边缘显示
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        searchLinearLayout = findViewById(R.id.linearLayout);
        searchBar = findViewById(R.id.searchBar);

        // 创建所有Fragment实例
        homeFragment = new HomeFragment();
        cartFragment = new CartFragment();
        wishlistFragment = new WishlistFragment();
        profileFragment = new ProfileFragment();
        searchFragment = new SearchFragment();

        // 设置底部导航栏
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                fm = getSupportFragmentManager();
                transaction = fm.beginTransaction();

                // 根据选中的导航项切换Fragment
                if (item.getItemId() == R.id.home) {
                    // 首页：清空返回栈并显示首页
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    transaction.replace(R.id.main_frame_layout, homeFragment, "home");
                } else if (item.getItemId() == R.id.cart) {
                    // 购物车：如果未添加则添加并加入返回栈
                    if (!cartFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, cartFragment, "cart");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.wishlist) {
                    // 愿望单：如果未添加则添加并加入返回栈
                    if (!wishlistFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, wishlistFragment, "wishlist");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.profile) {
                    // 个人资料：如果未添加则添加并加入返回栈
                    if (!profileFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, profileFragment, "profile");
                        transaction.addToBackStack(null);
                    }
                }
                transaction.commit(); // 提交事务
                return true;
            }
        });
        bottomNavigationView.setSelectedItemId(R.id.home); // 默认选中首页
        addOrRemoveBadge(); // 更新购物车徽章

        // 监听Fragment返回栈变化，更新底部导航选中状态和搜索栏显示
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateBottomNavigationSelectedItem(); // 更新底部导航选中项
                
                // 根据当前Fragment显示/隐藏搜索栏
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
                if (currentFragment instanceof SearchFragment) {
                    showSearchBar(); // SearchFragment时显示搜索栏
                } else if (currentFragment instanceof HomeFragment) {
                    showSearchBar(); // HomeFragment时显示搜索栏
                } else {
                    hideSearchBar(); // 其他Fragment时隐藏搜索栏
                }
            }
        });

        // 设置搜索栏点击监听器 - 点击时导航到搜索Fragment（每次都创建新实例）
        // 这个监听器始终存在，不会被SearchFragment覆盖
        searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                super.onSearchStateChanged(enabled);
                // 当搜索栏打开时，导航到搜索Fragment（创建新实例）
                if (enabled) {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
                    if (!(currentFragment instanceof SearchFragment)) {
                        SearchFragment newSearchFragment = new SearchFragment();
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_frame_layout, newSearchFragment, "search")
                                .addToBackStack(null)
                                .commit();
                    }
                } else {
                    // 搜索栏关闭时，如果当前是SearchFragment，返回home
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
                    if (currentFragment instanceof SearchFragment) {
                        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                            getSupportFragmentManager().popBackStackImmediate();
                        } else {
                            HomeFragment homeFragment = new HomeFragment();
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.main_frame_layout, homeFragment, "home")
                                    .commitAllowingStateLoss();
                        }
                        // 不隐藏搜索栏，让返回栈监听器处理显示/隐藏
                    }
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                super.onSearchConfirmed(text);
                // 搜索确认时，确保在搜索Fragment中（创建新实例）
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
                if (!(currentFragment instanceof SearchFragment)) {
                    SearchFragment newSearchFragment = new SearchFragment();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_frame_layout, newSearchFragment, "search")
                            .addToBackStack(null)
                            .commit();
                }
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                super.onButtonClicked(buttonCode);
                // 返回按钮被点击时，关闭搜索栏并返回home
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
                if (currentFragment instanceof SearchFragment) {
                    searchBar.closeSearch();
                    // 不在这里处理导航，让onSearchStateChanged(false)处理
                    // 不隐藏搜索栏，让返回栈监听器处理显示/隐藏
                }
            }
        });

        handleDeepLink(); // 处理深度链接

        // 如果从订单页面返回，直接跳转到个人资料页面
        if (getIntent().getBooleanExtra("orderPlaced", false)){
            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, profileFragment, "profile").addToBackStack(null).commit();
            bottomNavigationView.setSelectedItemId(R.id.profile);
        }
    }

    /**
     * 显示搜索栏
     */
    public void showSearchBar(){
        searchLinearLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏搜索栏
     */
    public void hideSearchBar(){
        searchLinearLayout.setVisibility(View.GONE);
    }

    /**
     * 处理返回按钮按下事件
     * 如果有Fragment在返回栈中，则弹出；否则执行默认返回行为
     */
    @Override
    public void onBackPressed() {
        if (fm.getBackStackEntryCount() > 0)
            fm.popBackStack(); // 弹出返回栈中的Fragment
        else
            super.onBackPressed(); // 执行默认返回行为
    }

    /**
     * 根据当前显示的Fragment更新底部导航栏的选中状态
     */
    private void updateBottomNavigationSelectedItem() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);

        // 根据当前Fragment类型设置对应的导航项为选中状态
        if (currentFragment instanceof HomeFragment)
            bottomNavigationView.setSelectedItemId(R.id.home);
        else if (currentFragment instanceof CartFragment)
            bottomNavigationView.setSelectedItemId(R.id.cart);
        else if (currentFragment instanceof WishlistFragment)
            bottomNavigationView.setSelectedItemId(R.id.wishlist);
        else if (currentFragment instanceof ProfileFragment)
            bottomNavigationView.setSelectedItemId(R.id.profile);
    }

    /**
     * 添加或移除购物车徽章
     * 根据购物车中的商品数量显示或隐藏徽章
     */
    public void addOrRemoveBadge() {
        // 检查用户是否已登录，未登录则隐藏徽章
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            // 用户未登录，隐藏徽章
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
            badge.setVisible(false);
            badge.clearNumber();
            return;
        }
        
        // 获取购物车商品数量并更新徽章
        FirebaseUtil.getCartItems().get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            int n = task.getResult().size(); // 获取购物车商品数量
                            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
                            badge.setBackgroundColor(Color.parseColor("#FFF44336")); // 设置徽章背景色为红色
                            if (n > 0) {
                                // 有商品时显示徽章并设置数量
                                badge.setVisible(true);
                                badge.setNumber(n);
                            } else {
                                // 无商品时隐藏徽章
                                badge.setVisible(false);
                                badge.clearNumber();
                            }
                        }
                    }
                });
    }

    /**
     * 处理深度链接
     * 如果应用通过深度链接打开，解析链接并跳转到对应产品页面
     */
    private void handleDeepLink(){
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent())
                .addOnSuccessListener(new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null)
                            deepLink = pendingDynamicLinkData.getLink(); // 获取深度链接

                        if (deepLink != null){
                            Log.i("DeepLink", deepLink.toString());
                            // 从链接中获取产品ID
                            String productId = deepLink.getQueryParameter("product_id");
                            // 创建产品Fragment并显示
                            Fragment fragment = ProductFragment.newInstance(Integer.parseInt(productId));
                            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, fragment).addToBackStack(null).commit();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("Error123", e.toString());
                    }
                });
    }
}