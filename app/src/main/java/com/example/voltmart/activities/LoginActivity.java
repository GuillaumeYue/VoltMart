package com.example.voltmart.activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.transition.Explode;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.AlertDialog;
import android.widget.LinearLayout;

import com.example.voltmart.R;
import com.example.voltmart.model.UserModel;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * 登录活动页面
 * 提供用户登录功能，支持邮箱密码登录和Google账号登录
 * 登录成功后根据用户类型跳转到主页面或管理员页面
 */
public class LoginActivity extends AppCompatActivity {

    // UI组件
    ProgressBar progressBar;              // 进度条，显示登录加载状态
    EditText emailEditText, passEditText; // 邮箱和密码输入框
    ImageView loginBtn;                   // 登录按钮
    TextView signupPageBtn;               // 跳转到注册页面的按钮
    TextView forgotPasswordBtn;           // 忘记密码按钮
    Button googleLoginBtn;                 // Google登录按钮

    // Firebase和Google登录相关
    FirebaseAuth firebaseAuth;            // Firebase认证实例
    GoogleSignInClient googleSignInClient; // Google登录客户端

    /**
     * 活动创建时的初始化方法
     * 设置UI组件、绑定点击事件、初始化Firebase和Google登录
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 启用边缘到边缘显示
        setContentView(R.layout.activity_login);

        // 初始化UI组件
        progressBar = findViewById(R.id.progress_bar);
        emailEditText = findViewById(R.id.emailEditText);
        passEditText = findViewById(R.id.passEditText);
        loginBtn = findViewById(R.id.loginBtn);
        signupPageBtn = findViewById(R.id.signupPageBtn);
        forgotPasswordBtn = findViewById(R.id.forgotPasswordBtn);
        googleLoginBtn = findViewById(R.id.googleLoginBtn);

        // 设置登录按钮点击事件
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser(); // 执行用户登录
            }
        });
        
        // 设置跳转到注册页面的按钮点击事件
        signupPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });
        
        // 设置忘记密码按钮点击事件
        forgotPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
        
        // 初始化Firebase认证实例
        firebaseAuth = FirebaseAuth.getInstance();

        // 配置Google登录选项：需要获取用户的邮箱地址和ID Token
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // 请求ID Token用于Firebase认证
                .requestEmail() // 请求用户邮箱
                .build();

        // 创建Google登录客户端
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // 设置Google登录按钮点击事件
        googleLoginBtn.setOnClickListener(v -> googleSignin());

        // 设置退出动画效果
        getWindow().setExitTransition(new Explode());
    }

    /**
     * 处理用户登录
     * 获取用户输入的邮箱和密码，验证后调用Firebase登录
     */
    private void loginUser() {
        String email = emailEditText.getText().toString(); // 获取邮箱
        String pass = passEditText.getText().toString();   // 获取密码
        boolean isValidated = validate(email, pass);      // 验证输入格式
        if (! isValidated)
            return; // 如果验证失败，直接返回
        loginAccountInFirebase(email,pass); // 调用Firebase登录
    }

    /**
     * 使用邮箱和密码在Firebase中登录账户
     * @param email 用户邮箱
     * @param pass 用户密码
     */
    private void loginAccountInFirebase(String email, String pass) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        changeInProgress(true); // 显示加载进度条
        
