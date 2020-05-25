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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

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
import com.malcolmmaima.dishi.Model.ReceiptModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ReceiptItemAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.fabric.sdk.android.services.common.SafeToast;

public class ReceiptActivity extends AppCompatActivity {

    String TAG = "ReceiptActivity";
    DatabaseReference receiptItemsRef, receiptObjRef, myRef;
    String key, myPhone, orderid, orderedOn, deliveredOn, restaurantName, restaurantPhone;
    FirebaseUser user;
    FirebaseAuth mAuth;
    private ArrayList<ProductDetailsModel> deliveredItems;
    private RecyclerView recyclerView;
    private ReceiptItemAdapter mAdapter;
    ImageView exitReceipt, receiptOptions;
    MyTextView_Roboto_Regular totalTitle, orderID;
    MyTextView_Roboto_Medium totalBill, dateOrdered, dateDelivered, vendorName, vendorPhone;
    int totalAmount;
    Double vatCharge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber();
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if(locked == true){
                                    Intent slideactivity = new Intent(ReceiptActivity.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            loadReceipt();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber();
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if(locked == true){
                                    Intent slideactivity = new Intent(ReceiptActivity.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            loadReceipt();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                            if(locked == true){
                                Intent slideactivity = new Intent(ReceiptActivity.this, SecurityPin.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                slideactivity.putExtra("pinType", "resume");
                                startActivity(slideactivity);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadReceipt() {
        setContentView(R.layout.activity_receipt);

        exitReceipt = findViewById(R.id.exitReceipt);
        recyclerView = findViewById(R.id.recyclerview);
        totalBill = findViewById(R.id.totalBill);
        totalTitle = findViewById(R.id.totalTitle);
        receiptOptions = findViewById(R.id.receiptOptions);
        orderID = findViewById(R.id.orderID);
        dateOrdered = findViewById(R.id.dateOrdered);
        dateDelivered = findViewById(R.id.dateDelivered);
        vendorName = findViewById(R.id.restaurantName);
        vendorPhone = findViewById(R.id.restaurantPhone);

        orderedOn = getIntent().getStringExtra("orderOn");
        deliveredOn = getIntent().getStringExtra("deliveredOn");
        key = getIntent().getStringExtra("key");

        restaurantPhone = getIntent().getStringExtra("restaurantPhone");
        vendorPhone.setText(restaurantPhone);

        restaurantName = getIntent().getStringExtra("restaurantName");
        vendorName.setText(restaurantName);


        receiptObjRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone+"/"+key);
        receiptObjRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    finish();
                    SafeToast.makeText(ReceiptActivity.this, "Receipt does not exist!", Toast.LENGTH_LONG).show();
                } else {
                    //meaning null, from notification pending intent
                    if(restaurantName.equals("vendorName")){
                        DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+restaurantPhone);
                        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                try {
                                    UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                    vendorName.setText(userModel.getFirstname() + " " + userModel.getLastname());
                                } catch (Exception e){
                                    Log.e(TAG, "onDataChange: ", e);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    vendorPhone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final AlertDialog callAlert = new AlertDialog.Builder(ReceiptActivity.this)
                                    //set message, title, and icon
                                    .setMessage("Call " + restaurantName + "?")
                                    //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                    //set three option buttons
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            String phone = restaurantPhone;
                                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                            startActivity(intent);
                                        }
                                    }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            //do nothing

                                        }
                                    })//setNegativeButton

                                    .create();
                            callAlert.show();
                        }
                    });

                    orderid = getIntent().getStringExtra("orderID");
                    orderID.setText("Order ID: #"+orderid);

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

                    //get the ordered items
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

                                    totalAmount = totalAmount + (Integer.parseInt(product.getPrice())*product.getQuantity());
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

                    //mark as seen
                    receiptObjRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone+"/"+key);
                    receiptObjRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            try {
                                ReceiptModel newReceipt = dataSnapshot.getValue(ReceiptModel.class);
                                if (newReceipt.getSeen() == false) {
                                    receiptObjRef.child("seen").setValue(true);
                                }
                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
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
                                            receiptObjRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    finish();
                                                    SafeToast.makeText(ReceiptActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                                }
                                            });
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
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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

