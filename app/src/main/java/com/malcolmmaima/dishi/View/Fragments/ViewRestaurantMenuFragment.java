package com.malcolmmaima.dishi.View.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocationModel;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewRestaurantMenuFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    List<ProductDetailsModel> list;
    RecyclerView recyclerview;
    String myPhone, fullName, phone;
    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;

    DatabaseReference menusRef, myFavourites, restaurantRef, myRef, myLocationRef, restaurantLocationRef;
    ValueEventListener myRefListener, mylocationListener, restaurantLocationListener;
    FirebaseDatabase db;
    FirebaseUser user;

    Double dist, distance;
    UserModel userModel;
    LiveLocationModel myLocation, restaurantLocation;
    View v;

    public static ViewRestaurantMenuFragment newInstance() {
        ViewRestaurantMenuFragment fragment = new ViewRestaurantMenuFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_view_restaurant_menu, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();

        phone = getArguments().getString("phone");
        fullName = getArguments().getString("fullName");
        menusRef = db.getReference("menus/"+ phone);

        restaurantRef = db.getReference( "restaurant_favourites/"+ phone);
        myFavourites = db.getReference("my_restaurant_favourites/"+myPhone);
        myRef = db.getReference("users/"+myPhone);

        myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
        restaurantLocationRef = FirebaseDatabase.getInstance().getReference("location/"+phone);

        //Get my location coordinates
        mylocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    myLocation = dataSnapshot.getValue(LiveLocationModel.class);
                     distance = computeDistance(myLocation.getLatitude(), myLocation.getLongitude(), restaurantLocation.getLatitude(), restaurantLocation.getLongitude(), "K");
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myLocationRef.addValueEventListener(mylocationListener);

        //get restaurant location coordinates
        restaurantLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    restaurantLocation = dataSnapshot.getValue(LiveLocationModel.class);
                    distance = computeDistance(myLocation.getLatitude(), myLocation.getLongitude(), restaurantLocation.getLatitude(), restaurantLocation.getLongitude(), "K");
                }catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        restaurantLocationRef.addValueEventListener(restaurantLocationListener);

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        //recyclerview.setNestedScrollingEnabled(false);
        emptyTag = v.findViewById(R.id.empty_tag);

        //Get my user details
        myRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    userModel = dataSnapshot.getValue(UserModel.class);
                }catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRef.addValueEventListener(myRefListener);

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
                fetchMenu();

            }
        });

        return  v;
    }

    private Double computeDistance(Double latitude, Double longitude, Double latitude1, Double longitude1, String k) {
        CalculateDistance calculateDistance = new CalculateDistance();
        dist = calculateDistance.distance(latitude, longitude, latitude1, longitude1, "K");

        return dist;
    }

    @Override
    public void onRefresh() {
        fetchMenu();
    }


    private void fetchMenu() {
        menusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                list = new ArrayList<>();
                int listSize = list.size(); //Bug fix, kept on refreshing menu on data change due to realtime location data.
                //Will use this to determine if the list of menu items has changed, only refresh then

                // StringBuffer stringbuffer = new StringBuffer();
                for(DataSnapshot dataSnapshot1 :dataSnapshot.getChildren()){
                    ProductDetailsModel productDetailsModel = dataSnapshot1.getValue(ProductDetailsModel.class); //Assign values to model
                    productDetailsModel.setKey(dataSnapshot1.getKey()); //Get item keys, useful when performing delete operations
                    productDetailsModel.setDistance(distance);
                    productDetailsModel.accountType = userModel.getAccount_type();
                    list.add(productDetailsModel);
                    //progressDialog.dismiss();
                }

                if(!list.isEmpty() && list.size() > listSize){

                    mSwipeRefreshLayout.setRefreshing(false);
                    Collections.reverse(list);
                    ProductAdapter recycler = new ProductAdapter(getContext(),list);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recycler.notifyDataSetChanged();
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.INVISIBLE);
                    icon.setVisibility(View.INVISIBLE);
                }

                else {

                    mSwipeRefreshLayout.setRefreshing(false);

                    ProductAdapter recycler = new ProductAdapter(getContext(),list);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator( new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                //  Log.w(TAG, "Failed to read value.", error.toException());

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(v != null){
            v = null;

            recyclerview.setAdapter(null);
            myRef.removeEventListener(myRefListener);
            myLocationRef.removeEventListener(mylocationListener);
            restaurantLocationRef.removeEventListener(restaurantLocationListener);
        }
    }
}