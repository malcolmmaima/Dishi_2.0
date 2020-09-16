package com.malcolmmaima.dishi.View.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.SearchActivity;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.List;



import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


public class FragmentSearchFood extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    String TAG = "FragmentSearchFood";
    RecyclerView recyclerview;
    MyTextView_Roboto_Regular emptyTag;
    SwipeRefreshLayout mSwipeRefreshLayout;
    String searchValue, myPhone;
    FirebaseUser user;
    UserModel myDetails;
    LiveLocationModel liveLocationModel;
    ValueEventListener locationListener;
    DatabaseReference myLocationRef, myUserDetails;
    int searchCap;
    View view;
    LinearLayoutManager layoutmanager;

    public static FragmentSearchFood newInstance() {
        FragmentSearchFood fragment = new FragmentSearchFood();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_search_food, container, false);

        searchCap = 100;
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber();
        //String searchString;
        //searchString = getArguments().getString("search");

        myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);

        liveLocationModel = null;
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    liveLocationModel = dataSnapshot.getValue(LiveLocationModel.class);
                } catch (Exception e){

                }
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
                try {
                    myDetails = dataSnapshot.getValue(UserModel.class);
                    myDetails.setPhone(dataSnapshot.getKey());
                } catch (Exception e){

                }
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

                searchFood(searchValue);
            }
        });

        return  view;
    }

    private void searchFood(String searchValue) {
        if(isStringNullOrWhiteSpace(searchValue)){
            emptyTag.setText("Type something");
            emptyTag.setVisibility(VISIBLE);
        } else {
            mSwipeRefreshLayout.setRefreshing(true);
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("menus").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot datasnapshot) {

                    List<ProductDetailsModel> foods = new ArrayList<>();
                    for(final DataSnapshot restaurants : datasnapshot.getChildren()){

                        /**
                         * Create new database reference for each restaurant and fetch user data
                         */
                        DatabaseReference userData = FirebaseDatabase.getInstance().getReference("users/"+ restaurants.getKey());
                        userData.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                final UserModel user = dataSnapshot.getValue(UserModel.class);
//                            Toast.makeText(getContext(), "Name: " + user.getFirstname()
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

                                                        //Toast.makeText(getContext(), restaurants.getKey() + ": " + dist + "km", Toast.LENGTH_SHORT).show();

                                                        for (DataSnapshot menu : restaurants.getChildren()) {
                                                            //Toast.makeText(getContext(), restaurants.getKey()+": "+ menu.getKey(), Toast.LENGTH_SHORT).show();
                                                            ProductDetailsModel product = menu.getValue(ProductDetailsModel.class);
                                                            product.setKey(menu.getKey());
                                                            product.setDistance(dist);
                                                            product.accountType = myDetails.getAccount_type();

                                                            if(foods.size() < searchCap){
//                                                                //Don't show my menu items in the search
//                                                                if (!myPhone.equals(product.getOwner())) {
//                                                                    //simply move the below code here
//                                                                }

                                                                String searchParams = product.getName()+" "+product.getDescription();
                                                                if (searchParams.toLowerCase().contains(searchValue.toLowerCase())) {
                                                                    foods.add(product);
                                                                } else if (searchValue.toLowerCase().contains(product.getName().toLowerCase())) {
                                                                    foods.add(product);
                                                                }
                                                                //search if word is equal to user name object
                                                                else if (searchValue.toLowerCase() == searchParams) {
                                                                    foods.add(product);
                                                                } else if (searchParams.toLowerCase().equals(searchValue.toLowerCase())) {
                                                                    foods.add(product);
                                                                }
                                                            }
                                                        }

                                                        if (!foods.isEmpty()) {
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            //Collections.sort(foods, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                            recyclerview.setVisibility(View.VISIBLE);

                                                            ProductAdapter recycler = new ProductAdapter(getContext(), foods);
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
                                                    /**
                                                     * Now lets compute distance of each restaurant with customer location
                                                     */
                                                    try {
                                                        LiveLocationModel restLiveLoc = dataSnapshot.getValue(LiveLocationModel.class);
                                                        CalculateDistance calculateDistance = new CalculateDistance();
                                                        Double dist = calculateDistance.distance(liveLocationModel.getLatitude(),
                                                                liveLocationModel.getLongitude(), restLiveLoc.getLatitude(), restLiveLoc.getLongitude(), "K");

                                                        //Toast.makeText(getContext(), restaurants.getKey() + ": " + dist + "km", Toast.LENGTH_SHORT).show();

                                                        for (DataSnapshot menu : restaurants.getChildren()) {
                                                            //Toast.makeText(getContext(), restaurants.getKey()+": "+ menu.getKey(), Toast.LENGTH_SHORT).show();
                                                            ProductDetailsModel product = menu.getValue(ProductDetailsModel.class);
                                                            product.setKey(menu.getKey());
                                                            product.setDistance(dist);
                                                            product.accountType = myDetails.getAccount_type();

                                                            if(foods.size() < searchCap){
                                                                //Don't show my menu items in the search
//                                                                if(!myPhone.equals(product.getOwner())){
//
//                                                                }
                                                                String searchParams = product.getName()+" "+product.getDescription();
                                                                if (searchParams.toLowerCase().contains(searchValue.toLowerCase())) {
                                                                    foods.add(product);
                                                                }
                                                                else if(searchValue.toLowerCase().contains(searchParams.toLowerCase())){
                                                                    foods.add(product);
                                                                }
                                                                //search if word is equal to user name object
                                                                else if(searchValue.toLowerCase() == searchParams.toLowerCase()){
                                                                    foods.add(product);
                                                                }
                                                                else if(searchParams.toLowerCase().equals(searchValue.toLowerCase())) {
                                                                    foods.add(product);
                                                                }
                                                            }

                                                        }

                                                        if (!foods.isEmpty()) {
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            //Collections.sort(foods, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                            recyclerview.setVisibility(View.VISIBLE);
                                                            recyclerview.setVisibility(VISIBLE);
                                                            ProductAdapter recycler = new ProductAdapter(getContext(), foods);
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
                                                        Log.e(TAG, "onDataChange: ", e);
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
                                    Log.e(TAG, "onDataChange: ", e);
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
        searchFood(searchValue);
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

            try {
                myLocationRef.removeEventListener(locationListener);
            } catch (Exception e){
                Log.e(TAG, "onDestroy: ", e);
            }
        }
    }

}
