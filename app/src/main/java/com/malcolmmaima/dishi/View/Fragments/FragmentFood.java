package com.malcolmmaima.dishi.View.Fragments;

import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.CustomerActivity;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



import static android.content.Context.LOCATION_SERVICE;

public class FragmentFood extends Fragment implements SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    List<ProductDetailsModel> list;
    RecyclerView recyclerview;
    String myPhone;
    MyTextView_Roboto_Regular emptyTag, distanceShow;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LiveLocationModel liveLocationModel;
    SeekBar seekBar;
    Boolean gpsDialogShown;

    DatabaseReference dbRef, menusRef, myLocationRef;
    FirebaseDatabase db;
    FirebaseUser user;
    ValueEventListener locationListener;
    View v;
    LinearLayoutManager layoutmanager;

    int location_filter;


    public static FragmentFood newInstance() {
        FragmentFood fragment = new FragmentFood();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_food, container, false);

        gpsDialogShown = false;
        location_filter = 0; // initialize distance filter

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference("users/"+myPhone);
        myLocationRef = db.getReference("location/"+myPhone);
        menusRef = db.getReference("menus");

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);
        seekBar = v.findViewById(R.id.seekBar);
        distanceShow = v.findViewById(R.id.distanceShow);

        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);

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
                        //Toast.makeText(getContext(), "filter posted", Toast.LENGTH_SHORT).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Write failed
                                Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                            }
                        });

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSwipeRefreshLayout.setRefreshing(true);
                fetchFood();
            }
        });

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        /**
         * On create view fetch my location coordinates
         */

        liveLocationModel = null;
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    liveLocationModel = dataSnapshot.getValue(LiveLocationModel.class);
                    //Toast.makeText(getContext(), "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();

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
                fetchFood();

            }
        });

        return  v;
    }

    private void fetchFood() {

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
                            if(dataSnapshot.exists()){
                                final UserModel user = dataSnapshot.getValue(UserModel.class);

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

                                                        //Toast.makeText(getContext(), restaurants.getKey() + ": " + dist + "km", Toast.LENGTH_SHORT).show();

                                                        /**
                                                         * if distance meets parameters set fetch menu
                                                         */

                                                        if (dist < location_filter) {
                                                            //Fetch menu items of restaurants that have passed distance parameter

                                                            for (DataSnapshot menu : restaurants.getChildren()) {
                                                                //Toast.makeText(getContext(), restaurants.getKey()+": "+ menu.getKey(), Toast.LENGTH_SHORT).show();
                                                                ProductDetailsModel product = menu.getValue(ProductDetailsModel.class);
                                                                product.setKey(menu.getKey());
                                                                product.setDistance(dist);
                                                                product.accountType = "1"; //This fragment belongs to account type 1 (customer)

                                                                if(list.size() < 120){ //put a cap of 100 items
                                                                    list.add(product);
                                                                }
                                                            }
                                                        }

                                                        if (!list.isEmpty()) {
                                                            /**
                                                             * https://howtodoinjava.com/sort/collections-sort/
                                                             * We want to sort from nearest to furthest location
                                                             */
                                                            Collections.reverse(list);
                                                            Collections.sort(list, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            ProductAdapter recycler = new ProductAdapter(getContext(), list);
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recycler.notifyDataSetChanged();
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.INVISIBLE);
                                                            icon.setVisibility(View.INVISIBLE);
                                                        } else {

                                                            mSwipeRefreshLayout.setRefreshing(false);

                                                            ProductAdapter recycler = new ProductAdapter(getContext(), list);
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.VISIBLE);
                                                            icon.setVisibility(View.VISIBLE);

                                                        }
                                                    } catch (Exception e){
                                                        Log.e("FragmentFood", "onDataChange: ", e);
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
                                                    LiveLocationModel restLiveLoc = dataSnapshot.getValue(LiveLocationModel.class);
//                                            Toast.makeText(getContext(), restaurants.getKey() + ": "
//                                                    + restLiveLoc.getLatitude() + ","
//                                                    + restLiveLoc.getLongitude(), Toast.LENGTH_SHORT).show();

                                                    /**
                                                     * Now lets compute distance of each restaurant with customer location
                                                     */
                                                    try {
                                                        CalculateDistance calculateDistance = new CalculateDistance();
                                                        Double dist = calculateDistance.distance(liveLocationModel.getLatitude(),
                                                                liveLocationModel.getLongitude(), restLiveLoc.getLatitude(), restLiveLoc.getLongitude(), "K");

                                                        //Toast.makeText(getContext(), restaurants.getKey() + ": " + dist + "km", Toast.LENGTH_SHORT).show();

                                                        /**
                                                         * if distance meets parameters set then fetch menu
                                                         */

                                                        if (dist < location_filter) {
                                                            //Fetch menu items of restaurants that have passed distance parameter

                                                            for (DataSnapshot menu : restaurants.getChildren()) {
                                                                //Toast.makeText(getContext(), restaurants.getKey()+": "+ menu.getKey(), Toast.LENGTH_SHORT).show();
                                                                ProductDetailsModel product = menu.getValue(ProductDetailsModel.class);
                                                                product.setKey(menu.getKey());
                                                                product.setDistance(dist);
                                                                product.accountType = "1"; //this fragment belongs to account type 1
                                                                if(list.size() < 120){
                                                                    list.add(product);
                                                                }
                                                            }
                                                        }

                                                        if (!list.isEmpty()) {
                                                            /**
                                                             * https://howtodoinjava.com/sort/collections-sort/
                                                             * We want to sort from nearest to furthest location
                                                             */
                                                            Collections.reverse(list);
                                                            Collections.sort(list, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            ProductAdapter recycler = new ProductAdapter(getContext(), list);
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recycler.notifyDataSetChanged();
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.INVISIBLE);
                                                            icon.setVisibility(View.INVISIBLE);
                                                        } else {

                                                            mSwipeRefreshLayout.setRefreshing(false);

                                                            ProductAdapter recycler = new ProductAdapter(getContext(), list);
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.VISIBLE);
                                                            icon.setVisibility(View.VISIBLE);

                                                        }
                                                    } catch (Exception e){

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
                                            Toast.makeText(getContext(), "Something went wrong, contact support!", Toast.LENGTH_LONG).show();
                                        }
                                    }

                                } catch (Exception e){

                                }
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

    private void checkGPS() {
        if(gpsDialogShown == false){
            LocationManager lm = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                //Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_LONG).show();
                GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getContext())
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this).build();
                googleApiClient.connect();

                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(5 * 1000);
                locationRequest.setFastestInterval(2 * 1000);
                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest);

                //**************************
                builder.setAlwaysShow(true); //this is the key ingredient
                //**************************

                PendingResult<LocationSettingsResult> result =
                        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(@NonNull LocationSettingsResult result) {
                        final Status status = result.getStatus();
                        //                final LocationSettingsStates state = result.getLocationSettingsStates();

                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:


                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied. But could be fixed by showing the user
                                // a dialog.
                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    status.startResolutionForResult(getActivity(), 1000);
                                } catch (IntentSender.SendIntentException e) {
                                    // Ignore the error.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                break;
                        }
                    }
                });
                gpsDialogShown = true;
            }
        }
    }

    @Override
    public void onRefresh() {
        fetchFood();
        checkGPS();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        myLocationRef.removeEventListener(locationListener);
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

            try { myLocationRef.removeEventListener(locationListener); } catch (Exception e){}
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}