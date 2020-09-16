package com.malcolmmaima.dishi.View.Activities;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.CartAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;



public class MyCart extends AppCompatActivity {

    String TAG = "MyCartActivity";
    List<ProductDetailsModel> list;
    RecyclerView recyclerview;
    String myPhone, restaurantName;
    TextView emptyTag, totalPrice, totalItems;
    AppCompatImageView icon;
    LiveLocationModel liveLocationModel;
    Button checkoutBtn;
    Boolean multipleRestaurants, dialogShow;
    DatabaseReference myCartRef, myLocationRef, myRef;
    FirebaseDatabase db;
    ValueEventListener locationListener, cartListener, totalItemsListener;
    FirebaseUser user;
    int itemCount;
    FirebaseAuth mAuth;
    LinearLayoutManager layoutmanager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cart);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
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
                                    Intent slideactivity = new Intent(MyCart.this, SecurityPin.class)
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

            icon = findViewById(R.id.menuIcon);
            recyclerview = findViewById(R.id.rview);
            emptyTag = findViewById(R.id.empty_tag);
            totalPrice = findViewById(R.id.total);
            totalItems = findViewById(R.id.totalItems);
            checkoutBtn = findViewById(R.id.checkOut);

            layoutmanager = new LinearLayoutManager(MyCart.this);
            recyclerview.setLayoutManager(layoutmanager);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            setTitle("");

            /**
             * initialize variables that we'll use to track if multiple restaurants or nah
             */
            restaurantName = "";
            multipleRestaurants = false;
            dialogShow = false;

            /**
             * Initialize firebase database
             */
            db = FirebaseDatabase.getInstance();
            myCartRef = db.getReference("cart/"+myPhone);

            /**
             * On create view fetch my location coordinates
             */

            myLocationRef = db.getReference("location/"+myPhone);

            liveLocationModel = null;
            locationListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    liveLocationModel = dataSnapshot.getValue(LiveLocationModel.class);
                    //Toast.makeText(getContext(), "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            myLocationRef.addValueEventListener(locationListener);

            fetchCart();

            /**
             * Initialize cart items total and keep track of items
             */
            final int[] total = {0};

            cartListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    total[0] = 0;

                    for(DataSnapshot cart : dataSnapshot.getChildren()){
                        ProductDetailsModel prod = cart.getValue(ProductDetailsModel.class);

                        int adapterTotal = prod.getQuantity() * Integer.parseInt(prod.getPrice());
                        total[0] = total[0] + adapterTotal;

                    }
                    totalPrice.setText("Ksh "+total[0]);
                    fetchCart();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            myCartRef.addValueEventListener(cartListener);

            /**
             * Exit activity
             */
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });


            /**
             * Checkout
             */

            checkoutBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {

                    /**
                     * We need to fetch a fresh list on every checkout so as to ascertain if it contains multiple providers or not
                     */
                    myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            list = new ArrayList<>();

                            for(DataSnapshot cart : dataSnapshot.getChildren()){
                                final ProductDetailsModel product = cart.getValue(ProductDetailsModel.class);
                                product.setKey(cart.getKey());
                                list.add(product);
                            }

                            if(!list.isEmpty()){
                                checkoutBtn.setVisibility(View.VISIBLE);
                                /**
                                 * Loop through list and find out if cart contains items from multiple providers
                                 */

                                restaurantName = list.get(0).getOwner();
                                for(int i=0; i<list.size(); i++){

                                    //Compare other providers in the list with the first index
                                    if(!restaurantName.equals(list.get(i).getOwner())){
                                        //Log.d(TAG, restaurantName + " != " + list.get(i).getOwner());
                                        multipleRestaurants = true;
                                        /**
                                         * Prompt user they are about to order from multiple providers, this means an increase in delivery charges
                                         */

                                        if(dialogShow == false){
                                            AlertDialog multiple = new AlertDialog.Builder(MyCart.this)
                                                    .setMessage("You are about to order from more than one vendor")
                                                    //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                                                    .setCancelable(false)
                                                    //set three option buttons
                                                    .setPositiveButton("PROCEED", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int whichButton) {
                                                            dialogShow = false;
                                                            //Snackbar.make(v.getRootView(), "Checkout module", Snackbar.LENGTH_LONG).show();

                                                            //Slide to new activity
                                                            Intent slideactivity = new Intent(MyCart.this, CheckOut.class)
                                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                            slideactivity.putExtra("subTotal",total[0]);
                                                            Bundle bndlanimation =
                                                                    null;
                                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                                                bndlanimation = ActivityOptions.makeCustomAnimation(MyCart.this, R.anim.animation, R.anim.animation2).toBundle();
                                                            }
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                                try {
                                                                    startActivity(slideactivity, bndlanimation);
                                                                } catch (Exception e){

                                                                }
                                                            }

                                                            finish(); //remove this activity from stack
                                                        }
                                                    })//setPositiveButton

                                                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogShow = false;
                                                        }
                                                    })

                                                    .create();
                                            multiple.show();
                                            dialogShow = true;
                                        }
                                    }

                                    if(i == list.size()-1){
                                        if(multipleRestaurants == false){
                                            //Log.d(TAG, i + " == " + (list.size()-1));
                                            //Slide to new activity
                                            Intent slideactivity = new Intent(MyCart.this, CheckOut.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            slideactivity.putExtra("subTotal",total[0]);
                                            Bundle bndlanimation =
                                                    null;
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                                bndlanimation = ActivityOptions.makeCustomAnimation(MyCart.this, R.anim.animation, R.anim.animation2).toBundle();
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                try {
                                                    startActivity(slideactivity, bndlanimation);
                                                } catch (Exception e){

                                                }
                                            }

                                            finish();//clear this activity from stack
                                        }

                                    }

                                }


                            }

                            //list is empty
                            else {
                                checkoutBtn.setVisibility(View.GONE);
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            });
        }

    }

    private void fetchCart() {


        /**
         * Loop through my cart items and add to list Array before passing to adapter
         */
        myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list = new ArrayList<>();

                for(DataSnapshot cart : dataSnapshot.getChildren()){
                    final ProductDetailsModel product = cart.getValue(ProductDetailsModel.class);
                    product.setKey(cart.getKey());
                    list.add(product);
                }

                if(!list.isEmpty()){
                    checkoutBtn.setVisibility(View.VISIBLE);
                    //mSwipeRefreshLayout.setRefreshing(false);
                    Collections.reverse(list);
                    CartAdapter recycler = new CartAdapter(MyCart.this,list);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recycler.notifyDataSetChanged();
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.INVISIBLE);
                    icon.setVisibility(View.INVISIBLE);

                    /**
                     * Loop through list and find out if cart contains items from multiple providers
                     */

                    final DatabaseReference[] restRef = new DatabaseReference[list.size()];
                }

                else {

                    //mSwipeRefreshLayout.setRefreshing(false);
                    checkoutBtn.setVisibility(View.GONE);
                    CartAdapter recycler = new CartAdapter(MyCart.this,list);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        /**
         * Listener to keep track of total items in cart
         */
        totalItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                list = new ArrayList<>();
                list.clear();
                itemCount = 0;
                for(DataSnapshot cart : dataSnapshot.getChildren()){
                    try {
                        final ProductDetailsModel product = cart.getValue(ProductDetailsModel.class);
                        product.setKey(cart.getKey());
                        list.add(product);
                    } catch (Exception e){ }
                }

                try {
                    restaurantName = list.get(0).getOwner();
                    for (int i = 0; i < list.size(); i++) {

                        //Compare other providers in the list with the first index

                        if (!restaurantName.equals(list.get(i).getOwner())) {
                            //Toast.makeText(MyCart.this, restaurantName + " != " + list.get(i).getOwner(), Toast.LENGTH_SHORT).show();
                            multipleRestaurants = true;
                        } else {
                            multipleRestaurants = false;
                        }

                        itemCount = itemCount + list.get(i).getQuantity();
                        totalItems.setText("" + itemCount);
                    }
                } catch (Exception e){
                    totalItems.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myCartRef.addValueEventListener(totalItemsListener);

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
                                Intent slideactivity = new Intent(MyCart.this, SecurityPin.class)
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        recyclerview.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                // no-op
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                recyclerview.setAdapter(null);
                layoutmanager = null;
            }
        });

        try { myLocationRef.removeEventListener(locationListener); } catch (Exception e){ }

        try { myCartRef.removeEventListener(cartListener); } catch (Exception e){ }
        try { myCartRef.removeEventListener(totalItemsListener); } catch (Exception e){ }
    }
}
