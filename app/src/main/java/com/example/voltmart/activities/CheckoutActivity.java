package com.example.voltmart.activities;

import static io.reactivex.rxjava3.internal.util.EndConsumerHelper.validate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import com.example.voltmart.utils.WindowInsetsHelper;
import androidx.core.view.WindowInsetsCompat;
import com.example.voltmart.R;
import com.example.voltmart.fragments.OrderDetailsFragment;
import com.example.voltmart.model.OrderItemModel;
import com.example.voltmart.utils.EmailSender;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.Timestamp;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.view.CardInputWidget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * 结账活动页面
 * 处理订单结算流程，包括：
 * - 计算小计、GST（5%）、QST（9.975%）、配送费和总价
 * - 收集用户信息（姓名、邮箱、电话、地址）
 * - 选择支付方式（信用卡/货到付款）
 * - 使用Stripe处理信用卡支付
 * - 使用Google Places API选择地址
 * - 创建订单并更新库存
 */
public class CheckoutActivity extends AppCompatActivity {

    TextView subtotalTextView;
    TextView gstTextView;
    TextView qstTextView;
    TextView deliveryTextView;
    TextView totalTextView;
    TextView stockErrorTextView;
    Button checkoutBtn;
    ImageView backBtn;
    ImageButton locationPickerBtn;
    
    RadioGroup paymentMethodRadioGroup;
    RadioButton cardPaymentRadio;
    RadioButton cashOnDeliveryRadio;
    LinearLayout cardDetailsLayout;
    CardInputWidget cardInputWidget;

    SweetAlertDialog dialog;

    int subTotal;
    int gst;
    int qst;
    int delivery;
    int total;
    int count=0;
    volatile boolean adequateStock = true;
    volatile boolean done = false;

    EditText nameEditText;
    EditText emailEditText;
    EditText phoneEditText;
    EditText addressEditText;
    EditText commentEditText;
    String name, email, phone, address, comment;
    
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private FusedLocationProviderClient fusedLocationClient;
    
    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_51SUYMgIjjD15pVReVOPaFvkmwrKx9MNX3pTXrKPcLsy77jxoPV5ctVi11BOiAVqX0vRfGG8uRtVpSg2PdiYjN0Ru003RwUw0vC";
    private Stripe stripe;
    private String selectedPaymentMethod = "card";

    final int[] prevOrderId = new int[1];
    final int[] countOfOrderedItems = new int[1];
    final int[] priceOfOrders = new int[1];

    final List<String>[] productDocId = new ArrayList[1];
    final List<Integer>[] oldStock = new ArrayList[1];
    final List<Integer>[] quan = new ArrayList[1];
    final List<String>[] lessStock = new ArrayList[1];

    final List<String>[] cartDocument = new ArrayList[1];
    final List<String>[] productName = new ArrayList[1];
    final List<Integer>[] productPrice = new ArrayList[1];
    final List<Integer>[] productQuantity = new ArrayList[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        if (STRIPE_PUBLISHABLE_KEY == null || STRIPE_PUBLISHABLE_KEY.isEmpty() || !STRIPE_PUBLISHABLE_KEY.startsWith("pk_")) {
            Log.e("Stripe", "Invalid Stripe publishable key format. Key must start with 'pk_test_' or 'pk_live_'");
            Toast.makeText(this, "Payment system configuration error", Toast.LENGTH_LONG).show();
        }
        PaymentConfiguration.init(this, STRIPE_PUBLISHABLE_KEY);
        Log.d("Stripe", "Stripe PaymentConfiguration initialized with key: " + STRIPE_PUBLISHABLE_KEY.substring(0, Math.min(20, STRIPE_PUBLISHABLE_KEY.length())) + "...");
        
        setContentView(R.layout.activity_checkout);
        
        LinearLayout labelLinearLayout = findViewById(R.id.labelLinearLayout);
        if (labelLinearLayout != null) {
            WindowInsetsHelper.applyTopWindowInsets(labelLinearLayout, 4);
        }

        subtotalTextView = findViewById(R.id.subtotalTextView);
        gstTextView = findViewById(R.id.gstTextView);
        qstTextView = findViewById(R.id.qstTextView);
        deliveryTextView = findViewById(R.id.deliveryTextView);
        totalTextView = findViewById(R.id.totalTextView);
        stockErrorTextView = findViewById(R.id.stockErrorTextView);
        checkoutBtn = findViewById(R.id.checkoutBtn);
        backBtn = findViewById(R.id.backBtn);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        commentEditText = findViewById(R.id.commentEditText);
        locationPickerBtn = findViewById(R.id.locationPickerBtn);
        
        paymentMethodRadioGroup = findViewById(R.id.paymentMethodRadioGroup);
        cardPaymentRadio = findViewById(R.id.cardPaymentRadio);
        cashOnDeliveryRadio = findViewById(R.id.cashOnDeliveryRadio);
        cardDetailsLayout = findViewById(R.id.cardDetailsLayout);
        cardInputWidget = findViewById(R.id.cardInputWidget);
        
        stripe = new Stripe(this, STRIPE_PUBLISHABLE_KEY);
        
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyDEUGiwgdHRRw4-2gmQI3e1DE0MK_KzMwc");
        }
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        locationPickerBtn.setOnClickListener(v -> {
            openLocationPicker();
        });
        
        paymentMethodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.cardPaymentRadio) {
                selectedPaymentMethod = "card";
                cardDetailsLayout.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.cashOnDeliveryRadio) {
                selectedPaymentMethod = "cash";
                cardDetailsLayout.setVisibility(View.GONE);
            }
        });

        subTotal = getIntent().getIntExtra("price", 1200);
        subtotalTextView.setText("$ " + subTotal);
        
        // Calculate GST: 5% on the selling price (subtotal)
        gst = (int) Math.round(subTotal * 0.05);
        gstTextView.setText("$ " + gst);
        
        // Calculate QST: 9.975% on the selling price (excluding GST)
        qst = (int) Math.round(subTotal * 0.09975);
        qstTextView.setText("$ " + qst);
        
        // Calculate delivery fee
        if (subTotal >= 5000) {
            delivery = 0;
            deliveryTextView.setText("$ 0");
        } else {
            delivery = 10;
            deliveryTextView.setText("$ 10");
        }
        
        // Calculate total: Subtotal + GST + QST + Delivery
        total = subTotal + gst + qst + delivery;
        totalTextView.setText("$ " + total);

        checkoutBtn.setOnClickListener(v -> {
            // Process payment first, then process order
            processPayment();
        });
        
        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });

//        progressDialog = new ProgressDialog(this);
//        progressDialog.setCancelable(false);
//        progressDialog.setMessage("Processing...");
        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getCartProducts();
    }

    private void changeToFirebase(){
        // Check if activity is finishing or destroyed
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("lastOrderId", prevOrderId[0] + 1);
        map.put("countOfOrderedItems", countOfOrderedItems[0] + count);
        map.put("priceOfOrders", priceOfOrders[0] + total);

        FirebaseUtil.getDetails().update(map);

        // Add null checks for arrays
        if (productDocId[0] != null) {
            for (int i=0; i<productDocId[0].size(); i++){
                FirebaseUtil.getProducts().document(productDocId[0].get(i)).update("stock", oldStock[0].get(i) - quan[0].get(i));
            }
            Log.i("check 3",productDocId[0].size()+"");
        }

        // Delete cart items - ensure user is authenticated
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            if (cartDocument[0] != null && !cartDocument[0].isEmpty()) {
                final int[] deletedCount = {0};
                final int totalItems = cartDocument[0].size();
                Log.i("Cart", "Deleting " + totalItems + " cart items");
                
                for (String docId : cartDocument[0]){
                    if (docId != null && !docId.isEmpty()) {
                        FirebaseUtil.getCartItems().document(docId)
                                .delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        deletedCount[0]++;
                                        if (task.isSuccessful()) {
                                            Log.i("Cart", "Cart item deleted: " + docId);
                                        } else {
                                            Log.e("Cart", "Failed to delete cart item: " + docId + ", Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                                        }
                                        
                                        // When all items are deleted, proceed
                                        if (deletedCount[0] == totalItems) {
                                            Log.i("Cart", "All cart items deletion completed. Success: " + (deletedCount[0] == totalItems));
                                        }
                                    }
                                });
                    }
                }
            } else {
                Log.w("Cart", "No cart items to delete - cartDocument is null or empty");
            }
        } else {
            Log.e("Cart", "User not authenticated, cannot delete cart items");
        }

        String subject = "Your Order is successfully placed with VoltMart!";
        String messageBody = "Dear " + name + ",\n\n" +
                "Thank you for placing your order with VoltMart. We are excited to inform you that your order has been successfully placed.\n\n" +
                "Order Details:\n" +
                "-----------------------------------------------------------------------------------\n" +
                String.format("%-50s %-10s %-10s\n", "Product Name", "Quantity", "Price") +
                "-----------------------------------------------------------------------------------\n";
        if (productName[0] != null) {
            for (int i = 0; i < productName[0].size(); i++) {
                messageBody += String.format("%-50s %-10s $%-10d\n", productName[0].get(i), productQuantity[0].get(i), productPrice[0].get(i));
            }
        }
        messageBody += "-----------------------------------------------------------------------------\n" +
                String.format("%-73s $%-10d\n", "Subtotal:", subTotal) +
                String.format("%-73s $%-10d\n", "GST (5%):", gst) +
                String.format("%-73s $%-10d\n", "QST (9.975%):", qst) +
                String.format("%-73s $%-10d\n", "Delivery:", delivery) +
                String.format("%-73s $%-10d\n", "Total:", total) +
                "-----------------------------------------------------------------------------\n\n" +
                "Thank you for choosing our service. If you have any questions or concerns, feel free to contact our customer support.\n\n" +
                "Best Regards,\n" +
                "VoltMart Team";
        EmailSender emailSender = new EmailSender(subject, messageBody, email);
        Log.i("startEmail", email);
        emailSender.sendEmail();

        // Show dialog on UI thread and check if activity is still valid
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    // Dismiss progress dialog if it's showing
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismissWithAnimation();
                    }
                    
                    new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Order placed Successfully!")
                            .setContentText("You will shortly receive an email confirming the order details.")
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    sweetAlertDialog.dismissWithAnimation();
                                    Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                                    intent.putExtra("orderPlaced", true);
                                    startActivity(intent);
                                    finish();
                                }
                            }).show();
                }
            }
        });
    }

    private void processOrder(FirestoreCallback callback) {
        if (!validate())
            return;

        name = nameEditText.getText().toString();
        email = emailEditText.getText().toString();
        phone = phoneEditText.getText().toString();
        address = addressEditText.getText().toString();
        comment = commentEditText.getText().toString();

        productDocId[0] = new ArrayList<>();
        oldStock[0] = new ArrayList<>();
        quan[0] = new ArrayList<>();
        lessStock[0] = new ArrayList<>();

        cartDocument[0] = new ArrayList<>();
        productName[0] = new ArrayList<>();
        productPrice[0] = new ArrayList<>();
        productQuantity[0] = new ArrayList<>();
        //        final OrderItemModel[] item = new OrderItemModel[1];
        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        prevOrderId[0] = (int) (long) task.getResult().get("lastOrderId");
                        countOfOrderedItems[0] = (int) (long) task.getResult().get("countOfOrderedItems");
                        priceOfOrders[0] = (int) (long) task.getResult().get("priceOfOrders");
                    }
                });

        FirebaseUtil.getCartItems()
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                count++;
                                cartDocument[0].add(document.getId());
                                productName[0].add(document.get("name").toString());
                                productPrice[0].add((int) (long) document.get("price"));
                                productQuantity[0].add((int) (long) document.get("quantity"));

                                OrderItemModel item = new OrderItemModel(prevOrderId[0] + 1, (int) (long) document.get("productId"), document.get("name").toString(), document.get("image").toString(),
                                        (int) (long) document.get("price"), (int) (long) document.get("quantity"), Timestamp.now(), name, email, phone, address, comment);

                                FirebaseFirestore.getInstance().collection("orders").document(FirebaseAuth.getInstance().getUid()).collection("items").add(item);
                                int quantity = (int) (long) document.get("quantity");

                                callback.onCallback(document, quantity);

                            }
                            Log.i("check0", done + "");

                            dialog.show();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                    callback.onCallback(adequateStock);
                                }
                            }, 2000);

                        } else {
                            new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText("Order Failed!")
                                    .setContentText("Something went wrong, please try again.")
                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                            Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }).show();
                        }
                    }
                });
    }

    public interface FirestoreCallback {
//        void onCallback(String docid, int oldstock, int quan);

        void onCallback(QueryDocumentSnapshot document, int quantity);

        void onCallback(boolean adequateStock);
    }

    private boolean validate() {
        boolean isValid = true;
        if (nameEditText.getText().toString().trim().length() == 0) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (emailEditText.getText().toString().trim().length() == 0) {
            emailEditText.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailEditText.getText().toString().trim()).matches()){
            emailEditText.setError("Email is not valid");
            isValid = false;
        }
        if (phoneEditText.getText().toString().trim().length() == 0) {
            phoneEditText.setError("Phone Number is required");
            isValid = false;
        }
        else if (phoneEditText.getText().toString().trim().length() != 10) {
            phoneEditText.setError("Phone number is not valid");
            isValid = false;
        }
        if (addressEditText.getText().toString().trim().length() == 0) {
            addressEditText.setError("Address is required");
            isValid = false;
        }
        return isValid;
    }

    private void processPayment() {
        // Validate form first
        if (!validate()) {
            return;
        }
        
        if (selectedPaymentMethod.equals("cash")) {
            // Cash on delivery - proceed directly to order processing
            processOrder(new FirestoreCallback() {
                @Override
                public void onCallback(QueryDocumentSnapshot document, int quantity) {
                    FirebaseUtil.getProducts().whereEqualTo("productId", document.get("productId"))
                            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        String docId = task.getResult().getDocuments().get(0).getId();
                                        int stock = (int) (long) task.getResult().getDocuments().get(0).get("stock");
                                        productDocId[0].add(docId);
                                        oldStock[0].add(stock);
                                        quan[0].add(quantity);

                                        if (stock < quantity){
                                            adequateStock = false;
                                            lessStock[0].add(document.get("name").toString());
                                        }

                                        done = true;
                                        Log.i("done", "check");

                                    } else
                                        Toast.makeText(CheckoutActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                }
                            });


                }

                @Override
                public void onCallback(boolean adequateStock) {
                    if (!adequateStock){
                        Log.i("check","1");
                        String errorText = "*The following product(s) have less stock left:";
                        for (int i=0; i<lessStock[0].size(); i++){
                            errorText += "\n\t\t\t• "+ lessStock[0].get(i) + " has only "+oldStock[0].get(i)+" stock left";
                        }
                        stockErrorTextView.setText(errorText);
                        stockErrorTextView.setVisibility(View.VISIBLE);
                        Toast.makeText(CheckoutActivity.this, "One of the products has got less stock left :(", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Log.i("check","2");
                        changeToFirebase();
                    }
                }
            });
            return;
        }
        
        // Card payment - validate and create payment method
        PaymentMethodCreateParams params = cardInputWidget.getPaymentMethodCreateParams();
        if (params == null) {
            Toast.makeText(this, "Please enter complete card details", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading dialog
        dialog.show();
        
        stripe.createPaymentMethod(params, new com.stripe.android.ApiResultCallback<PaymentMethod>() {
            @Override
            public void onSuccess(PaymentMethod paymentMethod) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Log.i("Stripe", "Payment method created: " + (paymentMethod.id != null ? paymentMethod.id : "N/A"));
                    Toast.makeText(CheckoutActivity.this, "Payment method validated successfully", Toast.LENGTH_SHORT).show();
                    
                    // Proceed with order processing
                    processOrder(new FirestoreCallback() {
                        @Override
                        public void onCallback(QueryDocumentSnapshot document, int quantity) {
                            FirebaseUtil.getProducts().whereEqualTo("productId", document.get("productId"))
                                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                String docId = task.getResult().getDocuments().get(0).getId();
                                                int stock = (int) (long) task.getResult().getDocuments().get(0).get("stock");
                                                productDocId[0].add(docId);
                                                oldStock[0].add(stock);
                                                quan[0].add(quantity);

                                                if (stock < quantity){
                                                    adequateStock = false;
                                                    lessStock[0].add(document.get("name").toString());
                                                }

                                                done = true;
                                                Log.i("done", "check");

                                            } else
                                                Toast.makeText(CheckoutActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                        }
                                    });


                        }

                        @Override
                        public void onCallback(boolean adequateStock) {
                            if (!adequateStock){
                                Log.i("check","1");
                                String errorText = "*The following product(s) have less stock left:";
                                for (int i=0; i<lessStock[0].size(); i++){
                                    errorText += "\n\t\t\t• "+ lessStock[0].get(i) + " has only "+oldStock[0].get(i)+" stock left";
                                }
                                stockErrorTextView.setText(errorText);
                                stockErrorTextView.setVisibility(View.VISIBLE);
                                Toast.makeText(CheckoutActivity.this, "One of the products has got less stock left :(", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Log.i("check","2");
                                changeToFirebase();
                            }
                        }
                    });
                });
            }
            
            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    String errorMessage = e.getMessage();
                    Log.e("Stripe", "Payment method creation failed: " + errorMessage);
                    
                    // Provide user-friendly error messages
                    String userMessage;
                    if (errorMessage != null && errorMessage.contains("Invalid API Key")) {
                        userMessage = "Payment configuration error. Please contact support.";
                        Log.e("Stripe", "Invalid API Key detected. Check STRIPE_PUBLISHABLE_KEY configuration.");
                    } else if (errorMessage != null && errorMessage.contains("card")) {
                        userMessage = "Card validation failed. Please check your card details.";
                    } else if (errorMessage != null) {
                        userMessage = "Payment failed: " + errorMessage;
                    } else {
                        userMessage = "Payment failed. Please try again.";
                    }
                    
                    Toast.makeText(CheckoutActivity.this, userMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void openLocationPicker() {
        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Use Autocomplete.IntentBuilder for full-screen autocomplete
        try {
            // Set the fields to specify which types of place data to return
            java.util.List<Place.Field> fields = java.util.Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG
            );
            
            // Start the autocomplete intent
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("Places", "Error opening location picker: " + e.getMessage());
            Toast.makeText(this, "Error opening location picker. Please enter address manually.", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                // Set the address in the EditText
                addressEditText.setText(place.getAddress());
                Log.i("Places", "Place: " + place.getName() + ", " + place.getAddress());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("Places", "Error: " + status.getStatusMessage());
                Toast.makeText(this, "Error selecting location: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open location picker
                openLocationPicker();
            } else {
                Toast.makeText(this, "Location permission is required to pick address from map", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}