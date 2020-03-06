package com.malcolmmaima.dishi.View.Fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import com.malcolmmaima.dishi.View.Adapter.MyOrdersAdapter;
import com.malcolmmaima.dishi.View.Adapter.OrdersAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyOrdersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    List<UserModel> orders;
    //    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference dbRef, myOrders, userDetailsRef;
    FirebaseDatabase db;
    FirebaseUser user;
    ValueEventListener userDetailsListener;

    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;


    public static MyOrdersFragment newInstance() {
        MyOrdersFragment fragment = new MyOrdersFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_my_orders, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();

        dbRef = db.getReference("users/"+myPhone);
        myOrders = db.getReference("my_orders/"+myPhone);

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


        return  v;
    }

    private void fetchOrders() {
        myOrders.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                /**
                 * Check to see if my orders exists for my account
                 */
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    orders = new ArrayList<>();
                    MyOrdersAdapter recycler = new MyOrdersAdapter(getContext(), orders);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                }

                /**
                 * Exists, get customer details and order details
                 */
                else {
                    for(final DataSnapshot userOrders : dataSnapshot.getChildren()){

                        /**
                         * Now lets get the user details
                         */
                        userDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+userOrders.getKey());

                        /**
                         * get items count
                         */

                        final DatabaseReference itemCountRef =
                                FirebaseDatabase.getInstance().getReference("orders/"+userOrders.getKey()+"/"+myPhone+"/items");

                        /**
                         * Assign user details to model and set item count value as well
                         */
                        userDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull final DataSnapshot userDetails) {
                                orders = new ArrayList<>();
                                /**
                                 * get item count value then user details to model
                                 */
                                itemCountRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        UserModel customer = userDetails.getValue(UserModel.class);
                                        customer.setPhone(userOrders.getKey());
                                        customer.itemCount = dataSnapshot.getChildrenCount();
                                        orders.add(customer);

                                        if (!orders.isEmpty()) {

                                            mSwipeRefreshLayout.setRefreshing(false);
//                                        progressDialog.dismiss();
                                            Collections.reverse(orders);
                                            MyOrdersAdapter recycler = new MyOrdersAdapter(getContext(), orders);
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

                                            mSwipeRefreshLayout.setRefreshing(false);
//                                        progressDialog.dismiss();
                                            MyOrdersAdapter recycler = new MyOrdersAdapter(getContext(), orders);
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
                        });

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRefresh() {
        fetchOrders();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
