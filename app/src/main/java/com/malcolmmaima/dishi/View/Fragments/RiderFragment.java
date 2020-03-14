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

import java.util.ArrayList;
import java.util.List;

public class RiderFragment extends Fragment {
    List<UserModel> AssignedOrders = new ArrayList<>();
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference myRestaurantsRef;
    ValueEventListener myRestaurantsRefListener;
    FirebaseDatabase db;
    FirebaseUser user;

    DatabaseReference[] ordersRef;
    ValueEventListener [] ordersRefListener;

    TextView emptyTag;
    AppCompatImageView icon;
    List <String> myRestaurants = new ArrayList<>();


    public static RiderFragment newInstance() {
        RiderFragment fragment = new RiderFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_rider, container, false);
        progressDialog = new ProgressDialog(getContext());

        AssignedOrders.clear();



        icon = v.findViewById(R.id.menuIcon);
        recyclerview = v.findViewById(R.id.rview);
        emptyTag = v.findViewById(R.id.empty_tag);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();

        myRestaurantsRef = db.getReference("my_restaurants/"+myPhone);

        /**
         * Loop through my restaurants and find restaurants I am a member of
         */

         myRestaurantsRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot restaurantPhone) {

                ordersRef = new DatabaseReference[(int) restaurantPhone.getChildrenCount()];
                ordersRefListener  = new ValueEventListener[(int) restaurantPhone.getChildrenCount()];
                myRestaurants.clear();
                for(DataSnapshot x : restaurantPhone.getChildren()){
                    myRestaurants.add(x.getKey()); //Add my restaurants to a list
                }

                try {
                    for(int i=0; i<myRestaurants.size(); i++){
                        ordersRef[i] = FirebaseDatabase.getInstance().getReference("orders/"+myRestaurants.get(i));
                        ordersRefListener[i] = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot orders : dataSnapshot.getChildren()){
                                    if(orders.child("rider").exists() && orders.child("rider").getValue().equals(myPhone)){
                                        Toast.makeText(getContext(), "assigned: " + dataSnapshot.getKey(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        };
                        ordersRef[i].addValueEventListener(ordersRefListener[i]);
                    }
                } catch (Exception e){

                }


                /**
                 * Loop through restaurant's order to find out if I have been assigned any order
                 */
//                    ordersRefListener = new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull final DataSnapshot customer) {
//
//                            for(final DataSnapshot order_ : customer.getChildren()){
//                                Toast.makeText(getContext(), "Order: " + orders.getKey(), Toast.LENGTH_SHORT).show();
//
//                                //Toast.makeText(getContext(), "customer: " +order_.getKey(), Toast.LENGTH_SHORT).show();
//                                DatabaseReference userDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+orders.getKey());
//                                ValueEventListener
//                                userDetailsRefListener = new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(@NonNull DataSnapshot userDetails) {
//                                        if(order_.child("rider").exists()){
//                                            if(order_.child("rider").getValue().equals(myPhone)){
//                                                UserModel assignedCustomer = userDetails.getValue(UserModel.class);
//                                                assignedCustomer.setPhone(order_.getKey());
//                                                assignedCustomer.itemCount = 0;
//                                                assignedCustomer.restaurantPhone = orders.getKey();
//                                                AssignedOrders.add(assignedCustomer);
//
//                                                Toast.makeText(getContext(), "user: "+ assignedCustomer.getPhone(), Toast.LENGTH_SHORT).show();
//
//                                                if (!AssignedOrders.isEmpty()) {
//                                                    //Collections.reverse(orders);
//                                                    OrdersAdapter recycler = new OrdersAdapter(getContext(), AssignedOrders);
//                                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
//                                                    recyclerview.setLayoutManager(layoutmanager);
//                                                    recyclerview.setItemAnimator(new DefaultItemAnimator());
//
//                                                    recycler.notifyDataSetChanged();
//
//                                                    recyclerview.getItemAnimator().setAddDuration(200);
//                                                    recyclerview.getItemAnimator().setRemoveDuration(200);
//                                                    recyclerview.getItemAnimator().setMoveDuration(200);
//                                                    recyclerview.getItemAnimator().setChangeDuration(200);
//
//                                                    recyclerview.setAdapter(recycler);
//                                                    emptyTag.setVisibility(View.INVISIBLE);
//                                                    icon.setVisibility(View.INVISIBLE);
//                                                } else {
////                                        progressDialog.dismiss();
//                                                    OrdersAdapter recycler = new OrdersAdapter(getContext(), AssignedOrders);
//                                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
//                                                    recyclerview.setLayoutManager(layoutmanager);
//                                                    recyclerview.setItemAnimator(new DefaultItemAnimator());
//                                                    recyclerview.setAdapter(recycler);
//                                                    emptyTag.setVisibility(View.VISIBLE);
//                                                    icon.setVisibility(View.VISIBLE);
//
//                                                }
//                                            }
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                                    }
//                                };
//                                userDetailsRef.addListenerForSingleValueEvent(userDetailsRefListener);
//
//                            }
//
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                        }
//                    };
//                    ordersRef.addValueEventListener(ordersRefListener);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
         myRestaurantsRef.addListenerForSingleValueEvent(myRestaurantsRefListener);




        return  v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            myRestaurantsRef.removeEventListener(myRestaurantsRefListener);
        } catch (Exception e){

        }
    }
}