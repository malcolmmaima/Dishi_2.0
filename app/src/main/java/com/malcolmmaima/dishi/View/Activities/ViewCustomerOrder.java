package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
import com.malcolmmaima.dishi.Controller.CalculateDistance;
import com.malcolmmaima.dishi.Controller.OnOrderChecked;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.CartAdapter;
import com.malcolmmaima.dishi.View.Adapter.ViewOrderAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewCustomerOrder extends AppCompatActivity implements OnOrderChecked {

    List<ProductDetails> list;
    String myPhone, phone, customerName;
    FirebaseUser user;
    DatabaseReference customerOrderItems, myLocationRef;
    ValueEventListener customerOrderItemsListener;
    TextView subTotal, deliveryChargeAmount, payment, totalBill;
    Double deliveryCharge, totalAmount;
    RecyclerView recyclerview;
    AppCompatButton confirmOrder, declineOrder;
    CardView DeliveryAddress;
    LiveLocation liveLocation;
    StaticLocation deliveryLocation;
    ValueEventListener locationListener;

    final int[] total = {0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_customer_order);

        //Initialize variables
        deliveryCharge = 0.0;
        totalAmount = 0.0;

        //Data passed from adapter
        phone = getIntent().getStringExtra("phone"); //From adapters
        customerName = getIntent().getStringExtra("name");

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
        customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+myPhone+"/"+phone);

        /**
         * On create view fetch my location coordinates
         */

        liveLocation = null;
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    liveLocation = dataSnapshot.getValue(LiveLocation.class);
                    //Toast.makeText(ViewCustomerOrder.this, "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();

                    /**
                     * Compute distance between customer and restaurant
                     */

                    CalculateDistance calculateDistance = new CalculateDistance();
                    Double dist = calculateDistance.distance(liveLocation.getLatitude(),
                            liveLocation.getLongitude(), deliveryLocation.getLatitude(), deliveryLocation.getLongitude(), "K");

                    Toast.makeText(ViewCustomerOrder.this, "Distance: " + dist, Toast.LENGTH_SHORT).show();
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myLocationRef.addListenerForSingleValueEvent(locationListener);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(customerName);

        subTotal = findViewById(R.id.subTotal);
        deliveryChargeAmount = findViewById(R.id.deliveryChargeAmount);
        payment = findViewById(R.id.payment);
        totalBill = findViewById(R.id.totalBill);
        recyclerview = findViewById(R.id.rview);
        confirmOrder = findViewById(R.id.btn_confirm);
        confirmOrder.setTag("confirm");
        declineOrder = findViewById(R.id.btn_decline);
        declineOrder.setTag("active");
        DeliveryAddress = findViewById(R.id.DeliveryAddress);

        /**
         * Initialize cart items total and keep track of items
         */
        customerOrderItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                total[0] = 0;

                list = new ArrayList<>();
                for(DataSnapshot items : dataSnapshot.child("items").getChildren()){

                    try {
                        ProductDetails prod = items.getValue(ProductDetails.class);
                        prod.setKey(items.getKey());
                        list.add(prod);
                        int adapterTotal = prod.getQuantity() * Integer.parseInt(prod.getPrice());
                        total[0] = total[0] + adapterTotal;
                        totalAmount = total[0] + deliveryCharge;
                        payment.setText(prod.getPaymentMethod()); //We just need to capture the payment method from one of the items
                    } catch (Exception e){

                    }
                }
                subTotal.setText("Ksh "+total[0]);

                if(!list.isEmpty()){
                    Collections.reverse(list);
                    ViewOrderAdapter recycler = new ViewOrderAdapter(ViewCustomerOrder.this, list, ViewCustomerOrder.this);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewCustomerOrder.this);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());

                    recycler.notifyDataSetChanged();

                    recyclerview.getItemAnimator().setAddDuration(200);
                    recyclerview.getItemAnimator().setRemoveDuration(200);
                    recyclerview.getItemAnimator().setMoveDuration(200);
                    recyclerview.getItemAnimator().setChangeDuration(200);
                    recyclerview.setAdapter(recycler);


                }

                totalBill.setText("ksh " + totalAmount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        customerOrderItems.addValueEventListener(customerOrderItemsListener);

        DeliveryAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * On create view fetch customer location coordinates
                 */
                customerOrderItemsListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        /**
                         * If this node exists then customer has set delivery address as static
                         */
                        if(dataSnapshot.child("static_address").exists()){
                            try {
                                deliveryLocation = dataSnapshot.child("static_address").getValue(StaticLocation.class);
                                Toast.makeText(ViewCustomerOrder.this, "loc: " + deliveryLocation.getPlace(), Toast.LENGTH_SHORT).show();

                            } catch (Exception e){

                            }
                        }
                        /**
                         * Node doen't exist which automatically means customer set "live" as delivery address
                         */
                        else {
                            Toast.makeText(ViewCustomerOrder.this, "loc doesn't exist, use live", Toast.LENGTH_SHORT).show();
                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };

                customerOrderItems.addListenerForSingleValueEvent(customerOrderItemsListener);

            }
        });

        customerOrderItems.child("items").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot items : dataSnapshot.getChildren()){
                    Boolean confirmed = items.child("confirmed").getValue(Boolean.class);
                    if(confirmed){
                        confirmOrder.setTag("end");
                        confirmOrder.setText("END");
                        declineOrder.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        confirmOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                /**
                 * Delivered order to address, notify customer
                 */
                if(confirmOrder.getTag().toString().equals("end")){
                    Toast.makeText(ViewCustomerOrder.this, "Notify customer order arrived", Toast.LENGTH_SHORT).show();
                }
                /**
                 * Accept order
                 */
                else {
                    AlertDialog confirmOrd = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Accept " + customerName + "'s order?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    int j = 0;
                                    //set product item status to confirmed
                                    for(int i = 0; i<list.size(); i++){
                                        if(list.get(i).getConfirmed() == true){
                                            j++;
                                            customerOrderItems.child("items").child(list.get(i).getKey()).child("confirmed").setValue(true);
                                        }

                                        if(i==list.size()-1){ //loop has reached the end

                                            if(j==0){
                                                Snackbar.make(v.getRootView(), "You must select available order items", Snackbar.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                }
                            })

                            .create();
                    confirmOrd.show();
                }


            }
        });

        declineOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if(declineOrder.getTag().equals("declined")){
                    Toast.makeText(ViewCustomerOrder.this, "Delete order", Toast.LENGTH_SHORT).show();
                }

                else {
                    AlertDialog confirmOrd = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Decline " + customerName + "'s order?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    int j = 0;
                                    //set product item status to confirmed
                                    for(int i = 0; i<list.size(); i++){
                                        customerOrderItems.child("items").child(list.get(i).getKey()).child("confirmed").setValue(false);

                                        if(i==list.size()-1){ //loop has reached the end
                                            confirmOrder.setVisibility(View.GONE);
                                            declineOrder.setTag("declined");
                                            declineOrder.setText("DELETE");
                                            Snackbar.make(v.getRootView(), "Order declined", Snackbar.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                }
                            })

                            .create();
                    confirmOrd.show();
                }

            }
        });

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });
        //topToolBar.setLogo(R.drawable.logo);
        //topToolBar.setLogoDescription(getResources().getString(R.string.logo_desc));


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        customerOrderItems.removeEventListener(customerOrderItemsListener);
    }

    /**
     * Initialize tracking of total for selected orders
     */
    int total_ = 0;
    @Override
    public void onItemChecked(Boolean isChecked, int position, String price, int quantity) {

        list.get(position).setConfirmed(isChecked);

        /**
         * Compute adapter price per quantity
         */
        int adapterPrice = Integer.parseInt(price.trim());
        int adapterTotal = adapterPrice * quantity;

        /**
         * Add or subtract price of selected/unselected product
         */
        if(isChecked){
            total_ = total_ + adapterTotal;
            totalAmount = total_ + deliveryCharge;
            subTotal.setText("Ksh " + total_);
            totalBill.setText("ksh " + totalAmount);

            if(confirmOrder.getTag().toString().equals("end")){
                confirmOrder.setTag("confirm");
                confirmOrder.setText("CONFIRM");
            }
        }

        else {
            total_ = total_ - adapterTotal;
            if(total_ < 0) { total_ = 0;}
            totalAmount = total_ + deliveryCharge;
            subTotal.setText("Ksh " + total_);
            totalBill.setText("ksh " + totalAmount);
        }
    }
}
