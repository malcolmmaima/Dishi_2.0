package com.malcolmmaima.dishi.View.Fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Utils.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.RestaurantAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class FragmentRestaurants extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    String TAG = "FragmentRestaurants";
    List<UserModel> list;
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;
    LiveLocationModel liveLocationModel;
    TextView emptyTag, distanceShow;
    DatabaseReference dbRef, menusRef, myLocationRef;
    ValueEventListener locationListener;
    FirebaseDatabase db;
    FirebaseUser user;
    SeekBar seekBar;
    AppCompatImageView icon;

    SwipeRefreshLayout mSwipeRefreshLayout;
    int location_filter;


    public static FragmentRestaurants newInstance() {
        FragmentRestaurants fragment = new FragmentRestaurants();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_restaurants, container, false);
        progressDialog = new ProgressDialog(getContext());

        location_filter = 0; // initialize distance filter

        seekBar = v.findViewById(R.id.seekBar);
        distanceShow = v.findViewById(R.id.distanceShow);
        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference("users/"+myPhone);
        myLocationRef = db.getReference("location/"+myPhone);
        menusRef = db.getReference("menus");

        //Fetch location filter value from database
        dbRef.child("location-filter").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {

                    location_filter = dataSnapshot.getValue(Integer.class);
                    seekBar.setProgress(location_filter);
                    distanceShow.setText("("+location_filter+"km)");

                } catch (Exception e){

                    //Doesn't exist in the database, lets set value in node
                    dbRef.child("location-filter").setValue(0);
                    location_filter = 0;
                    seekBar.setProgress(location_filter);
                    distanceShow.setText("("+location_filter+"km)");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Distance filter seekbar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                dbRef.child("location-filter").setValue(progress).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        location_filter = progress;
                        distanceShow.setText("("+progress+"km)");
                        //SafeToast.makeText(getContext(), "filter posted", Toast.LENGTH_SHORT).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        SafeToast.makeText(getContext(), "Something went wrong", Toast.LENGTH_LONG).show();
                    }
                });

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSwipeRefreshLayout.setRefreshing(true);
                fetchRestaurants();
            }
        });

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();

        /**
         * On create view fetch my location coordinates
         */

        liveLocationModel = null;
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    liveLocationModel = dataSnapshot.getValue(LiveLocationModel.class);
                    //SafeToast.makeText(getContext(), "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        myLocationRef.addValueEventListener(locationListener);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                mSwipeRefreshLayout.setRefreshing(true);

                // Fetching data from server
                fetchRestaurants();

            }
        });

        return  v;
    }

    private void fetchRestaurants() {

        //Fetch restaurants
        menusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot datasnapshot) {

                list = new ArrayList<>();
                for(final DataSnapshot restaurants : datasnapshot.getChildren()){

                    /**
                     * Create new database reference for each restaurant and fetch user data
                     */
                    DatabaseReference userData = FirebaseDatabase.getInstance().getReference("users/"+ restaurants.getKey());
                    userData.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            final UserModel user = dataSnapshot.getValue(UserModel.class);
                            user.setPhone(restaurants.getKey());
//                            SafeToast.makeText(getContext(), "Name: " + user.getFirstname()
//                                    + "\nliveStatus: " + user.getLiveStatus()
//                                    + "\nlocationType: " + user.getLocationType(), Toast.LENGTH_SHORT).show();

                            /**
                             * Check "liveStatus" of each restautant (must be true so as to allow menu to be fetched
                             */

                            try {
                                if (user.getLiveStatus() == true) {

                                    /**
                                     * Now check "locationType" so as to decide which location node to fetch, live or static
                                     */
                                    if (user.getLocationType().equals("default")) {
                                        //if location type is default then fetch static location
                                        DatabaseReference defaultLocation = FirebaseDatabase.getInstance().getReference("users/" + restaurants.getKey() + "/my_location");

                                        defaultLocation.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                try {
                                                    StaticLocationModel staticLocationModel = dataSnapshot.getValue(StaticLocationModel.class);

                                                    /**
                                                     * Now lets compute distance of each restaurant with customer location
                                                     */
                                                    CalculateDistance calculateDistance = new CalculateDistance();
                                                    Double dist = calculateDistance.distance(liveLocationModel.getLatitude(),
                                                            liveLocationModel.getLongitude(), staticLocationModel.getLatitude(), staticLocationModel.getLongitude(), "K");

                                                    //SafeToast.makeText(getContext(), restaurants.getKey() + ": " + dist + "km", Toast.LENGTH_SHORT).show();

                                                    /**
                                                     * if distance meets parameters set fetch menu
                                                     */

                                                    if (dist < location_filter) {
                                                        //Fetch menu items of restaurants that have passed distance parameter
                                                        user.setDistance(dist);
                                                        list.add(user);

//                                                    for (DataSnapshot menu : restaurants.getChildren()) {
//                                                        //SafeToast.makeText(getContext(), restaurants.getKey()+": "+ menu.getKey(), Toast.LENGTH_SHORT).show();
//                                                        ProductDetails product = menu.getValue(ProductDetails.class);
//                                                        product.setKey(menu.getKey());
//                                                        product.setDistance(dist);
//                                                        list.add(product);
//                                                    }
                                                    }

                                                    if (!list.isEmpty()) {
                                                        /**
                                                         * https://howtodoinjava.com/sort/collections-sort/
                                                         * We want to sort from nearest to furthest location
                                                         */
                                                        Collections.sort(list, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                        mSwipeRefreshLayout.setRefreshing(false);
                                                        //Collections.reverse(list);
                                                        RestaurantAdapter recycler = new RestaurantAdapter(getContext(), list);
                                                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                        recyclerview.setLayoutManager(layoutmanager);
                                                        recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                        recycler.notifyDataSetChanged();
                                                        recyclerview.setAdapter(recycler);
                                                        emptyTag.setVisibility(View.INVISIBLE);
                                                        icon.setVisibility(View.INVISIBLE);
                                                    } else {

                                                        mSwipeRefreshLayout.setRefreshing(false);

                                                        RestaurantAdapter recycler = new RestaurantAdapter(getContext(), list);
                                                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                        recyclerview.setLayoutManager(layoutmanager);
                                                        recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                        recyclerview.setAdapter(recycler);
                                                        emptyTag.setVisibility(View.VISIBLE);
                                                        icon.setVisibility(View.VISIBLE);
                                                    }
                                                } catch (Exception e){
                                                    Log.e(TAG, "onDataChange: ", e);
                                                }

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                    /**
                                     * If location type is live then track restaurant live location instead of static location
                                     */
                                    else if (user.getLocationType().equals("live")) {
                                        DatabaseReference restliveLocation = FirebaseDatabase.getInstance().getReference("location/" + restaurants.getKey());

                                        restliveLocation.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                try {

                                                    LiveLocationModel restLiveLoc = dataSnapshot.getValue(LiveLocationModel.class);

                                                    /**
                                                     * Now lets compute distance of each restaurant with customer location
                                                     */
                                                    CalculateDistance calculateDistance = new CalculateDistance();
                                                    Double dist = calculateDistance.distance(liveLocationModel.getLatitude(),
                                                            liveLocationModel.getLongitude(), restLiveLoc.getLatitude(), restLiveLoc.getLongitude(), "K");

                                                    //SafeToast.makeText(getContext(), restaurants.getKey() + ": " + dist + "km", Toast.LENGTH_SHORT).show();

                                                    /**
                                                     * if distance meets parameters set then fetch menu
                                                     */

                                                    if (dist < location_filter) {
                                                        //Fetch menu items of restaurants that have passed distance parameter

                                                        user.setDistance(dist);
                                                        list.add(user);
                                                    }

                                                } catch (Exception e){
                                                    Log.e(TAG, "onDataChange: ", e);
                                                }
                                                if (!list.isEmpty()) {
                                                    /**
                                                     * https://howtodoinjava.com/sort/collections-sort/
                                                     * We want to sort from nearest to furthest location
                                                     */
                                                    Collections.sort(list, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                    mSwipeRefreshLayout.setRefreshing(false);
                                                    //Collections.reverse(list);
                                                    RestaurantAdapter recycler = new RestaurantAdapter(getContext(), list);
                                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                    recyclerview.setLayoutManager(layoutmanager);
                                                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                    recycler.notifyDataSetChanged();
                                                    recyclerview.setAdapter(recycler);
                                                    emptyTag.setVisibility(View.INVISIBLE);
                                                    icon.setVisibility(View.INVISIBLE);
                                                } else {

                                                    mSwipeRefreshLayout.setRefreshing(false);

                                                    RestaurantAdapter recycler = new RestaurantAdapter(getContext(), list);
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

                                    /**
                                     * available track options are "default" which tracks the restaurant's static location under "users/phone/my_location"
                                     * and "live" which tracks the restaurant's live location under "location/phone"
                                     */
                                    else {
                                        SafeToast.makeText(getContext(), "Something went wrong, contact support!", Toast.LENGTH_LONG).show();
                                    }
                                }

                            } catch (Exception e){

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
    public void onRefresh() {
        fetchRestaurants();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Patch... dealing with memory leaks (any listener that uses addValueEventListener must be removed onDestroy())
        myLocationRef.removeEventListener(locationListener);
    }
}