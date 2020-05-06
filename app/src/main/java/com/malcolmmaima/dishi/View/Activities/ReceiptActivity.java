package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ReceiptItemAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ReceiptActivity extends AppCompatActivity {

    String TAG = "ReceiptActivity";
    DatabaseReference receiptItemsRef;
    String key, myPhone, orderid, orderedOn, deliveredOn;
    FirebaseUser user;
    private ArrayList<ProductDetailsModel> deliveredItems;
    private RecyclerView recyclerView;
    private ReceiptItemAdapter mAdapter;
    ImageView exitReceipt, receiptOptions;
    MyTextView_Roboto_Regular totalTitle, orderID;
    MyTextView_Roboto_Medium totalBill, dateOrdered, dateDelivered;
    int totalAmount;
    Double vatCharge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        exitReceipt = findViewById(R.id.exitReceipt);
        recyclerView = findViewById(R.id.recyclerview);
        totalBill = findViewById(R.id.totalBill);
        totalTitle = findViewById(R.id.totalTitle);
        receiptOptions = findViewById(R.id.receiptOptions);
        orderID = findViewById(R.id.orderID);
        dateOrdered = findViewById(R.id.dateOrdered);
        dateDelivered = findViewById(R.id.dateDelivered);

        orderedOn = getIntent().getStringExtra("orderOn");
        deliveredOn = getIntent().getStringExtra("deliveredOn");
        key = getIntent().getStringExtra("key");

        orderid = getIntent().getStringExtra("orderID");
        orderID.setText("Order ID: #"+orderid);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber();

        String dtEnd = deliveredOn;
        String dtStart = orderedOn;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
        try {
            //Convert String date values to Date values
            Date dateStart;
            Date dateEnd;

            dateEnd = format.parse(dtEnd);
            dateStart = format.parse(dtStart);

            dateDelivered.setText(""+dateEnd);
            dateOrdered.setText(""+dateStart);

        } catch (ParseException e) {
            e.printStackTrace();
            Log.d(TAG, "timeStamp: "+ e.getMessage());
        }

        receiptItemsRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone+"/"+key+"/items");
        receiptItemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                deliveredItems = new ArrayList<>();
                totalAmount = 0;
                for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){
                    try {
                        ProductDetailsModel product = dataSnapshot1.getValue(ProductDetailsModel.class);
                        deliveredItems.add(product);

                        totalAmount = totalAmount + Integer.parseInt(product.getPrice());
                        totalBill.setText("Ksh " + totalAmount);
                        totalTitle.setText("" + totalAmount);
                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                }

                mAdapter = new ReceiptItemAdapter(ReceiptActivity.this,deliveredItems);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(ReceiptActivity.this);
                recyclerView.setLayoutManager(mLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setAdapter(mAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //creating a popup menu
        PopupMenu popup = new PopupMenu(ReceiptActivity.this, receiptOptions);
        //inflating menu from xml resource
        popup.inflate(R.menu.receipt_options_menu);

        Menu myMenu = popup.getMenu();
        MenuItem deleteOption = myMenu.findItem(R.id.delete);
        MenuItem downloadOption = myMenu.findItem(R.id.download);
        try {
            deleteOption.setVisible(true);
            downloadOption.setVisible(true);
        } catch (Exception e){}

        receiptOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.delete:
                                Snackbar.make(v.getRootView(), "In development", Snackbar.LENGTH_LONG).show();
                                //do something
                                return (true);
                            case R.id.download:
                                Snackbar.make(v.getRootView(), "In development", Snackbar.LENGTH_LONG).show();
                                //do something
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                //displaying the popup
                popup.show();
            }
        });
        exitReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

