package com.example.voltmart.activities;

import static io.reactivex.rxjava3.internal.util.EndConsumerHelper.validate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.voltmart.R;
import com.example.voltmart.fragments.OrderDetailsFragment;
import com.example.voltmart.model.OrderItemModel;
import com.example.voltmart.utils.EmailSender;
import com.example.voltmart.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
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

public class CheckoutActivity extends AppCompatActivity {

    TextView subtotalTextView, deliveryTextView, totalTextView, stockErrorTextView;
    Button checkoutBtn;
    ImageView backBtn;

    SweetAlertDialog dialog;

    int subTotal, count=0;
    volatile boolean adequateStock = true, done = false;

    EditText nameEditText, emailEditText, phoneEditText, addressEditText, commentEditText;
    String name, email, phone, address, comment;

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
        setContentView(R.layout.activity_checkout);

        subtotalTextView = findViewById(R.id.subtotalTextView);
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

        subTotal = getIntent().getIntExtra("price", 1200);
        subtotalTextView.setText("$ " + subTotal);
        if (subTotal >= 5000) {
            deliveryTextView.setText("$ 0");
            totalTextView.setText("$ " + subTotal);
        } else {
            deliveryTextView.setText("$ 50");
            totalTextView.setText("$ " + (subTotal + 50));
        }

