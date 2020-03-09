package com.malcolmmaima.dishi.View.Fragments;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.LocationSettings;
import com.malcolmmaima.dishi.View.Adapter.OrdersAdapter;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class OrdersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    List<UserModel> orders = new ArrayList<>();

//    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;
    Switch liveStatus;
    ImageView liveStatusIcon;
    TextView liveTitle;
    CardView live;

    DatabaseReference dbRef, incomingOrders, userDetailsRef;
    FirebaseDatabase db;
    FirebaseUser user;
    ValueEventListener liveListener, inComingOrdersListener, userDetailsListener;

    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;

    public static OrdersFragment newInstance() {
        OrdersFragment fragment = new OrdersFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_orders, container, false);
//        progressDialog = new ProgressDialog(getContext());

        orders.clear();

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();

        dbRef = db.getReference("users/"+myPhone);
        incomingOrders = db.getReference("orders/"+myPhone);

        //Our Live Status Switch
        liveTitle = v.findViewById(R.id.liveTitle);
        liveStatusIcon = v.findViewById(R.id.liveStatus);
        liveStatus = v.findViewById(R.id.switch1);
        live = v.findViewById(R.id.card_view);

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);

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
                            .setMessage("Restaurant Go Live?")
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

        incomingOrders.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                try {
                    Toast.makeText(getContext(), "New order", Toast.LENGTH_LONG).show();
                    //fetchOrders();
                } catch(Exception e){

                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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
                }

                for(final DataSnapshot userOrders : dataSnapshot.getChildren()){

                    /**
                     * Now lets get the user details
                     */
                    userDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+userOrders.getKey());

                    /**
                     * get items count
                     */

                    final DatabaseReference itemCountRef =
                            FirebaseDatabase.getInstance().getReference("orders/"+myPhone+"/"+userOrders.getKey()+"/items");

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
                            itemCountRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    UserModel customer = userDetails.getValue(UserModel.class);
                                    customer.setPhone(userOrders.getKey());
                                    customer.itemCount = dataSnapshot.getChildrenCount();
                                    //orders.add(customer);
                                    orders.add(customer);

                                    if (!orders.isEmpty()) {
                                        //Collections.reverse(orders);
                                        OrdersAdapter recycler = new OrdersAdapter(getContext(), orders);
                                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                        recyclerview.setLayoutManager(layoutmanager);
                                        recyclerview.setItemAnimator(new DefaultItemAnimator());

                                        recycler.notifyDataSetChanged();

                                        recyclerview.getItemAnimator().setAddDuration(200);
                                        recyclerview.getItemAnimator().setRemoveDuration(200);
                                        recyclerview.getItemAnimator().setMoveDuration(200);
                                        recyclerview.getItemAnimator().setChangeDuration(200);

                                        recyclerview.setAdapter(recycler);
                                        emptyTag.setVisibility(View.INVISIBLE);
                                        icon.setVisibility(View.INVISIBLE);
                                    } else {
//                                        progressDialog.dismiss();
                                        OrdersAdapter recycler = new OrdersAdapter(getContext(), orders);
                                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                        recyclerview.setLayoutManager(layoutmanager);
                                        recyclerview.setItemAnimator(new DefaultItemAnimator());
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
    public void onDestroy() {
        super.onDestroy();
        try {
            dbRef.removeEventListener(liveListener);
            incomingOrders.removeEventListener(inComingOrdersListener);
        } catch (Exception e){

        }

    }
}