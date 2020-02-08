package com.malcolmmaima.dishi.View.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.widget.TextViewCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.malcolmmaima.dishi.R;

public class CheckOut extends AppCompatActivity {

    AppCompatButton orderBtn;
    CardView PaymentMethod, DeliveryAddress;
    EditText remarks;
    TextView SubTotal,deliveryChargeAmount, VATamount, totalBill;
    Double deliveryAmount, totalBillAmount,VAT;
    String [] paymentMethods = {"M-Pesa","Cash on Delivery"};
    String selectedPaymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_out);

        initWidgets();
        selectedPaymentMethod = "";
        deliveryAmount = 0.0;
        totalBillAmount = 0.0;

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
                        }
                        if(which == 1){
                            selectedPaymentMethod = "cash";
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
                Snackbar.make(v.getRootView(), "Clicked", Snackbar.LENGTH_LONG).show();
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

    private void initWidgets() {
        orderBtn = findViewById(R.id.btn_order);
        PaymentMethod = findViewById(R.id.PaymentMethod);
        DeliveryAddress = findViewById(R.id.DeliveryAddress);
        remarks = findViewById(R.id.remarks);

        SubTotal = findViewById(R.id.subTotal);
        deliveryChargeAmount = findViewById(R.id.deliveryChargeAmount);
        VATamount = findViewById(R.id.VATamount);
        totalBill = findViewById(R.id.totalBill);
    }
}
