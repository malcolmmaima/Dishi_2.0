package com.malcolmmaima.dishi.View.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Maps.SearchLocation;

public class CheckOut extends AppCompatActivity {

    AppCompatButton orderBtn;
    CardView PaymentMethod, DeliveryAddress;
    EditText remarks;
    TextView SubTotal,deliveryChargeAmount, VATamount, totalBill;
    Double deliveryAmount, totalBillAmount,VAT;
    String [] paymentMethods = {"M-Pesa","Cash on Delivery"};
    String selectedPaymentMethod, myPhone;
    Double lat, lng;
    String placeName;
    AppCompatImageView paymentStatus, deliveryLocationStatus;
    DatabaseReference myRef, restaurantRef;


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

        myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //Initialize some values
        VAT = 0.16 * totalBillAmount;
        int subTotalAmount = getIntent().getIntExtra("subTotal", 0);
        SubTotal.setText("Ksh " + subTotalAmount);
        deliveryChargeAmount.setText("Ksh " + deliveryAmount);
        VATamount.setText("Ksh " + VAT);

        totalBillAmount = subTotalAmount + deliveryAmount + VAT;
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
                requestLocation();
            }
        });

        /**
         * Complete order
         */
        orderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v.getRootView(), "Clicked", Snackbar.LENGTH_LONG).show();
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

        }
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