        checkoutBtn.setOnClickListener(v -> {
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
//                                                    callback.onCallback(docId, stock, quantity);
//                                                    if (!adequateStock){
//                                                        stockErrorTextView.setText("* One of the products has got out of stock :(");
//                                                        stockErrorTextView.setVisibility(View.VISIBLE);
//                                                    }

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
        Map<String, Object> map = new HashMap<>();
        map.put("lastOrderId", prevOrderId[0] + 1);
        map.put("countOfOrderedItems", countOfOrderedItems[0] + count);
        map.put("priceOfOrders", priceOfOrders[0] + subTotal);

        FirebaseUtil.getDetails().update(map);

        for (int i=0; i<productDocId[0].size(); i++){
            FirebaseUtil.getProducts().document(productDocId[0].get(i)).update("stock", oldStock[0].get(i) - quan[0].get(i));
        }
        Log.i("check 3",productDocId[0].size()+"");

        // Delete cart items with proper error handling
        int[] deletedCount = new int[1];
        int totalCartItems = cartDocument[0].size();

        if (totalCartItems == 0) {
            Log.w("CheckoutActivity", "No cart items to delete");
        } else {
            for (String docId : cartDocument[0]){
                FirebaseUtil.getCartItems().document(docId)
                        .delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    deletedCount[0]++;
                                    Log.i("CheckoutActivity", "Cart item deleted: " + docId + " (" + deletedCount[0] + "/" + totalCartItems + ")");
                                } else {
                                    Log.e("CheckoutActivity", "Failed to delete cart item: " + docId, task.getException());
                                }
                            }
                        });
            }
        }

        String subject = "Your Order is successfully placed with VoltMart!";
        String messageBody = "Dear " + name + ",\n\n" +
                "Thank you for placing your order with VoltMart. We are excited to inform you that your order has been successfully placed.\n\n" +
                "Order Details:\n" +
                "-----------------------------------------------------------------------------------\n" +
                String.format("%-50s %-10s %-10s\n", "Product Name", "Quantity", "Price") +
                "-----------------------------------------------------------------------------------\n";
        for (int i = 0; i < productName[0].size(); i++) {
            messageBody += String.format("%-50s %-10s $%-10d\n", productName[0].get(i), productQuantity[0].get(i), productPrice[0].get(i));
        }
        messageBody += "-----------------------------------------------------------------------------\n" +
                String.format("%-73s $%-10d\n", "Total:", subTotal) +
                "-----------------------------------------------------------------------------\n\n" +
                "Thank you for choosing our service. If you have any questions or concerns, feel free to contact our customer support.\n\n" +
                "Best Regards,\n" +
                "VoltMart Team";
        EmailSender emailSender = new EmailSender(subject, messageBody, email);
        Log.i("startEmail", email);
        emailSender.sendEmail();

        new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Order placed Successfully!")
                .setContentText("You will shortly receive an email confirming the order details.")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                        intent.putExtra("orderPlaced", true);
                        startActivity(intent);
                        finish();
                    }
                }).show();
    }

    private void processOrder(FirestoreCallback callback) {
        if (!validate())
            return;

        name = nameEditText.getText().toString();
        email = emailEditText.getText().toString();
        phone = phoneEditText.getText().toString();
        address = addressEditText.getText().toString();
        comment = commentEditText.getText().toString();

        // Reset count for this checkout
        count = 0;
        adequateStock = true;
        done = false;

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
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            DocumentSnapshot document = task.getResult();
                            Object lastOrderIdObj = document.get("lastOrderId");
                            Object countOfOrderedItemsObj = document.get("countOfOrderedItems");
                            Object priceOfOrdersObj = document.get("priceOfOrders");

                            prevOrderId[0] = lastOrderIdObj != null ? (int) (long) lastOrderIdObj : 0;
                            countOfOrderedItems[0] = countOfOrderedItemsObj != null ? (int) (long) countOfOrderedItemsObj : 0;
                            priceOfOrders[0] = priceOfOrdersObj != null ? (int) (long) priceOfOrdersObj : 0;
                        } else {
                            // If document doesn't exist or task failed, use default values
                            prevOrderId[0] = 0;
                            countOfOrderedItems[0] = 0;
                            priceOfOrders[0] = 0;
                        }
                    }
                });

        FirebaseUtil.getCartItems()
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Add null checks for all document fields
                                Object nameObj = document.get("name");
                                Object imageObj = document.get("image");
                                Object productIdObj = document.get("productId");
                                Object priceObj = document.get("price");
                                Object quantityObj = document.get("quantity");

                                // Skip this document if any required field is null, and delete the invalid item
                                if (nameObj == null || imageObj == null || productIdObj == null || priceObj == null || quantityObj == null) {
                                    Log.e("CheckoutActivity", "Deleting invalid cart item with null fields: " + document.getId());
                                    // Delete the invalid cart item
                                    FirebaseUtil.getCartItems().document(document.getId()).delete()
                                            .addOnSuccessListener(aVoid -> Log.i("CheckoutActivity", "Deleted invalid cart item: " + document.getId()))
                                            .addOnFailureListener(e -> Log.e("CheckoutActivity", "Failed to delete invalid cart item: " + document.getId(), e));
                                    continue;
                                }

                                count++;
                                cartDocument[0].add(document.getId());
                                productName[0].add(nameObj.toString());
                                productPrice[0].add((int) (long) priceObj);
                                productQuantity[0].add((int) (long) quantityObj);

                                OrderItemModel item = new OrderItemModel(prevOrderId[0] + 1, (int) (long) productIdObj, nameObj.toString(), imageObj.toString(),
                                        (int) (long) priceObj, (int) (long) quantityObj, Timestamp.now(), name, email, phone, address, comment);

                                // Create order with error handling
                                FirebaseFirestore.getInstance().collection("orders").document(FirebaseAuth.getInstance().getUid()).collection("items").add(item)
                                        .addOnSuccessListener(documentReference -> {
                                            Log.i("CheckoutActivity", "Order item created successfully: " + documentReference.getId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("CheckoutActivity", "Failed to create order item", e);
                                        });
                                int quantity = (int) (long) quantityObj;

                                callback.onCallback(document, quantity);

                            }

                            // Check if we have any items to process
                            if (count == 0) {
                                Log.e("CheckoutActivity", "No cart items found to process");
                                dialog.dismiss();
                                new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Order Failed!")
                                        .setContentText("Your cart is empty. Please add items to your cart before checkout.")
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                finish();
                                            }
                                        }).show();
                                return;
                            }

                            Log.i("check0", "Processing " + count + " items, done: " + done);

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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}