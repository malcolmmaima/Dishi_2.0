package com.malcolmmaima.dishi.View.Fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.alexzh.circleimageview.CircleImageView;
import com.google.common.collect.Range;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference myRef;
    ValueEventListener myListener;
    FirebaseUser user;

    CircleImageView profilePhoto;
    TextView profileName, profileBio, following, followers;
    ImageButton emoji;
    EmojiconEditText myStatusUpdate;
    View rootView;
    EmojIconActions emojIcon;
    Button postBtn;
    UserModel myUserDetails;

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_user_profile, container, false);
        rootView = v.findViewById(R.id.activity_main);
        profilePhoto = v.findViewById(R.id.user_profile_photo);
        profileName = v.findViewById(R.id.user_profile_name);
        profileBio = v.findViewById(R.id.user_profile_short_bio);
        following = v.findViewById(R.id.following);
        followers = v.findViewById(R.id.followers);
        recyclerview = v.findViewById(R.id.rview);

        emoji = v.findViewById(R.id.emoji);
        myStatusUpdate = v.findViewById(R.id.myStatus);
        postBtn = v.findViewById(R.id.postStatus);
        postBtn.setVisibility(View.GONE);
        emoji.setVisibility(View.GONE);


        progressDialog = new ProgressDialog(getContext());

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

        myStatusUpdate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    emoji.setVisibility(View.VISIBLE);
                    postBtn.setVisibility(View.VISIBLE);
                }
                else {

                    if(myStatusUpdate.getText().toString().length() > 1){
                        postBtn.setVisibility(View.VISIBLE);
                        emoji.setVisibility(View.VISIBLE);
                    } else {
                        postBtn.setVisibility(View.GONE);
                        emoji.setVisibility(View.GONE);
                    }

                }
            }
        });

        emojIcon = new EmojIconActions(getContext(), rootView, myStatusUpdate, emoji);
        emojIcon.setUseSystemEmoji(false); //if we set this to true then the default emojis for chat wil be the system emojis
        emojIcon.setIconsIds(R.drawable.ic_action_keyboard, R.drawable.smiley);
        emojIcon.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {
                Log.e(TAG, "Keyboard opened!");
            }

            @Override
            public void onKeyboardClose() {
                emojIcon.closeEmojIcon();
                Log.e(TAG, "Keyboard closed");
            }
        });

        myListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    myUserDetails = dataSnapshot.getValue(UserModel.class);
                    profileName.setText(myUserDetails.getFirstname() + " " + myUserDetails.getLastname());
                    profileBio.setText(myUserDetails.getBio());

                    Double totalFollowers = Double.valueOf(myUserDetails.getFollowers());
                    Double totalFollowing = Double.valueOf(myUserDetails.getFollowing());
                    /**
                     * Followers counter
                     */

                    //below 1000
                    if(totalFollowers < 1000){
                        DecimalFormat value = new DecimalFormat("#");
                        followers.setText(""+value.format(totalFollowers));
                    }

                    // 1000 to 999,999
                    else if(totalFollowers >= 1000 && totalFollowers <= 999999){
                        if(totalFollowers % 1000 == 0){ //No remainder
                            DecimalFormat value = new DecimalFormat("#####");
                            followers.setText(""+value.format(totalFollowers/1000)+"K");
                        }

                        else { //Has remainder 999.9K
                            DecimalFormat value = new DecimalFormat("######.#");
                            Double divided = totalFollowers/1000;
                            if(value.format(divided).equals("1000")){
                                followers.setText("1M"); //if rounded off
                            } else {
                                followers.setText(""+value.format(divided)+"K");
                            }
                        }
                    }

                    // 1,000,0000 to 999,999,999
                    else if(totalFollowers >= 1000000 && totalFollowers <= 999999999){
                        if(totalFollowers % 1000000 == 0) { //No remainder
                            DecimalFormat value = new DecimalFormat("#");
                            followers.setText(""+value.format(totalFollowers/1000000)+"M");
                        }

                        else { //Has remainder 9.9M, 999.9M etc
                            DecimalFormat value = new DecimalFormat("#.#");
                            if(value.format(totalFollowers/1000000).equals("1000")){
                                followers.setText("1B"); //if rounded off
                            } else {
                                followers.setText(""+value.format(totalFollowers/1000000)+"M");
                            }
                        }
                    }

                    /**
                     * Following counter
                     */
                    //below 1000
                    if(totalFollowing < 1000){
                        DecimalFormat value = new DecimalFormat("#");
                        following.setText(""+value.format(totalFollowing));
                    }

                    // 1000 to 999,999
                    else if(totalFollowing >= 1000 && totalFollowing <= 999999){
                        if(totalFollowing % 1000 == 0){ //No remainder
                            DecimalFormat value = new DecimalFormat("#####");
                            following.setText(""+value.format(totalFollowing/1000)+"K");
                        }

                        else { //Has remainder 999.9K
                            DecimalFormat value = new DecimalFormat("######.#");
                            Double divided = totalFollowing/1000;
                            if(value.format(divided).equals("1000")){
                                following.setText("1M"); //if rounded off
                            } else {
                                following.setText(""+value.format(divided)+"K");
                            }
                        }
                    }

                    // 1,000,0000 to 999,999,999
                    else if(totalFollowing >= 1000000 && totalFollowing <= 999999999){
                        if(totalFollowing % 1000000 == 0) { //No remainder
                            DecimalFormat value = new DecimalFormat("#");
                            following.setText(""+value.format(totalFollowing/1000000)+"M");
                        }

                        else { //Has remainder 9.9M, 999.9M etc
                            DecimalFormat value = new DecimalFormat("#.#");
                            if(value.format(totalFollowing/1000000).equals("1000")){
                                following.setText("1B"); //if rounded off
                            } else {
                                following.setText(""+value.format(totalFollowing/1000000)+"M");
                            }
                        }
                    }



                } catch (Exception e){}

                try {
                    //Load food image
                    Picasso.with(getContext()).load(myUserDetails.getProfilePic()).fit().centerCrop()
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profilePhoto);
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRef.addValueEventListener(myListener);

        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(getContext(), myStatusUpdate.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });

        emoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojIcon.ShowEmojIcon();
            }
        });

        profilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent slideactivity = new Intent(getContext(), ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", myUserDetails.getProfilePic());
                    startActivity(slideactivity);
                } catch (Exception e){}
            }
        });
        return  v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myRef.removeEventListener(myListener);
    }
}