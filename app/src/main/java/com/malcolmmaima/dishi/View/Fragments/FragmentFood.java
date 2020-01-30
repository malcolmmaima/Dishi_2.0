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
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;

import java.util.List;

public class FragmentFood extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    List<ProductDetails> list;
    RecyclerView recyclerview;
    String myPhone;
    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;

    DatabaseReference dbRef, menusRef;
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
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mSwipeRefreshLayout.setRefreshing(false);

                for(final DataSnapshot restaurants : dataSnapshot.getChildren()){

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
                                            Toast.makeText(getContext(), restaurants.getKey() + ": " + staticLocation.getLatitude() + ","+ staticLocation.getLongitude(), Toast.LENGTH_SHORT).show();
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