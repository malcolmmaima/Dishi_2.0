package com.malcolmmaima.dishi.View.Fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
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
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.OrdersAdapter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class RiderRequestsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    String TAG = "RiderRequestsFragment";
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;
    DatabaseReference myRideOrderRequests;
    ValueEventListener myRideOrderRequestsListener;
    ChildEventListener myRideOrderRequestsChildListener;
    FirebaseDatabase db;
    FirebaseUser user;

    DatabaseReference [] ordersRef;
    ValueEventListener [] ordersRefListener;

    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    List <String> myRestaurants = new ArrayList<>();
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinearLayoutManager layoutmanager;

    UserModel assignedCustomer;
    View v;


    public static RiderRequestsFragment newInstance() {
        RiderRequestsFragment fragment = new RiderRequestsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_rider, container, false);
        progressDialog = new ProgressDialog(getContext());

        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            db = FirebaseDatabase.getInstance();

            myRideOrderRequests = db.getReference("my_ride_requests/"+myPhone);

        } catch (Exception e){
            Log.e(TAG, "onCreateView: ", e);
        }

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
                fetchOrders(); //initialize
                /**
                 * Listener to check if there are new ride requests
                 */

                myRideOrderRequestsChildListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        fetchOrders();
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                        //SafeToast.makeText(getContext(), "Removed " + dataSnapshot.getKey(), Toast.LENGTH_SHORT).show();
                        fetchOrders();
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };
                myRideOrderRequests.addChildEventListener(myRideOrderRequestsChildListener);

            }
        });

        return  v;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            for(int i=0; i<myRestaurants.size(); i++) {
                ordersRef[i].removeEventListener(ordersRefListener[i]);
                //SafeToast.makeText(getContext(), "removed: ref[" + i + "]", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e){
            Log.e(TAG, "onDetach: ", e);
        }
    }


    @Override
    public void onRefresh() {
        fetchOrders();
    }

    private void fetchOrders() {
        mSwipeRefreshLayout.setRefreshing(true);
        /**
         * Loop through my restaurants and find restaurants I am a member of
         */
        List<UserModel> AssignedOrders = new ArrayList<>();
        //AssignedOrders.clear();
        myRestaurants.clear();
        ordersRef = new DatabaseReference[0];
        ordersRefListener = new ValueEventListener[0];
        assignedCustomer = new UserModel();

        myRideOrderRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot restaurantPhone) {

                if(!restaurantPhone.exists()){
                    Log.d("RiderRequestFragment", " no ride requests");
                    mSwipeRefreshLayout.setRefreshing(false);
                    OrdersAdapter recycler = new OrdersAdapter(getContext(), AssignedOrders);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                } else {
                    myRestaurants.clear();
                    ordersRef = new DatabaseReference[(int) restaurantPhone.getChildrenCount()];
                    ordersRefListener  = new ValueEventListener[(int) restaurantPhone.getChildrenCount()];

                    for(DataSnapshot x : restaurantPhone.getChildren()){
                        myRestaurants.add(x.getKey()); //Add my restaurants to a list
                        Log.d("RiderRequestFragment", " passed x loop: 1");
                    }

                    try {
                        for(int i=0; i<myRestaurants.size(); i++){
                            Log.d("RiderRequestFragment", " looped thru restaurants: 2");
                            AssignedOrders.clear(); //clear list
                            ordersRef[i] = FirebaseDatabase.getInstance().getReference("orders/"+myRestaurants.get(i));

                            ordersRefListener[i] = new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                                    if(!dataSnapshot.exists()){
                                        mSwipeRefreshLayout.setRefreshing(false);
                                        Log.d("RiderRequestFragment", dataSnapshot.getKey()
                                                +" => order does not exist");
                                    } else {
                                        for(final DataSnapshot orders : dataSnapshot.getChildren()){

                                            if(orders.child("rider").exists() && orders.child("rider").getValue().equals(myPhone)){
                                                //SafeToast.makeText(getContext(), "assigned: " + orders.getKey(), Toast.LENGTH_SHORT).show();
                                                Log.d("RiderRequestFragment", " been assigned order: 3");
                                                AssignedOrders.clear(); //clear list
                                                myRestaurants.clear();
                                                assignedCustomer = new UserModel();
                                                DatabaseReference userDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+orders.getKey());
                                                ValueEventListener
                                                        userDetailsRefListener = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot userDetails) {

                                                        assignedCustomer = userDetails.getValue(UserModel.class);
                                                        assignedCustomer.setPhone(orders.getKey());
                                                        assignedCustomer.riderPhone = myPhone;
                                                        assignedCustomer.itemCount = dataSnapshot.child(orders.getKey()).child("items").getChildrenCount();
                                                        assignedCustomer.restaurantPhone = dataSnapshot.getKey();
                                                        assignedCustomer.setAccount_type("3"); //This fragment belongs to rider accounts, type 3

                                                        AssignedOrders.add(assignedCustomer);

                                                        if (!AssignedOrders.isEmpty()) {
                                                            Log.d("RiderRequestFragment", " found restaurant ride request: 3");
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            LinkedHashSet<UserModel> hashSet = new LinkedHashSet<>(AssignedOrders);
                                                            ArrayList<UserModel> listWithoutDuplicates = new ArrayList<>(hashSet);

                                                            OrdersAdapter recycler = new OrdersAdapter(getContext(), listWithoutDuplicates);
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recycler.notifyDataSetChanged();
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.INVISIBLE);
                                                            icon.setVisibility(View.INVISIBLE);
                                                            myRestaurants.clear();

                                                        } else {
                                                            Log.d("RiderRequestFragment", " 0 found restaurant ride request: 3");
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            OrdersAdapter recycler = new OrdersAdapter(getContext(), AssignedOrders);
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
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            };
                            ordersRef[i].addListenerForSingleValueEvent(ordersRefListener[i]);

                        }
                    } catch (Exception e){ }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRideOrderRequests.addListenerForSingleValueEvent(myRideOrderRequestsListener);
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
                myRideOrderRequests.removeEventListener(myRideOrderRequestsChildListener);
            } catch (Exception e){

            }
        }
    }
}