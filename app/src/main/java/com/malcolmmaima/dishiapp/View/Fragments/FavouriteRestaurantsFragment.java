package com.malcolmmaima.dishiapp.View.Fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
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
import com.malcolmmaima.dishiapp.Controller.Utils.CalculateDistance;
import com.malcolmmaima.dishiapp.Model.LiveLocationModel;
import com.malcolmmaima.dishiapp.Model.StaticLocationModel;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Adapter.RestaurantAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class FavouriteRestaurantsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    String TAG = "FavouriteRestaurantsFragment";
    List<UserModel> list;
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;
    LiveLocationModel liveLocationModel;
    MyTextView_Roboto_Regular emptyTag;
    DatabaseReference dbRef, favouriteRestaurantsRef, myLocationRef;
    ValueEventListener locationListener;
    FirebaseDatabase db;
    FirebaseUser user;
    AppCompatImageView icon;
    View v;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinearLayoutManager layoutmanager;

    public static FavouriteRestaurantsFragment newInstance() {
        FavouriteRestaurantsFragment fragment = new FavouriteRestaurantsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_favourite_restaurants, container, false);
        progressDialog = new ProgressDialog(getContext());

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);

        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference("users/"+myPhone);
        myLocationRef = db.getReference("location/"+myPhone);
        favouriteRestaurantsRef = db.getReference("my_restaurant_favourites/"+myPhone);

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
                fetchRestaurants();

            }
        });

        return  v;
    }

    //Fetch my favourite restaurants
    private void fetchRestaurants() {

        //Fetch restaurants
        favouriteRestaurantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot datasnapshot) {

                list = new ArrayList<>();
                if(!datasnapshot.exists()){
                    mSwipeRefreshLayout.setRefreshing(false);

                    RestaurantAdapter recycler = new RestaurantAdapter(getContext(), list);
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                } else {
                    for(final DataSnapshot restaurants : datasnapshot.getChildren()){

                        /**
                         * Create new database reference for each restaurant and fetch user data
                         */
                        DatabaseReference userData = FirebaseDatabase.getInstance().getReference("users/"+ restaurants.getKey());
                        userData.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()){
                                    //Log.d("UsersNode", restaurants.getKey()+" does not exist");
                                }
                                else {
                                    final UserModel user = dataSnapshot.getValue(UserModel.class);
                                    user.setPhone(restaurants.getKey());

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

                                                            user.setDistance(dist);
                                                            list.add(user);

                                                            /**
                                                             * if distance meets parameters set fetch menu
                                                             */

                                                            if (!list.isEmpty()) {
                                                                /**
                                                                 * https://howtodoinjava.com/sort/collections-sort/
                                                                 * We want to sort from nearest to furthest location
                                                                 */
                                                                Collections.sort(list, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                                mSwipeRefreshLayout.setRefreshing(false);
                                                                //Collections.reverse(list);
                                                                RestaurantAdapter recycler = new RestaurantAdapter(getContext(), list);
                                                                recyclerview.setLayoutManager(layoutmanager);
                                                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                                recycler.notifyDataSetChanged();
                                                                recyclerview.setAdapter(recycler);
                                                                emptyTag.setVisibility(View.INVISIBLE);
                                                                icon.setVisibility(View.INVISIBLE);
                                                            } else {

                                                                mSwipeRefreshLayout.setRefreshing(false);

                                                                RestaurantAdapter recycler = new RestaurantAdapter(getContext(), list);
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

                                                            //Toast.makeText(getContext(), restaurants.getKey() + ": " + dist + "km", Toast.LENGTH_SHORT).show();

                                                            user.setDistance(dist);
                                                            list.add(user);

                                                        } catch (Exception e){

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
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recycler.notifyDataSetChanged();
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.INVISIBLE);
                                                            icon.setVisibility(View.INVISIBLE);
                                                        } else {

                                                            mSwipeRefreshLayout.setRefreshing(false);

                                                            RestaurantAdapter recycler = new RestaurantAdapter(getContext(), list);
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
                                                Toast.makeText(getContext(), "Something went wrong, contact support!", Toast.LENGTH_LONG).show();
                                            }
                                        }

                                    } catch (Exception e){ }
                                }

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
        fetchRestaurants();
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
            //Patch... dealing with memory leaks (any listener that uses addValueEventListener must be removed onDestroy())
            myLocationRef.removeEventListener(locationListener); } catch (Exception e){}
        }
    }

}