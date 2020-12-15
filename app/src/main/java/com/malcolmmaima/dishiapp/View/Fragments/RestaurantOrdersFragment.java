package com.malcolmmaima.dishiapp.View.Fragments;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Activities.LocationSettings;
import com.malcolmmaima.dishiapp.View.Adapter.OrdersAdapter;

import java.util.ArrayList;
import java.util.List;

public class RestaurantOrdersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    List<UserModel> orders = new ArrayList<>();

//    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;
    Switch liveStatus;
    ImageView liveStatusIcon;
    MyTextView_Roboto_Regular liveTitle;
    CardView live;

    DatabaseReference dbRef, incomingOrders, userDetailsRef;
    FirebaseDatabase db;
    FirebaseUser user;
    ValueEventListener liveListener, inComingOrdersListener, userDetailsListener;

    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinearLayoutManager layoutmanager;
    View v;

    public static RestaurantOrdersFragment newInstance() {
        RestaurantOrdersFragment fragment = new RestaurantOrdersFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_restaurant_orders, container, false);
//        progressDialog = new ProgressDialog(getContext());

        orders.clear();

        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            db = FirebaseDatabase.getInstance();

            dbRef = db.getReference("users/"+myPhone);
            incomingOrders = db.getReference("orders/"+myPhone);
        } catch (Exception e){

        }

        //Our Live Status Switch
        liveTitle = v.findViewById(R.id.liveTitle);
        liveStatusIcon = v.findViewById(R.id.liveStatus);
        liveStatus = v.findViewById(R.id.switch1);
        live = v.findViewById(R.id.card_view);

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);
        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                mSwipeRefreshLayout.setRefreshing(true);
                fetchOrders();

            }
        });

        //Just a listener to keep track of orders. refresh if empty
        inComingOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                /**
                 * My orders node doesn't exist, meaning i dont have any order requests
                 */
                if(!dataSnapshot.exists()){
                    fetchOrders();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        incomingOrders.addValueEventListener(inComingOrdersListener);

        liveTitle.setText("loading...");
        liveListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                try {
                    liveTitle.setText("Live");
                    Boolean live = dataSnapshot.getValue(Boolean.class);
                    if(live == true){
                        liveStatus.setChecked(true);
                        liveStatusIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                    }

                    if(live == false){
                        liveStatus.setChecked(false);
                        liveStatusIcon.setColorFilter(ContextCompat.getColor(getContext(), R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
                    }

                    if(live == null){
                        dbRef.child("liveStatus").setValue(false);
                    }

                } catch (Exception e){
                    //Node doesn't exist
                    liveTitle.setText("Go live...");

                    /**
                     * This exception means that this is a fresh account which translates to the user not having set location settings
                     * thus lets set "live" as the default location type "users/locationType"
                     *
                    */
                    dbRef.child("locationType").setValue("live");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                liveTitle.setText("error...");
            }

        };
        dbRef.child("liveStatus").addValueEventListener(liveListener);

        liveStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    final AlertDialog goLive = new AlertDialog.Builder(getContext())
                            .setMessage("Go Live?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dbRef.child("liveStatus").setValue(true);
                                    liveStatus.setChecked(true);
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dbRef.child("liveStatus").setValue(false);
                                    liveStatus.setChecked(false);
                                }
                            })

                            .create();
                    goLive.show();
                }

                else {
                    dbRef.child("liveStatus").setValue(isChecked);
                }

            }
        });

        live.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog settings = new AlertDialog.Builder(getContext())
                        .setMessage("Change location settings?")
                        //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                        .setCancelable(false)
                        //set three option buttons
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent slideactivity = new Intent(getContext(), LocationSettings.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Bundle bndlanimation =
                                        ActivityOptions.makeCustomAnimation(getContext(), R.anim.animation,R.anim.animation2).toBundle();
                                startActivity(slideactivity, bndlanimation);
                            }
                        })//setPositiveButton

                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Do nothing
                            }
                        })

                        .create();
                settings.show();

            }
        });

//        //Load progress dialog before fetching on runtime
//        progressDialog.setMessage("Fetching...");
//        progressDialog.show();

        return  v;
    }

    private void fetchOrders() {
        orders.clear();
        /**
         * Loop through users who have sent orders
         */
        inComingOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                /**
                 * My orders node doesn't exist, meaning i dont have any order requests
                 */
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    OrdersAdapter recycler = new OrdersAdapter(getContext(), orders);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                }

                for(final DataSnapshot userOrders : dataSnapshot.getChildren()){

                    /**
                     * Now lets get the user details
                     */
                    userDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+userOrders.getKey());

                    /**
                     * get items count
                     */

                    final DatabaseReference itemExtraDetails =
                            FirebaseDatabase.getInstance().getReference("orders/"+myPhone+"/"+userOrders.getKey());

                    /**
                     * Assign user details to model and set item count value as well
                     */
                    userDetailsListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull final DataSnapshot userDetails) {
                            //orders.clear();

                            /**
                             * get item count value then user details to model
                             */
                            itemExtraDetails.child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    UserModel customer = userDetails.getValue(UserModel.class);
                                    customer.setPhone(userOrders.getKey());
                                    customer.itemCount = dataSnapshot.getChildrenCount();
                                    customer.setAccount_type("2");
                                    customer.restaurantPhone = myPhone;

                                    //get time order was initiated
                                    itemExtraDetails.child("initiatedOn").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            customer.timeStamp = snapshot.getValue(String.class);

                                            orders.add(customer);

                                            if (!orders.isEmpty()) {
                                                //Collections.reverse(orders);
                                                OrdersAdapter recycler = new OrdersAdapter(getContext(), orders);
                                                recyclerview.setLayoutManager(layoutmanager);
                                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                recycler.notifyDataSetChanged();
                                                recyclerview.setAdapter(recycler);
                                                emptyTag.setVisibility(View.INVISIBLE);
                                                icon.setVisibility(View.INVISIBLE);
                                            } else {
//                                        progressDialog.dismiss();
                                                OrdersAdapter recycler = new OrdersAdapter(getContext(), orders);
                                                recyclerview.setLayoutManager(layoutmanager);
                                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                recyclerview.setAdapter(recycler);
                                                emptyTag.setVisibility(View.VISIBLE);
                                                icon.setVisibility(View.VISIBLE);

                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });



                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };
                    userDetailsRef.addListenerForSingleValueEvent(userDetailsListener);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        incomingOrders.addListenerForSingleValueEvent(inComingOrdersListener);
    }


    @Override
    public void onRefresh() {
        fetchOrders();
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(v != null){
            v = null;

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
            try {
                dbRef.removeEventListener(liveListener);
                incomingOrders.removeEventListener(inComingOrdersListener);
            } catch (Exception e){

            }
        }
    }

}