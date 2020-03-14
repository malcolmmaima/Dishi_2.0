package com.malcolmmaima.dishi.View.Fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
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
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.OrdersAdapter;
import com.malcolmmaima.dishi.View.Adapter.RestaurantRiderRequestAdapter;

import java.util.ArrayList;
import java.util.List;

public class MyRestaurantsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    List<UserModel> riderRequests = new ArrayList<>();
    RecyclerView recyclerview;
    String myPhone;
    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;

    DatabaseReference userDetailsRef, myRestaurantsRef;
    ValueEventListener myRestaurantsRefListener, userDetailsListener;
    FirebaseDatabase db;
    FirebaseUser user;


    public static MyRestaurantsFragment newInstance() {
        MyRestaurantsFragment fragment = new MyRestaurantsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_my_restaurants, container, false);

        riderRequests.clear();

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        myRestaurantsRef = db.getReference("my_restaurants/"+ myPhone);


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

                // Fetching data from server
                fetchRiderRequests();

            }
        });

        return  v;
    }

    @Override
    public void onRefresh() {
        fetchRiderRequests();
    }

    private void fetchRiderRequests() {
        riderRequests.clear();

        myRestaurantsRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                /**
                 * My restaurants node doesn't exist, meaning i dont have any rider requests
                 */
                if(!dataSnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    RestaurantRiderRequestAdapter recycler = new RestaurantRiderRequestAdapter(getContext(), riderRequests);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                }

                for(final DataSnapshot restaurantRequests : dataSnapshot.getChildren()){

                    /**
                     * Now lets get the user details
                     */
                    userDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+restaurantRequests.getKey());

                    /**
                     * Assign user details to model and set item count value as well
                     */
                    userDetailsListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull final DataSnapshot userDetails) {
                            mSwipeRefreshLayout.setRefreshing(false);
                            UserModel restaurant = userDetails.getValue(UserModel.class);
                            restaurant.setPhone(restaurantRequests.getKey());
                            restaurant.itemCount = 0;
                            //orders.add(customer);
                            riderRequests.add(restaurant);

                            if (!riderRequests.isEmpty()) {
                                //Collections.reverse(orders);
                                RestaurantRiderRequestAdapter recycler = new RestaurantRiderRequestAdapter(getContext(), riderRequests);
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
                                RestaurantRiderRequestAdapter recycler = new RestaurantRiderRequestAdapter(getContext(), riderRequests);
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
                    userDetailsRef.addListenerForSingleValueEvent(userDetailsListener);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        myRestaurantsRef.addListenerForSingleValueEvent(myRestaurantsRefListener);

    }
}