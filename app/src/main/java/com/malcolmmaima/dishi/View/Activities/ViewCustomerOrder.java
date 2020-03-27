package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.malcolmmaima.dishi.Controller.TrackingService;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.CartAdapter;
import com.malcolmmaima.dishi.View.Adapter.ViewOrderAdapter;
import com.malcolmmaima.dishi.View.Maps.GeoTracking;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewCustomerOrder extends AppCompatActivity implements OnOrderChecked {

    List<ProductDetails> list;
    String myPhone, phone, customerName, restaurantPhone;
    FirebaseUser user;
    DatabaseReference riderRequests, customerOrderItems, myLocationRef, myRidersRef, riderStatus;
    ValueEventListener customerOrderItemsListener, myRidersListener, currentRiderListener, riderStatusListener;
    TextView subTotal, deliveryChargeAmount, payment, totalBill, customerRemarks, riderName, restaurantName;
    FloatingActionButton acceptOrd;
    ImageView riderIcon;
    Double deliveryCharge, totalAmount;
    RecyclerView recyclerview;
    AppCompatButton confirmOrder, declineOrder;
    CardView DeliveryAddress, OrderStatus;
    LiveLocation liveLocation;
    StaticLocation deliveryLocation;
    ValueEventListener locationListener;
    Menu myMenu;
    List<String> myRiders, ridersName;
    String accType, restaurantname, restaurantProfile;
    CircleImageView profilePic;
    UserModel riderUser;
    String riderPhone;
    String [] restaurantActions = {"View","Message", "Call"};

    final int[] total = {0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_customer_order);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //Initialize variables
        deliveryCharge = 0.0;
        totalAmount = 0.0;

        //Data passed from adapter
        phone = getIntent().getStringExtra("phone"); //From adapters
        customerName = getIntent().getStringExtra("name");
        restaurantPhone = getIntent().getStringExtra("restaurantPhone");
        accType = getIntent().getStringExtra("accountType");
        restaurantname = getIntent().getStringExtra("restaurantName");
        restaurantProfile = getIntent().getStringExtra("restaurantProfile");

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
        customerRemarks = findViewById(R.id.customerRemarks);
        riderName = findViewById(R.id.riderName);
        riderIcon = findViewById(R.id.riderIcon);
        profilePic = findViewById(R.id.profilePic);
        OrderStatus = findViewById(R.id.card_order_status);
        acceptOrd = findViewById(R.id.confirmOrd);
        acceptOrd.setTag("accept");
        restaurantName = findViewById(R.id.restaurantName);

        restaurantName.setText(restaurantname);

        /**
         * Load image url onto imageview
         */
        try {
            //Load retaurant image
            Picasso.with(ViewCustomerOrder.this).load(restaurantProfile).fit().centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profilePic);
        } catch (Exception e){

        }

        restaurantName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ViewCustomerOrder.this);
                builder.setTitle(restaurantname);
                builder.setItems(restaurantActions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        if(which == 0){
                            Intent slideactivity = new Intent(ViewCustomerOrder.this, ViewRestaurant.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            slideactivity.putExtra("restaurant_phone", restaurantPhone);
                            slideactivity.putExtra("distance", 0.0);
                            slideactivity.putExtra("profilePic", restaurantProfile);
                            Bundle bndlanimation =
                                    null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                bndlanimation = ActivityOptions.makeCustomAnimation(ViewCustomerOrder.this, R.anim.animation,R.anim.animation2).toBundle();
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                startActivity(slideactivity, bndlanimation);
                            }
                        }
                        if(which == 1){
                            Snackbar snackbar = Snackbar.make(v.getRootView(), "In development", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }

                        if(which == 2){
                            final AlertDialog callAlert = new AlertDialog.Builder(v.getContext())
                                    //set message, title, and icon
                                    .setMessage("Call " + restaurantname + "?")
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

                    }
                });
                builder.create();
                builder.show();
            }
        });

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(ViewCustomerOrder.this, ViewImage.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("imageURL", restaurantProfile);
                startActivity(slideactivity);
            }
        });

        if(accType.equals("2")){
            OrderStatus.setVisibility(View.GONE);
            myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
            customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+myPhone+"/"+phone);
            myRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+myPhone);
        }

        if(accType.equals("3")){
            OrderStatus.setVisibility(View.VISIBLE);
            myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
            customerOrderItems = FirebaseDatabase.getInstance().getReference("orders/"+restaurantPhone+"/"+phone);
            myRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+restaurantPhone);
        }

        /**
         * On create view fetch my location coordinates
         */

        try {
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
                    } catch (Exception e) {

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            myLocationRef.addListenerForSingleValueEvent(locationListener);
        } catch (Exception e){

        }


        /**
         * Initialize cart items total and keep track of items
         */
        customerOrderItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()){
                    finish();
                    Toast.makeText(ViewCustomerOrder.this, "Order Complete!", Toast.LENGTH_LONG).show();
                }

                total[0] = 0;

                list = new ArrayList<>();

                String remarks = dataSnapshot.child("remarks").getValue(String.class);
                try {
                    riderPhone = dataSnapshot.child("rider").getValue(String.class);
                } catch (Exception e){

                }

                customerRemarks.setText("Remarks: "+remarks);
                for(DataSnapshot items : dataSnapshot.child("items").getChildren()){

                    try {
                        ProductDetails prod = items.getValue(ProductDetails.class);
                        prod.setKey(items.getKey());
                        list.add(prod);

                        if(prod.getConfirmed() == true){
                            int adapterTotal = prod.getQuantity() * Integer.parseInt(prod.getPrice());
                            total[0] = total[0] + adapterTotal;
                            totalAmount = total[0] + deliveryCharge;
                        }

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

        /**
         * Fetch my riders list
         */
        myRidersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myRiders = new ArrayList<String>();
                ridersName = new ArrayList<String>();

                for(final DataSnapshot rider : dataSnapshot.getChildren()){
                    DatabaseReference riderUserInfo = FirebaseDatabase.getInstance().getReference("users/"+rider.getKey());
                    riderUserInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            riderUser = dataSnapshot.getValue(UserModel.class);

                            myRiders.add(rider.getKey());
                            ridersName.add(riderUser.getFirstname() + " " + riderUser.getLastname());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                try {
                    Collections.reverse(myRiders);
                    Collections.reverse(ridersName);
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRidersRef.addValueEventListener(myRidersListener);

        /**
         * fetch current rider
         */
        currentRiderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    riderName.setText("You are the default rider for this order. Assign rider top right icon.");
                }

                else {
                    final DatabaseReference riderUserInfo = FirebaseDatabase.getInstance().getReference("users/"+dataSnapshot.getValue());
                    riderUserInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            riderUser = dataSnapshot.getValue(UserModel.class);
                            riderName.setText(riderUser.getFirstname()+" "+riderUser.getLastname());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    if(accType.equals("2")){
                        try {
                            //Toast.makeText(ViewCustomerOrder.this, "rider: "+ riderPhone, Toast.LENGTH_LONG).show();
                            riderStatus = FirebaseDatabase.getInstance().getReference("my_ride_requests/" + riderPhone + "/" + myPhone + "/" + phone);
                        } catch (Exception e){

                        }
                    }

                    if(accType.equals("3")){
                        riderStatus = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myPhone+"/"+restaurantPhone+"/"+phone);

                        /**
                         * Close this activity for rider if restaurant has assigned the order to a different rider
                         */
                        if(!myPhone.equals(dataSnapshot.getValue())){
                            finish();
                            Toast.makeText(ViewCustomerOrder.this, "Order assigned to different rider!", Toast.LENGTH_LONG).show();
                        }
                    }

                    riderStatusListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dtSnapshot) {
                            String riderOrderStatus = dtSnapshot.getValue(String.class);
                            if(riderPhone == null){
                                Snackbar.make(findViewById(R.id.parentlayout), "Error, rider does not exist!", Snackbar.LENGTH_LONG).show();
                                customerOrderItems.child("rider").removeValue();
                            }

                            try {
                                if (riderOrderStatus.equals("accepted")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewCustomerOrder.this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                                    acceptOrd.setTag("decline");
                                    acceptOrd.setImageResource(R.drawable.ic_clear_white_36dp);
                                    confirmOrder.setVisibility(View.VISIBLE);
                                }

                                if (riderOrderStatus.equals("assigned")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewCustomerOrder.this, R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
                                    acceptOrd.setTag("accept");
                                    acceptOrd.setImageResource(R.drawable.ic_action_save);
                                    confirmOrder.setVisibility(View.GONE);
                                }

                                if (riderOrderStatus.equals("declined")) {
                                    riderIcon.setColorFilter(ContextCompat.getColor(ViewCustomerOrder.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
                                    acceptOrd.setTag("accept");
                                    acceptOrd.setImageResource(R.drawable.ic_action_save);
                                    confirmOrder.setVisibility(View.GONE);
                                }

                                if(accType.equals("2")){
                                    confirmOrder.setVisibility(View.VISIBLE);
                                }
                            } catch (Exception e){

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };
                    try {
                        riderStatus.addValueEventListener(riderStatusListener);
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        customerOrderItems.child("rider").addValueEventListener(currentRiderListener);

        acceptOrd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(acceptOrd.getTag().equals("accept")){

                    final AlertDialog accept = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Accept ride order request?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    riderStatus.setValue("accepted");
                                    myRidersRef.child(myPhone).setValue("active").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            acceptOrd.setTag("decline");
                                            acceptOrd.setImageResource(R.drawable.ic_clear_white_36dp);
                                        }
                                    });

                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })

                            .create();
                    accept.show();

                }

                if(acceptOrd.getTag().equals("decline")){

                    final AlertDialog decline = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Decline ride order request?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    riderStatus.setValue("declined");
                                    myRidersRef.child(myPhone).setValue("inactive").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            acceptOrd.setTag("accept");
                                            acceptOrd.setImageResource(R.drawable.ic_action_save);
                                        }
                                    });

                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })

                            .create();
                    decline.show();

                }

            }
        });

        DeliveryAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(ViewCustomerOrder.this, GeoTracking.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if(accType.equals("2")){
                    slideactivity.putExtra("restaurantPhone", myPhone);
                    slideactivity.putExtra("customerPhone", phone);
                }

                if(accType.equals("3")){
                    slideactivity.putExtra("restaurantPhone", restaurantPhone);
                    slideactivity.putExtra("customerPhone", phone);
                }

                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(ViewCustomerOrder.this, R.anim.animation,R.anim.animation2).toBundle();
                startActivity(slideactivity, bndlanimation);

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
                    AlertDialog finish = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Order Delivered?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    customerOrderItems.child("completed").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Snackbar.make(v.getRootView(), "Awaiting customer confirmation", Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                }
                            })

                            .create();
                    finish.show();

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
            total[0] = total[0] + adapterTotal;
            totalAmount = total[0] + deliveryCharge;
            subTotal.setText("Ksh " + total[0]);
            totalBill.setText("ksh " + totalAmount);

            if(confirmOrder.getTag().toString().equals("end")){
                confirmOrder.setTag("confirm");
                confirmOrder.setText("CONFIRM");
            }
        }

        else {
            total[0] = total[0] - adapterTotal;
            if(total[0] < 0) { total[0] = 0;}
            totalAmount = total[0] + deliveryCharge;
            subTotal.setText("Ksh " + total[0]);
            totalBill.setText("ksh " + totalAmount);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.assign_order_menu, menu);
        myMenu = menu;
        MenuItem item = menu.findItem(R.id.addRider);
        if (item != null) {
            item.setVisible(true);
        }

        else {
            item.setVisible(false);
        }

        if(accType.equals("2")){
            item.setVisible(true);
        }

        if(accType.equals("3")){
            item.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) { switch(item.getItemId()) {

        case R.id.callCustomer:

            final AlertDialog callAlert = new AlertDialog.Builder(this)
                    //set message, title, and icon
                    .setMessage("Call " + customerName + "?")
                    //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                    //set three option buttons
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String phone_ = phone;
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone_, null));
                            startActivity(intent);
                        }
                    }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //do nothing

                        }
                    })//setNegativeButton

                    .create();
            callAlert.show();
            return (true);

        case R.id.addRider:

            /**
             * Hide / show icon if atleast one order item confirmed
             */
            for(int i=0; i<list.size(); i++){
                //Loop through the order items, if atleast one order item is confirmed then allow assigning of order to rider
                if(list.get(i).getConfirmed() == true){
                    i = list.size(); //end loop
                    AlertDialog assignRider = new AlertDialog.Builder(ViewCustomerOrder.this)
                            .setMessage("Assign order to rider?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, final int whichButton) {
                                    customerOrderItems.child("rider").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(!dataSnapshot.exists()){
                                                String [] riders = new String[ridersName.size()];
                                                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ViewCustomerOrder.this);
                                                builder.setItems(ridersName.toArray(riders), new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, final int which) {
                                                        customerOrderItems.child("rider").setValue(myRiders.get(which)).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {


                                                                myRidersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                        //Remove existing ride requests then set new request
                                                                        for(DataSnapshot riders : dataSnapshot.getChildren()){
                                                                            riderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+riders.getKey());
                                                                            riderRequests.child(myPhone).child(phone).removeValue();
                                                                        }

                                                                        riderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myRiders.get(which));
                                                                        riderRequests.child(myPhone).child(phone).setValue("assigned");
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                    }
                                                                });
                                                            }
                                                        });
                                                    }
                                                });
                                                builder.create();
                                                builder.show();

                                            }

                                            else {
                                                //Change rider
                                                AlertDialog changeRider = new AlertDialog.Builder(ViewCustomerOrder.this)
                                                        .setTitle("Already exists")
                                                        .setMessage("Change rider?")
                                                        //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                                                        .setCancelable(false)
                                                        //set three option buttons
                                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                                //Load riders list
                                                                String [] riders = new String[ridersName.size()];
                                                                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ViewCustomerOrder.this);
                                                                builder.setItems(ridersName.toArray(riders), new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, final int which) {
                                                                        /**
                                                                         * Logic here is to assign the rider number to the order, then send rider request to rider awaiting confirmation
                                                                         */
                                                                        customerOrderItems.child("rider").setValue(myRiders.get(which)).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid) {


                                                                                myRidersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                        //Remove existing ride requests then set new request
                                                                                        for(DataSnapshot riders : dataSnapshot.getChildren()){
                                                                                            /**
                                                                                             * Delete any existing rider request for this particular order by looping through my riders,
                                                                                             * getting their primary key (phone) then deleting from 'my_rider_requests'
                                                                                             */
                                                                                            riderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+riders.getKey());
                                                                                            riderRequests.child(myPhone).child(phone).removeValue();
                                                                                        }

                                                                                        /**
                                                                                         * then send fresh rider order request, this is to avoid assigning a single order to
                                                                                         * multiple riders. Preventing duplicates
                                                                                         */
                                                                                        riderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+myRiders.get(which));
                                                                                        riderRequests.child(myPhone).child(phone).setValue("assigned");
                                                                                    }

                                                                                    @Override
                                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                    }
                                                                                });
                                                                            }
                                                                        });
                                                                    }
                                                                });
                                                                builder.create();
                                                                builder.show();
                                                            }
                                                        })//setPositiveButton

                                                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                //Do nothing
                                                            }
                                                        })

                                                        .create();
                                                changeRider.show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                    //Load riders list

                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                    Toast.makeText(ViewCustomerOrder.this, "Default rider is your account", Toast.LENGTH_LONG).show();
                                }
                            })

                            .create();
                    assignRider.show();
                }

                else {
                    //
                    Toast.makeText(this, "You must confirm at-least one item", Toast.LENGTH_LONG).show();
                }
            }

            return(true);
    }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            customerOrderItems.removeEventListener(customerOrderItemsListener);
            customerOrderItems.removeEventListener(currentRiderListener);
            myRidersRef.removeEventListener(myRidersListener);
            riderStatus.removeEventListener(riderStatusListener);
        } catch (Exception e){

        }
    }
}
