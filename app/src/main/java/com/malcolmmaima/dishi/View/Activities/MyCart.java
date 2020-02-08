package com.malcolmmaima.dishi.View.Activities;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.malcolmmaima.dishi.Controller.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.CartAdapter;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyCart extends AppCompatActivity {

    List<ProductDetails> list;
    RecyclerView recyclerview;
    String myPhone, restaurantName;
    TextView emptyTag, totalPrice, totalItems;
    AppCompatImageView icon;
    LiveLocation liveLocation;
    Button checkoutBtn;
    Boolean multipleRestaurants;
    DatabaseReference myCartRef, myLocationRef;
    FirebaseDatabase db;
    FirebaseUser user;
    int itemCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cart);

        itemCount = 0;

        icon = findViewById(R.id.menuIcon);
        recyclerview = findViewById(R.id.rview);
        emptyTag = findViewById(R.id.empty_tag);
        totalPrice = findViewById(R.id.total);
        totalItems = findViewById(R.id.totalItems);
        checkoutBtn = findViewById(R.id.checkOut);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");

        /**
         * initialize variables that we'll use to track if multiple restaurants or nah
         */
        restaurantName = "";
        multipleRestaurants = false;


        /**
         * Initialize firebase database
         */
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        myCartRef = db.getReference("cart/"+myPhone);

        /**
         * On create view fetch my location coordinates
         */

        myLocationRef = db.getReference("location/"+myPhone);

        liveLocation = null;
        myLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                liveLocation = dataSnapshot.getValue(LiveLocation.class);
                //Toast.makeText(getContext(), "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        fetchCart();

        /**
         * Initialize cart items total and keep track of items
         */
        final int[] total = {0};

        myCartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                total[0] = 0;
                for(DataSnapshot cart : dataSnapshot.getChildren()){
                    ProductDetails prod = cart.getValue(ProductDetails.class);

                    int adapterTotal = prod.getQuantity() * Integer.parseInt(prod.getPrice());
                    total[0] = total[0] + adapterTotal;
                }
                totalPrice.setText("Ksh "+total[0]);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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
                            final ProductDetails product = cart.getValue(ProductDetails.class);
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
                                    //Toast.makeText(MyCart.this, restaurantName + " != " + list.get(i).getOwner(), Toast.LENGTH_SHORT).show();
                                    multipleRestaurants = true;

                                    //Perform action only once (if loop is complete)
                                    if(i == list.size()-1){
                                        Snackbar.make(v.getRootView(), "Multiple restaurants", Snackbar.LENGTH_LONG).show();
                                    }
                                }

                                else {
                                    multipleRestaurants = false;

                                    //Perform action only once (if loop is complete)
                                    if(i == list.size()-1){
                                        Snackbar.make(v.getRootView(), "No Multiple restaurants", Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            }


                        }

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

    private void fetchCart() {


        /**
         * Loop through my cart items and add to list Array before passing to adapter
         */
        myCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list = new ArrayList<>();

                for(DataSnapshot cart : dataSnapshot.getChildren()){
                    final ProductDetails product = cart.getValue(ProductDetails.class);
                    product.setKey(cart.getKey());
                    list.add(product);
                }

                if(!list.isEmpty()){
                    checkoutBtn.setVisibility(View.VISIBLE);
                    //mSwipeRefreshLayout.setRefreshing(false);
                    Collections.reverse(list);
                    CartAdapter recycler = new CartAdapter(MyCart.this,list);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(MyCart.this);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());

                    recycler.notifyDataSetChanged();

                    recyclerview.getItemAnimator().setAddDuration(200);
                    recyclerview.getItemAnimator().setRemoveDuration(200);
                    recyclerview.getItemAnimator().setMoveDuration(200);
                    recyclerview.getItemAnimator().setChangeDuration(200);

                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.INVISIBLE);
                    icon.setVisibility(View.INVISIBLE);

                    /**
                     * Loop through list and find out if cart contains items from multiple providers
                     */

                    final DatabaseReference restRef[] = new DatabaseReference[list.size()];

                    restaurantName = list.get(0).getOwner();
                    for(int i=0; i<list.size(); i++){

                        //Compare other providers in the list with the first index
                        if(!restaurantName.equals(list.get(i).getOwner())){
                            //Toast.makeText(MyCart.this, restaurantName + " != " + list.get(i).getOwner(), Toast.LENGTH_SHORT).show();
                            multipleRestaurants = true;
                        }

                        else {
                            multipleRestaurants = false;
                        }

                        itemCount = itemCount + list.get(i).getQuantity();
                        totalItems.setText(""+itemCount);
                    }

                }

                else {

                    //mSwipeRefreshLayout.setRefreshing(false);
                    checkoutBtn.setVisibility(View.GONE);
                    CartAdapter recycler = new CartAdapter(MyCart.this,list);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(MyCart.this);
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
    }
}
