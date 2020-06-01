package com.malcolmmaima.dishi.View.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.SearchActivity;
import com.malcolmmaima.dishi.View.Adapter.RestaurantAdapter;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class FragmentSearchVendors extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    RecyclerView recyclerview;
    MyTextView_Roboto_Regular emptyTag;
    SwipeRefreshLayout mSwipeRefreshLayout;
    String searchValue, myPhone;
    FirebaseUser user;
    LiveLocationModel liveLocationModel;
    DatabaseReference myLocationRef, myUserDetails;
    ValueEventListener locationListener;
    UserModel myDetails;
    int searchCap;
    View view;
    LinearLayoutManager layoutmanager;

    public static FragmentSearchVendors newInstance() {
        FragmentSearchVendors fragment = new FragmentSearchVendors();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_search_vendors, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber();
        searchCap = 100;
        //String searchString;
        //searchString = getArguments().getString("search");

        myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);

        liveLocationModel = null;
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                liveLocationModel = dataSnapshot.getValue(LiveLocationModel.class);
                //SafeToast.makeText(getContext(), "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myLocationRef.addValueEventListener(locationListener);

        myUserDetails = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

        myUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myDetails = dataSnapshot.getValue(UserModel.class);
                myDetails.setPhone(dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        SearchActivity activity = (SearchActivity) getActivity();
        searchValue = activity.getSearchValue();

        recyclerview = view.findViewById(R.id.rview);
        emptyTag = view.findViewById(R.id.empty_tag);
        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
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

                searchVendors(searchValue);
            }
        });


        return view;
    }

    private void searchVendors(String searchValue) {
        if(isStringNullOrWhiteSpace(searchValue)){
            emptyTag.setText("Type something");
            emptyTag.setVisibility(VISIBLE);
        } else {
            mSwipeRefreshLayout.setRefreshing(true);
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("menus").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot datasnapshot) {
                    List<UserModel> restaurantsList = new ArrayList<>();
                    for(final DataSnapshot restaurants : datasnapshot.getChildren()){
                        Log.d("Restaurants", "found: "+restaurants.getKey());
                        /**
                         * Create new database reference for each restaurant and fetch user data
                         */
                        DatabaseReference userData = FirebaseDatabase.getInstance().getReference("users/"+ restaurants.getKey());
                        userData.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                final UserModel user = dataSnapshot.getValue(UserModel.class);
                                user.setPhone(restaurants.getKey());
                                Log.d("Restaurants", "name: "+user.getFirstname());

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

                                                        String restaurantName = user.getFirstname() + " " + user.getLastname();
                                                        if (!myPhone.equals(user.getPhone())) {

                                                            if(restaurantsList.size() < searchCap){
                                                                if (restaurantName.toLowerCase().contains(searchValue.toLowerCase())) {
                                                                    user.setDistance(dist);
                                                                    restaurantsList.add(user);
                                                                } else if (searchValue.toLowerCase().contains(restaurantName.toLowerCase())) {
                                                                    user.setDistance(dist);
                                                                    restaurantsList.add(user);
                                                                }
                                                                //search if word is equal to user name object
                                                                else if (searchValue.toLowerCase() == restaurantName.toLowerCase()) {
                                                                    user.setDistance(dist);
                                                                    restaurantsList.add(user);
                                                                } else if (restaurantName.toLowerCase().equals(searchValue.toLowerCase())) {
                                                                    user.setDistance(dist);
                                                                    restaurantsList.add(user);
                                                                }

                                                                Log.d("Restaurants", "added: " + user.getFirstname());
                                                            }
                                                        }

                                                        if (!restaurantsList.isEmpty()) {
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            Log.d("Restaurants", "list: [" + restaurantsList.size() + "]");
                                                            recyclerview.setVisibility(VISIBLE);
                                                            /**
                                                             * https://howtodoinjava.com/sort/collections-sort/
                                                             * We want to sort from nearest to furthest location
                                                             */
                                                            //Collections.sort(restaurantsList, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                            //Collections.reverse(list);
                                                            RestaurantAdapter recycler = new RestaurantAdapter(getContext(), restaurantsList);
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            //recyclerView.setItemAnimator(new SlideInLeftAnimator());
                                                            recycler.notifyDataSetChanged();
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.GONE);
                                                        } else {
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            recyclerview.setVisibility(INVISIBLE);
                                                            emptyTag.setVisibility(VISIBLE);
                                                            recyclerview.setVisibility(View.GONE);
                                                            emptyTag.setText("Nothing found");
                                                        }
                                                    } catch (Exception e){
                                                        Log.e("SearchActivity", "onDataChange: ", e);
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

                                                        String restaurantName = user.getFirstname() + " " + user.getLastname();
                                                        if (!myPhone.equals(user.getPhone())) {
                                                            if(restaurantsList.size() < searchCap){
                                                                if (restaurantName.toLowerCase().contains(searchValue.toLowerCase())) {
                                                                    user.setDistance(dist);
                                                                    restaurantsList.add(user);
                                                                } else if (searchValue.toLowerCase().contains(restaurantName.toLowerCase())) {
                                                                    user.setDistance(dist);
                                                                    restaurantsList.add(user);
                                                                }
                                                                //search if word is equal to user name object
                                                                else if (searchValue.toLowerCase() == restaurantName.toLowerCase()) {
                                                                    user.setDistance(dist);
                                                                    restaurantsList.add(user);
                                                                } else if (restaurantName.toLowerCase().equals(searchValue.toLowerCase())) {
                                                                    user.setDistance(dist);
                                                                    restaurantsList.add(user);
                                                                }

                                                                Log.d("Restaurants", "added: " + user.getFirstname());
                                                            }
                                                        }

                                                        if (!restaurantsList.isEmpty()) {
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            Log.d("Restaurants", "list: [" + restaurantsList.size() + "]");
                                                            recyclerview.setVisibility(VISIBLE);
                                                            /**
                                                             * https://howtodoinjava.com/sort/collections-sort/
                                                             * We want to sort from nearest to furthest location
                                                             */
                                                            //Collections.sort(restaurantsList, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                            //Collections.reverse(list);
                                                            RestaurantAdapter recycler = new RestaurantAdapter(getContext(), restaurantsList);
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recycler.notifyDataSetChanged();
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.INVISIBLE);
                                                        } else {
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            recyclerview.setVisibility(INVISIBLE);
                                                            emptyTag.setVisibility(VISIBLE);
                                                            recyclerview.setVisibility(View.GONE);
                                                            emptyTag.setText("Nothing found");
                                                        }
                                                    } catch (Exception e){
                                                        Log.e("SearchActivity", "onDataChange: ", e);
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
    }

    public static boolean isStringNullOrWhiteSpace(String value) {
        if (value == null) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRefresh() {
        searchVendors(searchValue);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(view != null){
            view = null;

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
        }
    }
}
