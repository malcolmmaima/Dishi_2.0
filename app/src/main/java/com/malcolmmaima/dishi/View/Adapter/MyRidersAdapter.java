package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MyRidersAdapter extends RecyclerView.Adapter<MyRidersAdapter.MyHolder>{
    Context context;
    List<UserModel> listdata;
    long DURATION = 200;
    DatabaseReference myRidersRef;
    String myPhone;
    FirebaseUser user;

    public MyRidersAdapter(Context context, List<UserModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public MyRidersAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_my_rider,parent,false);

        MyRidersAdapter.MyHolder myHolder = new MyRidersAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final MyRidersAdapter.MyHolder holder, final int position) {
        final UserModel orderDetails = listdata.get(position);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        myRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+myPhone);

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.riderName.setText(orderDetails.getFirstname() + " " + orderDetails.getLastname());


        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Load rider details/options", Toast.LENGTH_SHORT).show();
            }
        });


        /**
         * View image click listener
         */
        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(context, ViewImage.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("imageURL", orderDetails.getProfilePic());
                context.startActivity(slideactivity);
            }
        });


        /**
         * Load image url onto imageview
         */
        try {
            //Load food image
            Picasso.with(context).load(orderDetails.getProfilePic()).fit().centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.profilePic);
        } catch (Exception e){

        }

    }

    /**
     * @https://medium.com/better-programming/android-recyclerview-with-beautiful-animations-5e9b34dbb0fa
     */
    private void setAnimation(View itemView, int i) {
        boolean on_attach = true;
        if(!on_attach){
            i = -1;
        }
        boolean isNotFirstItem = i == -1;
        i++;
        itemView.setAlpha(0.f);
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator animator = ObjectAnimator.ofFloat(itemView, "alpha", 0.f, 0.5f, 1.0f);
        ObjectAnimator.ofFloat(itemView, "alpha", 0.f).start();
        animator.setStartDelay(isNotFirstItem ? DURATION / 2 : (i * DURATION / 3));
        animator.setDuration(500);
        animatorSet.play(animator);
        animator.start();
    }

    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        TextView riderName;
        ImageView profilePic;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);
            riderName = itemView.findViewById(R.id.riderName);
            profilePic = itemView.findViewById(R.id.profilePic);
            cardView = itemView.findViewById(R.id.card_view);


            //Long Press
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final AlertDialog addRider = new AlertDialog.Builder(v.getContext())
                            //set message, title, and icon
                            .setMessage("Remove " + riderName.getText().toString() + "?")
                            //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                            //set three option buttons
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    myRidersRef.child(listdata.get(getAdapterPosition()).getPhone()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            listdata.remove(getAdapterPosition());
                                            notifyItemRemoved(getAdapterPosition());
                                        }
                                    });
                                }
                            }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //do nothing

                                }
                            })//setNegativeButton

                            .create();
                    addRider.show();
                    return false;
                }
            });

        }
    }

}
