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

public class SplashActivity extends AppCompatActivity {

    LottieAnimationView lottieAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        lottieAnimation = findViewById(R.id.lottieAnimationView);
        lottieAnimation.playAnimation();

        //validation logged in
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if(currentUser == null){
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                } else {
                    if(FirebaseAuth.getInstance().getCurrentUser().getEmail().equals("alexyuehan@gmail.com"))
                        startActivity(new Intent(SplashActivity.this, AdminActivity.class));
                    else
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }
                finish();
            }
        }, 4000);
    }
}