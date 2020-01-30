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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.MenuAdapter;
import com.malcolmmaima.dishi.View.Adapter.ProductAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FragmentFood extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    List<ProductDetails> list;
    RecyclerView recyclerview;
    String myPhone;
    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    LiveLocation liveLocation;

    DatabaseReference dbRef, menusRef, myLocationRef;
    FirebaseDatabase db;
    FirebaseUser user;


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
        final View v = inflater.inflate(R.layout.fragment_food, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference("users/"+myPhone);
        myLocationRef = db.getReference("location/"+myPhone);
        menusRef = db.getReference("menus");

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
         * On create view fetch my location coordinates
         */

        liveLocation = null;
        myLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                liveLocation = dataSnapshot.getValue(LiveLocation.class);
                //Toast.makeText(getContext(), "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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
                            final UserModel user = dataSnapshot.getValue(UserModel.class);
//                            Toast.makeText(getContext(), "Name: " + user.getFirstname()
//                                    + "\nliveStatus: " + user.getLiveStatus()
//                                    + "\nlocationType: " + user.getLocationType(), Toast.LENGTH_SHORT).show();

                            /**
                             * Check "liveStatus" of each restautant (must be true so as to allow menu to be fetched
                             */
                            if(user.getLiveStatus() == true){

                                /**
                                 * Now check "locationType" so as to decide which location node to fetch, live or static
                                 */
                                if(user.getLocationType().equals("default")){
                                    //if location type is default then fetch static location
                                    DatabaseReference defaultLocation = FirebaseDatabase.getInstance().getReference("users/"+restaurants.getKey()+"/my_location");

                                    defaultLocation.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            StaticLocation staticLocation = dataSnapshot.getValue(StaticLocation.class);
//                                            Toast.makeText(getContext(), restaurants.getKey() + ": "
//                                                    + staticLocation.getLatitude() + ","
//                                                    + staticLocation.getLongitude(), Toast.LENGTH_SHORT).show();

                                            /**
                                             * Now lets compute distance of each restaurant with customer location
                                             */
                                            CalculateDistance calculateDistance = new CalculateDistance();
                                            Double dist = calculateDistance.distance(liveLocation.getLatitude(),
                                                    liveLocation.getLongitude(), staticLocation.getLatitude(), staticLocation.getLongitude(), "K");

                                            //Toast.makeText(getContext(), restaurants.getKey() + ": " + dist + "km", Toast.LENGTH_SHORT).show();

                                            /**
                                             * if distance meets parameters set fetch menu
                                             */

                                            if(dist < 10.0){
                                                //Fetch menu items of restaurants that have passed distance parameter

                                                for(DataSnapshot menu : restaurants.getChildren()){
                                                    //Toast.makeText(getContext(), restaurants.getKey()+": "+ menu.getKey(), Toast.LENGTH_SHORT).show();
                                                    ProductDetails product = menu.getValue(ProductDetails.class);
                                                    product.setKey(menu.getKey());
                                                    product.setDistance(dist);
                                                    list.add(product);
                                                }
                                            }

                                            if(!list.isEmpty()){

                                                mSwipeRefreshLayout.setRefreshing(false);
                                                Collections.reverse(list);
                                                ProductAdapter recycler = new ProductAdapter(getContext(),list);
                                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                recyclerview.setLayoutManager(layoutmanager);
                                                recyclerview.setItemAnimator( new DefaultItemAnimator());

                                                recycler.notifyDataSetChanged();

                                                recyclerview.getItemAnimator().setAddDuration(200);
                                                recyclerview.getItemAnimator().setRemoveDuration(200);
                                                recyclerview.getItemAnimator().setMoveDuration(200);
                                                recyclerview.getItemAnimator().setChangeDuration(200);

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
}