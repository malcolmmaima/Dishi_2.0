package com.malcolmmaima.dishi.View.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
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
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.StaticLocationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ProductHistoryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class HistoryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    String TAG = "HistoryFragment";
    List<ProductDetailsModel> list;
    RecyclerView recyclerview;
    String myPhone;
    TextView emptyTag, clearAll;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LiveLocationModel liveLocationModel;

    DatabaseReference dbRef, ordersHistory, myLocationRef;
    FirebaseDatabase db;
    FirebaseUser user;
    ValueEventListener locationListener;

    public static HistoryFragment newInstance() {
        HistoryFragment fragment = new HistoryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myLocationRef.removeEventListener(locationListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_history, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference("users/"+myPhone);
        myLocationRef = db.getReference("location/"+myPhone);
        ordersHistory = db.getReference("orders_history/"+myPhone);

        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);
        clearAll = v.findViewById(R.id.clearAll);
        clearAll.setVisibility(View.GONE);

        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog deletePost = new AlertDialog.Builder(v.getContext())
                        //set message, title, and icon
                        .setMessage("Clear all?")
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                DatabaseReference itemDetails = FirebaseDatabase.getInstance().getReference("orders_history/"+myPhone);
                                itemDetails.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        fetchFood();
                                    }
                                });
                            }
                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do nothing

                            }
                        })//setNegativeButton

                        .create();
                deletePost.show();
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
                liveLocationModel = dataSnapshot.getValue(LiveLocationModel.class);
                //SafeToast.makeText(getContext(), "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();
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
        ordersHistory.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot datasnapshot) {

                list = new ArrayList<>();
                if(!datasnapshot.exists()){
                    clearAll.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setRefreshing(false);
                    ProductHistoryAdapter recycler = new ProductHistoryAdapter(getContext(), list);
                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                    recyclerview.setLayoutManager(layoutmanager);
                    recyclerview.setItemAnimator(new DefaultItemAnimator());
                    recyclerview.setAdapter(recycler);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                } else {
                    for(final DataSnapshot products : datasnapshot.getChildren()){

                        /**
                         * Create new database reference for each restaurant and fetch user data
                         */
                        DatabaseReference userData = FirebaseDatabase.getInstance().getReference("users/"+ products.child("owner").getValue());
                        userData.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                final UserModel user = dataSnapshot.getValue(UserModel.class);
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
                                            DatabaseReference defaultLocation = FirebaseDatabase.getInstance().getReference("users/" + products.child("owner").getValue() + "/my_location");

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

                                                        ProductDetailsModel product = products.getValue(ProductDetailsModel.class);
                                                        //product.setKey(products.getKey());
                                                        product.setDistance(dist);
                                                        product.accountType = "1"; //This fragment belongs to account type 1 (customer)
                                                        list.add(product);

                                                        if (!list.isEmpty()) {
                                                            clearAll.setVisibility(View.VISIBLE);
                                                            clearAll.setText("CLEAR ALL (" + list.size() + ")");
                                                            /**
                                                             * https://howtodoinjava.com/sort/collections-sort/
                                                             * We want to sort from nearest to furthest location
                                                             */
                                                            //Sort by distance to restaurant offering the particular food item... from closest to furthest
                                                            //Collections.sort(list, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            //Collections.reverse(list); //Filter by order it appears in db.. collect then reverse

                                                            try {
                                                                //filter by time ordered ... from most recent to oldest
                                                                Collections.sort(list, (item1, item2) -> (item2.getUploadDate().compareTo(item1.getUploadDate())));
                                                            } catch (Exception e) {
                                                            }
                                                            ProductHistoryAdapter recycler = new ProductHistoryAdapter(getContext(), list);
                                                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recycler.notifyDataSetChanged();
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.INVISIBLE);
                                                            icon.setVisibility(View.INVISIBLE);
                                                        } else {

                                                            clearAll.setVisibility(View.GONE);
                                                            mSwipeRefreshLayout.setRefreshing(false);

                                                            ProductHistoryAdapter recycler = new ProductHistoryAdapter(getContext(), list);
                                                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.VISIBLE);
                                                            icon.setVisibility(View.VISIBLE);

                                                        }
                                                    } catch (Exception e){
                                                        Log.e("HistoryFragment", "onDataChange: ", e);
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
                                            DatabaseReference restliveLocation = FirebaseDatabase.getInstance().getReference("location/" + products.child("owner").getValue());

                                            restliveLocation.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                    try {
                                                    LiveLocationModel restLiveLoc = dataSnapshot.getValue(LiveLocationModel.class);

                                                    /**
                                                     * Now lets compute distance of each restaurant with customer location
                                                     */
                                                    try {
                                                        CalculateDistance calculateDistance = new CalculateDistance();
                                                        Double dist = calculateDistance.distance(liveLocationModel.getLatitude(),
                                                                liveLocationModel.getLongitude(), restLiveLoc.getLatitude(), restLiveLoc.getLongitude(), "K");

                                                        ProductDetailsModel product = products.getValue(ProductDetailsModel.class);
                                                        //product.setKey(products.getKey());
                                                        product.setDistance(dist);
                                                        product.accountType = "1"; //this fragment belongs to account type 1
                                                        list.add(product);

                                                        if (!list.isEmpty()) {
                                                            clearAll.setVisibility(View.VISIBLE);
                                                            clearAll.setText("CLEAR ALL (" + list.size() + ")");
                                                            /**
                                                             * https://howtodoinjava.com/sort/collections-sort/
                                                             * We want to sort from nearest to furthest location
                                                             */
                                                            //Collections.sort(list, (bo1, bo2) -> (bo1.getDistance() > bo2.getDistance() ? 1 : -1));
                                                            mSwipeRefreshLayout.setRefreshing(false);
                                                            //Collections.reverse(list);

                                                            try {
                                                                Collections.sort(list, (item1, item2) -> (item2.getUploadDate().compareTo(item1.getUploadDate())));
                                                            } catch (Exception e) {
                                                            }
                                                            ProductHistoryAdapter recycler = new ProductHistoryAdapter(getContext(), list);
                                                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                            recyclerview.setLayoutManager(layoutmanager);
                                                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                                                            recycler.notifyDataSetChanged();
                                                            recyclerview.setAdapter(recycler);
                                                            emptyTag.setVisibility(View.INVISIBLE);
                                                            icon.setVisibility(View.INVISIBLE);
                                                        } else {
                                                            clearAll.setVisibility(View.GONE);
                                                            mSwipeRefreshLayout.setRefreshing(false);

                                                            ProductHistoryAdapter recycler = new ProductHistoryAdapter(getContext(), list);
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRefresh() {
        fetchFood();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        myLocationRef.removeEventListener(locationListener);
    }
}