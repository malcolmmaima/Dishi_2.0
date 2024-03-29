package com.malcolmmaima.dishiapp.View.Fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.malcolmmaima.dishiapp.View.Adapter.MyOrdersAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class MyOrdersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    List<UserModel> orders = new ArrayList<>();
    //    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference dbRef, myOrders, userDetailsRef;
    FirebaseDatabase db;
    FirebaseUser user;
    ValueEventListener myOrdersListener;
    LinearLayoutManager layoutmanager;
    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    View v;


    public MyOrdersFragment newInstance() {
        MyOrdersFragment fragment = new MyOrdersFragment();
        fetchOrders(); //Ukora ...calling this method from here fixes my orders list duplication bug problem so meeeh
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_my_orders, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();

        dbRef = db.getReference("users/"+myPhone);
        myOrders = db.getReference("my_orders/"+myPhone);

        icon = v.findViewById(R.id.menuIcon);
        icon.setVisibility(View.GONE);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);
        emptyTag.setVisibility(View.GONE);
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

        myOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists()) {
                    fetchOrders();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myOrders.addValueEventListener(myOrdersListener);

        return  v;
    }


    private void fetchOrders() {
        orders.clear();
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
                         * Assign user details to model and set item count value as well
                         */
                        userDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull final DataSnapshot userDetails) {
                                final UserModel customer = userDetails.getValue(UserModel.class);
                                customer.setPhone(userOrders.getKey());
                                orders.add(customer);
                                /**
                                 * get item count value then user details to model
                                 */
                                final DatabaseReference itemCountRef =
                                        FirebaseDatabase.getInstance().getReference("orders/"+userOrders.getKey()+"/"+myPhone+"/items");

                                itemCountRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(!dataSnapshot.exists()){
                                            myOrders.child(userOrders.getKey()).removeValue(); //update my_orders node
                                        } else {
                                            customer.itemCount = dataSnapshot.getChildrenCount();

//                                        LinkedHashSet<UserModel> hashSet = new LinkedHashSet<>(orders);
//                                        ArrayList<UserModel> listWithoutDuplicates = new ArrayList<>(hashSet);
                                            if (!orders.isEmpty()) {

                                                mSwipeRefreshLayout.setRefreshing(false);
                                                Collections.reverse(orders);
                                                MyOrdersAdapter recycler = new MyOrdersAdapter(getContext(), orders);
                                                recyclerview.setLayoutManager(layoutmanager);
                                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                recycler.notifyDataSetChanged();
                                                recyclerview.setAdapter(recycler);
                                                emptyTag.setVisibility(View.INVISIBLE);
                                                icon.setVisibility(View.INVISIBLE);
                                            } else {

                                                mSwipeRefreshLayout.setRefreshing(false);
//                                        progressDialog.dismiss();
                                                MyOrdersAdapter recycler = new MyOrdersAdapter(getContext(), orders);
                                                recyclerview.setLayoutManager(layoutmanager);
                                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                recyclerview.setAdapter(recycler);
                                                emptyTag.setVisibility(View.VISIBLE);
                                                icon.setVisibility(View.VISIBLE);

                                            }
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
            try { myOrders.removeEventListener(myOrdersListener); } catch (Exception e){ }
        }
    }

}
