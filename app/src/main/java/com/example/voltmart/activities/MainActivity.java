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
import com.example.voltmart.utils.WindowInsetsHelper;
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

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    LinearLayout searchLinearLayout;
    MaterialSearchBar searchBar;

    HomeFragment homeFragment;
    CartFragment cartFragment;
    SearchFragment searchFragment;
    WishlistFragment wishlistFragment;
    ProfileFragment profileFragment;

    FragmentManager fm;
    FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        searchLinearLayout = findViewById(R.id.linearLayout);
        searchBar = findViewById(R.id.searchBar);
        WindowInsetsHelper.applyTopWindowInsets(searchLinearLayout, 4);

        homeFragment = new HomeFragment();
        cartFragment = new CartFragment();
        wishlistFragment = new WishlistFragment();
        profileFragment = new ProfileFragment();
        searchFragment = new SearchFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                fm = getSupportFragmentManager();
                transaction = fm.beginTransaction();

                if (item.getItemId() == R.id.home) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    transaction.replace(R.id.main_frame_layout, homeFragment, "home");
                } else if (item.getItemId() == R.id.cart) {
                    if (!cartFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, cartFragment, "cart");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.wishlist) {
                    if (!wishlistFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, wishlistFragment, "wishlist");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.profile) {
                    if (!profileFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, profileFragment, "profile");
                        transaction.addToBackStack(null);
                    }
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
                
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
                if (currentFragment instanceof SearchFragment || currentFragment instanceof HomeFragment) {
                    showSearchBar();
                } else {
                    hideSearchBar();
                }
            }
        });

        searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                super.onSearchStateChanged(enabled);
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
                    }
                }
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                super.onSearchConfirmed(text);
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
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
                if (currentFragment instanceof SearchFragment) {
                    searchBar.closeSearch();
                }
            }
        });

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
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
            badge.setVisible(false);
            badge.clearNumber();
            return;
        }
        
        FirebaseUtil.getCartItems().get()
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