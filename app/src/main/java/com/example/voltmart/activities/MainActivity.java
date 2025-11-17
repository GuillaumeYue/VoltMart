package com.example.voltmart.activities;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    HomeFragment homeFragment;
    CartFragment cartFragment;
    SearchFragment searchFragment;
    WishlistFragment wishlistFragment;
    ProfileFragment profileFragment;
    LinearLayout searchLinearLayout;
    MaterialSearchBar searchBar;

    FragmentManager fm;
    FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchLinearLayout = findViewById(R.id.linearLayout);
        searchBar = findViewById(R.id.searchBar);

        homeFragment = new HomeFragment();
        cartFragment = new CartFragment();
        wishlistFragment = new WishlistFragment();
        profileFragment = new ProfileFragment();
        searchFragment = new SearchFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.home);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                addOrRemoveBadge();
            }
        }, 1000); // 延迟1秒执行

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                fm = getSupportFragmentManager();
                transaction = fm.beginTransaction();

                if (item.getItemId() == R.id.home) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//                    transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                    transaction.replace(R.id.main_frame_layout, homeFragment, "home");
                } else if (item.getItemId() == R.id.cart) {
//                        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                    transaction.replace(R.id.main_frame_layout, cartFragment, "cart");
                    // Don't add to back stack for main navigation tabs
                } else if (item.getItemId() == R.id.wishlist) {
//                        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                    transaction.replace(R.id.main_frame_layout, wishlistFragment, "wishlist");
                    // Don't add to back stack for main navigation tabs
                } else if (item.getItemId() == R.id.profile) {
//                        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
                    transaction.replace(R.id.main_frame_layout, profileFragment, "profile");
                    // Don't add to back stack for main navigation tabs
                }
                transaction.commit();
                return true;
            }
        });
        bottomNavigationView.setSelectedItemId(R.id.home);
        addOrRemoveBadge();

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateBottomNavigationSelectedItem();
            }
        });

        try {
            searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
                @Override
                public void onSearchStateChanged(boolean enabled) {
                    super.onSearchStateChanged(enabled);
                    Log.d("MainActivity", "Search state changed: enabled=" + enabled);
                }

                @Override
                public void onSearchConfirmed(CharSequence text) {
                    Log.d("MainActivity", "Search confirmed: " + text);
                    // Only navigate to search fragment if not already there
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
                    if (currentFragment == null || !(currentFragment instanceof SearchFragment)) {
                        // Create a new SearchFragment instance for each search
                        SearchFragment newSearchFragment = new SearchFragment();
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_frame_layout, newSearchFragment, "search")
                                .addToBackStack(null)
                                .commit();
                    }
                    super.onSearchConfirmed(text);
                }

                @Override
                public void onButtonClicked(int buttonCode) {
                    Log.d("MainActivity", "Button clicked: buttonCode=" + buttonCode);
                    // Let SearchFragment handle the close button
                    super.onButtonClicked(buttonCode);
                }
            });
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing search bar", e);
            // 如果搜索栏初始化失败，隐藏它
            searchLinearLayout.setVisibility(View.GONE);
        }

        handleDeepLink();

        if (getIntent().getBooleanExtra("orderPlaced", false)){
            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, profileFragment, "profile").addToBackStack(null).commit();
            bottomNavigationView.setSelectedItemId(R.id.profile);
        }
    }

    public void showSearchBar(){
        searchLinearLayout.setVisibility(View.VISIBLE);
    }

    public void hideSearchBar(){
        searchLinearLayout.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (fm.getBackStackEntryCount() > 0)
            fm.popBackStack();
        else
            super.onBackPressed();
    }

    private void updateBottomNavigationSelectedItem() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);

        if (currentFragment instanceof HomeFragment)
            bottomNavigationView.setSelectedItemId(R.id.home);
        else if (currentFragment instanceof CartFragment)
            bottomNavigationView.setSelectedItemId(R.id.cart);
        else if (currentFragment instanceof WishlistFragment)
            bottomNavigationView.setSelectedItemId(R.id.wishlist);
        else if (currentFragment instanceof ProfileFragment)
            bottomNavigationView.setSelectedItemId(R.id.profile);
    }

    public void addOrRemoveBadge() {
        // 首先检查用户是否登录
        if (!FirebaseUtil.isUserLoggedIn()) {
            // 用户未登录，直接设置徽章为0
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
            badge.setBackgroundColor(Color.parseColor("#FFF44336"));
            badge.setVisible(false);
            badge.clearNumber();
            return;
        }

        // 获取购物车引用
        CollectionReference cartRef = FirebaseUtil.getCartItems();

        // 检查是否返回了虚拟集合（用户未登录但绕过检查的情况）
        if (cartRef.getPath().contains("dummy_collection")) {
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
            badge.setBackgroundColor(Color.parseColor("#FFF44336"));
            badge.setVisible(false);
            badge.clearNumber();
            return;
        }

        // 用户已登录，正常查询购物车
        cartRef.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            int n = task.getResult().size();
                            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
                            badge.setBackgroundColor(Color.parseColor("#FFF44336"));
                            if (n > 0) {
                                badge.setVisible(true);
                                badge.setNumber(n);
                            } else {
                                badge.setVisible(false);
                                badge.clearNumber();
                            }
                        } else {
                            // 查询失败，隐藏徽章
                            Log.e("MainActivity", "Error getting cart items", task.getException());
                            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
                            badge.setVisible(false);
                            badge.clearNumber();
                        }
                    }
                });
    }

    private void handleDeepLink(){
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent())
                .addOnSuccessListener(new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null)
                            deepLink = pendingDynamicLinkData.getLink();

                        if (deepLink != null){
                            Log.i("DeepLink", deepLink.toString());
                            String productId = deepLink.getQueryParameter("product_id");
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