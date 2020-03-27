package com.malcolmmaima.dishi.View.Fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.OrdersAdapter;

import java.util.ArrayList;
import java.util.List;

public class RiderFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{
    List<UserModel> AssignedOrders = new ArrayList<>();
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference myRideOrderRequests, riderStatusRef;
    ValueEventListener myRideOrderRequestsListener;
    FirebaseDatabase db;
    FirebaseUser user;

    DatabaseReference [] ordersRef;
    ValueEventListener [] ordersRefListener;

    TextView emptyTag;
    AppCompatImageView icon;
    List <String> myRestaurants = new ArrayList<>();
    SwipeRefreshLayout mSwipeRefreshLayout;


    public static RiderFragment newInstance() {
        RiderFragment fragment = new RiderFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_rider, container, false);
        progressDialog = new ProgressDialog(getContext());

        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            db = FirebaseDatabase.getInstance();

            myRideOrderRequests = db.getReference("my_ride_requests/"+myPhone);

        } catch (Exception e){

        }

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

        /**
         * We need to keep track of my active status and whether i (rider) have any orders in progress
         */

        myRideOrderRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot restaurantRequest : dataSnapshot.getChildren()){
                    //Toast.makeText(getContext(), "restaurant: " + restaurantRequest.getKey(), Toast.LENGTH_SHORT).show();
                    for(DataSnapshot assignedCustomer : restaurantRequest.getChildren()){
                        /**
                         * we just need 1 'accepted' order request to keep rider status active otherwise if none then inactive
                         */
                        if(assignedCustomer.getValue().equals("accepted")){
                            DatabaseReference myrestaurants = FirebaseDatabase.getInstance().getReference("my_restaurants/"+myPhone);
                            myrestaurants.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for(DataSnapshot restaurants : dataSnapshot.getChildren()){
                                        //Toast.makeText(getContext(), "restaurants: " + restaurants.getKey(), Toast.LENGTH_SHORT).show();
                                        DatabaseReference restaurantRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+restaurants.getKey()+"/"+myPhone);
                                        restaurantRidersRef.setValue("active");

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }

                        else {
                            DatabaseReference myrestaurants = FirebaseDatabase.getInstance().getReference("my_restaurants/"+myPhone);
                            myrestaurants.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for(DataSnapshot restaurants : dataSnapshot.getChildren()){
                                        //Toast.makeText(getContext(), "restaurants: " + restaurants.getKey(), Toast.LENGTH_SHORT).show();
                                        DatabaseReference restaurantRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+restaurants.getKey()+"/"+myPhone);
                                        restaurantRidersRef.setValue("inactive");

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRideOrderRequests.addValueEventListener(myRideOrderRequestsListener);


        return  v;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            for(int i=0; i<myRestaurants.size(); i++) {
                ordersRef[i].removeEventListener(ordersRefListener[i]);
                //Toast.makeText(getContext(), "removed: ref[" + i + "]", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e){

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            myRideOrderRequests.removeEventListener(myRideOrderRequestsListener);
        } catch (Exception e){

        }
    }

    @Override
    public void onRefresh() {
        fetchOrders();
    }

    private void fetchOrders() {
        /**
         * Loop through my restaurants and find restaurants I am a member of
         */

        AssignedOrders.clear();
        myRestaurants.clear();
        ordersRef = new DatabaseReference[0];
        ordersRefListener = new ValueEventListener[0];

        myRideOrderRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot restaurantPhone) {

                myRestaurants.clear();
                ordersRef = new DatabaseReference[(int) restaurantPhone.getChildrenCount()];
                ordersRefListener  = new ValueEventListener[(int) restaurantPhone.getChildrenCount()];

                for(DataSnapshot x : restaurantPhone.getChildren()){
                    myRestaurants.add(x.getKey()); //Add my restaurants to a list
                }

                try {
                    for(int i=0; i<myRestaurants.size(); i++){
                        AssignedOrders.clear(); //clear list
                        ordersRef[i] = FirebaseDatabase.getInstance().getReference("orders/"+myRestaurants.get(i));
                        ordersRefListener[i] = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                                for(final DataSnapshot orders : dataSnapshot.getChildren()){

                                    if(orders.child("rider").exists() && orders.child("rider").getValue().equals(myPhone)){
                                        //Toast.makeText(getContext(), "assigned: " + orders.getKey(), Toast.LENGTH_SHORT).show();

                                        AssignedOrders.clear(); //clear list
                                        myRestaurants.clear();
                                        DatabaseReference userDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+orders.getKey());
                                        ValueEventListener
                                                userDetailsRefListener = new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot userDetails) {
                                                UserModel assignedCustomer = userDetails.getValue(UserModel.class);
                                                assignedCustomer.setPhone(orders.getKey());
                                                assignedCustomer.itemCount = dataSnapshot.child(orders.getKey()).child("items").getChildrenCount();
                                                assignedCustomer.restaurantPhone = dataSnapshot.getKey();
                                                assignedCustomer.setAccount_type("3"); //This fragment belongs to rider accounts, type 3
                                                AssignedOrders.add(assignedCustomer);

                                                mSwipeRefreshLayout.setRefreshing(false);
                                                if (!AssignedOrders.isEmpty()) {

                                                    //Collections.reverse(orders);
                                                    OrdersAdapter recycler = new OrdersAdapter(getContext(), AssignedOrders);
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
                                                    OrdersAdapter recycler = new OrdersAdapter(getContext(), AssignedOrders);
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
                                        };
                                        userDetailsRef.addListenerForSingleValueEvent(userDetailsRefListener);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        };
                        ordersRef[i].addValueEventListener(ordersRefListener[i]);
                    }
                } catch (Exception e){

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRideOrderRequests.addListenerForSingleValueEvent(myRideOrderRequestsListener);
    }
}