        // 使用邮箱和密码登录
        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                changeInProgress(false); // 隐藏加载进度条
                if (task.isSuccessful()){
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // 检查邮箱是否已验证
                        if (firebaseUser.isEmailVerified()){
                            // 确保用户信息存在于Firestore中
                            ensureUserInFirestore(firebaseUser);
                            
                            // 根据用户邮箱判断是管理员还是普通用户
                            String userEmail = firebaseUser.getEmail();
                            if (userEmail != null && (userEmail.equals("alexyuehan@gmail.com") || userEmail.equals("hi4659287@gmail.com")))
                                // 管理员跳转到管理员页面，使用转场动画
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class), ActivityOptions.makeSceneTransitionAnimation(LoginActivity.this).toBundle());
                            else
                                // 普通用户跳转到主页面
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish(); // 结束当前登录页面
                        } else {
                            // 邮箱未验证，提示用户
                            Toast.makeText(LoginActivity.this, "Email not verified, please verify your email", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else {
                    // 登录失败，显示错误提示
                    Toast.makeText(LoginActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 控制登录过程中的UI状态
     * @param inProgress 是否正在登录中
     */
    private void changeInProgress(boolean inProgress){
        if (inProgress){
            // 显示进度条，隐藏登录按钮
            progressBar.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.GONE);
        } else {
            // 隐藏进度条，显示登录按钮
            progressBar.setVisibility(View.GONE);
            loginBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 验证用户输入的邮箱和密码格式
     * @param email 用户输入的邮箱
     * @param pass 用户输入的密码
     * @return 验证是否通过
     */
    private boolean validate(String email, String pass){
        int flag=0; // 错误标志，0表示无错误
        // 验证邮箱格式
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailEditText.setError("Email is invalid"); // 显示邮箱格式错误
            flag=1; // 设置错误标志
        }
        // 验证密码长度（至少6个字符）
        if (pass.length() < 6) {
            passEditText.setError("Password must be of six characters"); // 显示密码长度错误
            flag = 1; // 设置错误标志
        }
        return flag == 0; // 返回验证结果
    }

    /**
     * 启动Google登录流程
     * 打开Google账号选择页面
     */
    private void googleSignin() {
        Intent intent = googleSignInClient.getSignInIntent(); // 获取Google登录意图
        startActivityForResult(intent, 101); // 启动登录活动，请求码为101
    }

    /**
     * 处理从其他活动返回的结果
     * 当Google登录完成后，获取登录结果并进行Firebase认证
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 检查是否是Google登录的返回结果（请求码101）
        if (requestCode == 101){
            // 从返回的Intent中获取Google登录账户信息
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class); // 获取账户信息
                firebaseAuth(account.getIdToken()); // 使用ID Token进行Firebase认证
            } catch (Exception e){
                // 登录失败，显示错误提示
                Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 使用Google登录的ID Token进行Firebase认证
     * @param idToken Google登录返回的ID Token
     */
    private void firebaseAuth(String idToken) {
        // 使用Google ID Token创建Firebase认证凭证
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        // 使用凭证进行Firebase登录
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // 确保用户信息存在于Firestore中
                                ensureUserInFirestore(firebaseUser);
                                
                                // 登录成功，获取用户邮箱
                                String email = firebaseUser.getEmail();
                                // 根据邮箱判断用户类型并跳转
                                if (email != null && email.equals("alexyuehan@gmail.com"))  // 管理员邮箱
                                    startActivity(new Intent(LoginActivity.this, AdminActivity.class)); // 跳转到管理员页面
                                else
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class)); // 跳转到主页面
                                finish(); // 结束登录页面
                            }
                        }
                        else
                            // 认证失败，显示错误提示
                            Toast.makeText(LoginActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    /**
     * 确保用户信息存在于Firestore中
     * 如果用户不存在，则创建用户文档
     * @param firebaseUser Firebase用户对象
     */
    private void ensureUserInFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;
        
        String uid = firebaseUser.getUid();
        
        // 检查用户是否已存在于Firestore中
        FirebaseUtil.getUsers().document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            // 如果用户不存在，创建用户文档
                            if (document == null || !document.exists()) {
                                String email = firebaseUser.getEmail();
                                String displayName = firebaseUser.getDisplayName();
                                String phoneNumber = firebaseUser.getPhoneNumber();
                                
                                UserModel userModel = new UserModel(
                                        uid,
                                        email != null ? email : "",
                                        displayName != null ? displayName : "",
                                        phoneNumber != null ? phoneNumber : ""
                                );
                                
                                FirebaseUtil.getUsers().document(uid).set(userModel)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    android.util.Log.d("LoginActivity", "User created in Firestore successfully");
                                                } else {
                                                    android.util.Log.e("LoginActivity", "Failed to create user in Firestore", task.getException());
                                                }
                                            }
                                        });
                            }
                        } else {
                            android.util.Log.e("LoginActivity", "Error checking user in Firestore", task.getException());
                        }
                    }
                });
    }

    /**
     * 显示忘记密码对话框
     * 让用户输入邮箱地址，然后发送密码重置邮件
     */
    private void showForgotPasswordDialog() {
        // 创建对话框布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // 创建邮箱输入框
        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setMinWidth(400);
        
        // 如果邮箱输入框已有内容，自动填充
        String currentEmail = emailEditText.getText().toString();
        if (!currentEmail.isEmpty()) {
            emailInput.setText(currentEmail);
        }
        
        layout.addView(emailInput);

        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your email address and we'll send you a link to reset your password.");
        builder.setView(layout);
        
        // 设置确认按钮
        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }
            sendPasswordResetEmail(email);
        });
        
        // 设置取消按钮
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.show();
    }

    /**
     * 发送密码重置邮件
     * @param email 用户邮箱地址
     */
    private void sendPasswordResetEmail(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, 
                                    "Password reset email sent! Please check your inbox.", 
                                    Toast.LENGTH_LONG).show();
                        } else {
                            String errorMessage = "Failed to send reset email";
                            if (task.getException() != null) {
                                String exceptionMessage = task.getException().getMessage();
                                if (exceptionMessage != null) {
                                    if (exceptionMessage.contains("user-not-found")) {
                                        errorMessage = "No account found with this email address";
                                    } else if (exceptionMessage.contains("invalid-email")) {
                                        errorMessage = "Invalid email address";
                                    } else {
                                        errorMessage = exceptionMessage;
                                    }
                                }
                            }
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}