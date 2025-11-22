package com.example.voltmart.activities;

import static io.reactivex.rxjava3.internal.util.EndConsumerHelper.validate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.voltmart.R;
import com.example.voltmart.model.UserModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * 注册活动页面
 * 提供用户注册功能
 * 功能包括：
 * - 用户输入姓名、邮箱和密码
 * - 创建Firebase账户
 * - 发送邮箱验证
 * - 设置用户显示名称
 */
public class SignupActivity extends AppCompatActivity {

    // UI组件
    ProgressBar progressBar;        // 进度条，显示注册加载状态
    EditText nameEditText;          // 姓名输入框
    EditText emailEditText;         // 邮箱输入框
    EditText passEditText;          // 密码输入框
    ImageView nextBtn;              // 下一步/注册按钮
    TextView loginPageBtn;          // 跳转到登录页面的按钮

    /**
     * 活动创建时的初始化方法
     * 初始化UI组件并设置点击事件
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 启用边缘到边缘显示
        setContentView(R.layout.activity_signup);

        // 初始化UI组件
        progressBar = findViewById(R.id.progress_bar);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passEditText = findViewById(R.id.passEditText);
        nextBtn = findViewById(R.id.nextBtn);
        loginPageBtn = findViewById(R.id.loginPageBtn);

        // 设置注册按钮点击事件
        nextBtn.setOnClickListener(v -> createAccount());

        // 设置跳转到登录页面的按钮点击事件
        loginPageBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // 结束当前注册页面
        });
    }

    /**
     * 处理用户注册
     * 获取用户输入，验证后调用Firebase注册
     */
    private void createAccount() {
        String email = emailEditText.getText().toString(); // 获取邮箱
        String pass = passEditText.getText().toString();   // 获取密码
        boolean isValidated = validate(email, pass);       // 验证输入格式
        if (!isValidated)
            return; // 如果验证失败，直接返回
        createAccountInFirebase(email,pass); // 调用Firebase注册
    }

    /**
     * 在Firebase中创建账户
     * @param email 用户邮箱
     * @param pass 用户密码
     */
    void createAccountInFirebase(String email, String pass) {
        changeInProgress(true);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        changeInProgress(false);
                        if (task.isSuccessful()){
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // 发送邮箱验证
                                firebaseUser.sendEmailVerification();
                                
                                // 更新用户显示名称
                                String displayName = nameEditText.getText().toString();
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build();
                                firebaseUser.updateProfile(profileUpdates);
                                
                                // 将用户信息保存到Firestore
                                saveUserToFirestore(firebaseUser, displayName);
                            }
                            
                            Toast.makeText(SignupActivity.this, "Successfully created account, check email to verify", Toast.LENGTH_SHORT).show();
                            firebaseAuth.signOut();
                            finish();
                        }
                        else {
                            Toast.makeText(SignupActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    void changeInProgress(boolean inProgress){
        if (inProgress){
            progressBar.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            nextBtn.setVisibility(View.VISIBLE);
        }
    }

    boolean validate(String email, String pass){
        int flag=0;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailEditText.setError("Email is invalid");
            flag=1;
        }
        if (pass.length() < 6){
            passEditText.setError("Password must be of six characters");
            flag=1;
        }
        return flag == 0;
    }

    /**
     * 将用户信息保存到Firestore的users集合
     * @param firebaseUser Firebase用户对象
     * @param displayName 用户显示名称
     */
    private void saveUserToFirestore(FirebaseUser firebaseUser, String displayName) {
        if (firebaseUser == null) return;
        
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String phoneNumber = firebaseUser.getPhoneNumber();
        
        // 创建UserModel对象
        UserModel userModel = new UserModel(
                uid,
                email != null ? email : "",
                displayName != null && !displayName.isEmpty() ? displayName : "",
                phoneNumber != null ? phoneNumber : ""
        );
        
        // 保存到Firestore，使用uid作为文档ID
        FirebaseUtil.getUsers().document(uid).set(userModel)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            android.util.Log.d("SignupActivity", "User saved to Firestore successfully");
                        } else {
                            android.util.Log.e("SignupActivity", "Failed to save user to Firestore", task.getException());
                        }
                    }
                });
    }

}