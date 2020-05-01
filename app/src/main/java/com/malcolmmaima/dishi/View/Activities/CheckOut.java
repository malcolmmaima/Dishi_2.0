package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Utils.GenerateRandomString;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Controller.TrackingService;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Maps.SearchLocation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.fabric.sdk.android.services.common.SafeToast;

public class CheckOut extends AppCompatActivity {

    List<ProductDetailsModel> list;
    AppCompatButton orderBtn;
    CardView PaymentMethod, DeliveryAddress;
    EditText remarks;
    TextView SubTotal,deliveryChargeAmount, VATamount, totalBill;
    Double deliveryAmount, totalBillAmount,VAT;
    String [] paymentMethods = {"M-Pesa","Cash on Delivery"};
    String [] deliveryAddress = {"Live Location","Select Location"};
    String selectedPaymentMethod, myPhone;
    Double lat, lng;
    String placeName, locationSet;
    AppCompatImageView paymentStatus, deliveryLocationStatus;
    DatabaseReference myRef, myCartRef;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_out);

        initWidgets();
        selectedPaymentMethod = "";
        deliveryAmount = 0.0;
        totalBillAmount = 0.0;
        lat = 0.0;
        lng = 0.0;
        placeName = "";
        locationSet = "";
        progressDialog = new ProgressDialog(CheckOut.this);

        myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //Initialize some values
        int subTotalAmount = getIntent().getIntExtra("subTotal", 0);

        totalBillAmount = Double.valueOf(subTotalAmount);
        SubTotal.setText("Ksh " + subTotalAmount);
        deliveryChargeAmount.setText("Ksh " + deliveryAmount);

        DecimalFormat df = new DecimalFormat("#"); //#.##
        VAT = 0.16 * totalBillAmount; //16% VAT : Kenya
        VAT = Double.valueOf(df.format(VAT));
        VATamount.setText("Ksh " + VAT);

        totalBillAmount = subTotalAmount + deliveryAmount; //+ VAT
        totalBill.setText("Ksh " + totalBillAmount);

        setTitle("Checkout");

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        PaymentMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(CheckOut.this);
                builder.setItems(paymentMethods, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            selectedPaymentMethod = ""; //will set to "mpesa" once implemented Mpesa
                            Snackbar.make(v.getRootView(), "In development", Snackbar.LENGTH_LONG).show();

                            //Lets set to grey for now since we have not yet implemented Mpesa
                            paymentStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                        if(which == 1){
                            selectedPaymentMethod = "cash";
                            paymentStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                    }
                });
                builder.create();
                builder.show();
            }
        });

        DeliveryAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(CheckOut.this);
                builder.setItems(deliveryAddress, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            locationSet = "live";
                            startTrackerService();
                            deliveryLocationStatus.setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                        if(which == 1){
                            requestLocation();
                        }
                    }
                });
                builder.create();
                builder.show();
            }
        });

        /**
         * Complete order
         */
        orderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //We need to perform a validation check before sending the order
                if(!selectedPaymentMethod.equals("") && !locationSet.equals("")){
                    sendOrder();
                }

                else{
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout),
                            "Set Address and Payment method", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        });
    }

    private void sendOrder() {

        progressDialog.setMessage("Sending...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        //Get current date
        GetCurrentDate currentDate = new GetCurrentDate();
        final String orderDate = currentDate.getDate();

        //for static location preference
        final StaticLocationModel staticLocationModel = new StaticLocationModel();

        /**
         * Loop through my cart items and add to list Array before passing to adapter
         */
        myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list = new ArrayList<>();
                String myRemarks = "";
                if(remarks.getText().toString().equals("")){
                    myRemarks = "none";
                }

                else {
                    myRemarks = remarks.getText().toString().trim();
                }

                for(DataSnapshot cart : dataSnapshot.getChildren()){
                    final ProductDetailsModel product = cart.getValue(ProductDetailsModel.class);

                    product.setPaymentMethod(selectedPaymentMethod);
                    product.setAddress(locationSet);
                    product.setDistance(null);
                    product.setUploadDate(orderDate);
                    product.setConfirmed(false);
                    list.add(product);

                    DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders/"+product.getOwner());

                    //Generate a random String
                    GenerateRandomString randomString = new GenerateRandomString();
                    String orderID_1 = randomString.getAlphaNumericString(3);

                    //Generate random integer
                    int orderID_2 = new Random().nextInt(1000);
                    String orderID = orderID_1.toUpperCase()+""+orderID_2;

                    ordersRef.child(myPhone).child("initiatedOn").setValue(orderDate);
                    ordersRef.child(myPhone).child("orderID").setValue(orderID);
                    ordersRef.child(myPhone).child("items").child(cart.getKey()).setValue(product);
                    ordersRef.child(myPhone).child("completed").setValue(false);
                    ordersRef.child(myPhone).child("remarks").setValue(myRemarks);

                    if(locationSet.equals("static")){
                        staticLocationModel.setLatitude(lat);
                        staticLocationModel.setLongitude(lng);
                        staticLocationModel.setPlace(placeName);

                        ordersRef.child(myPhone).child("static_address").setValue(staticLocationModel);
                    }

                    //Loop has reached the end
                    if(dataSnapshot.getChildrenCount() == list.size()){
                        //Clear my cart then exit
                        myCartRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                //We need to have a node that keeps track of our active orders to the different restaurants
                                DatabaseReference myOrders = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone);

                                //Post restaurant phone numbers which act as our primary key, keep track of our active orders
                                for(int i = 0; i<list.size(); i++){
                                    myOrders.child(list.get(i).getOwner()).setValue("active");

                                    if(i == list.size()-1){
                                        progressDialog.dismiss();
                                        finish();
                                        SafeToast.makeText(CheckOut.this, "Order sent!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                progressDialog.dismiss();
                                Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout),
                                        "Something went wrong", Snackbar.LENGTH_LONG);
                                snackbar.show();

                                if(snackbar.getDuration() == 3000){
                                    finish();
                                }
                            }
                        });

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    /**
     * Listen to the SearchLocation activity for LatLng values sent back
     */
    private static final int REQUEST_GET_MAP_LOCATION = 0;
    void requestLocation() {
        startActivityForResult(new Intent(CheckOut.this, SearchLocation.class), REQUEST_GET_MAP_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GET_MAP_LOCATION && resultCode == Activity.RESULT_OK) {
            Double latitude = data.getDoubleExtra("latitude", 0.0);
            Double longitude = data.getDoubleExtra("longitude", 0.0);

            lat = latitude;
            lng = longitude;
            placeName = data.getStringExtra("place");

            deliveryLocationStatus
                    .setColorFilter(ContextCompat.getColor(CheckOut.this, R.color.colorPrimary),
                            android.graphics.PorterDuff.Mode.SRC_IN);

            locationSet = "static"; //after location coordinates have been returned by the searchlocation module, set this value to static

        }
    }

    private void startTrackerService() {
        startService(new Intent(this, TrackingService.class));
        //Notify the user that tracking has been enabled//

        //SafeToast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show();

        //////////////////////////////////
    }

    private void initWidgets() {
        orderBtn = findViewById(R.id.btn_order);
        PaymentMethod = findViewById(R.id.PaymentMethod);
        DeliveryAddress = findViewById(R.id.DeliveryAddress);
        remarks = findViewById(R.id.remarks);

        SubTotal = findViewById(R.id.subTotal);
        deliveryChargeAmount = findViewById(R.id.deliveryChargeAmount);
        VATamount = findViewById(R.id.VATamount);
        totalBill = findViewById(R.id.totalBill);

        paymentStatus = findViewById(R.id.paymentStatus);
        deliveryLocationStatus = findViewById(R.id.deliveryLocationStatus);
    }
}
