package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.GetCurrentDate;
import com.malcolmmaima.dishi.Model.ProductDetails;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.AddMenu;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProduct;
import com.squareup.picasso.Picasso;

import java.util.List;


public class CartAdapter extends RecyclerView.Adapter<CartAdapter.MyHolder>{

    Context context;
    List<ProductDetails> listdata;
    long DURATION = 200;
    private boolean on_attach = true;

    public CartAdapter(Context context, List<ProductDetails> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public CartAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_cart,parent,false);

        CartAdapter.MyHolder myHolder = new CartAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final CartAdapter.MyHolder holder, final int position) {
        final ProductDetails productDetails = listdata.get(position);

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.foodPrice.setText("Ksh "+productDetails.getPrice());
        holder.foodName.setText(productDetails.getName());

        //Fetch restaurant user details
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users/"+productDetails.getOwner());
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    UserModel user = dataSnapshot.getValue(UserModel.class);
                    String fullName = user.getFirstname() + " " + user.getLastname();
                    holder.restaurantName.setText(fullName);
                } catch (Exception e){
                    holder.restaurantName.setText("error fetching...");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(productDetails.getDescription().length() > 89) {
            holder.foodDescription.setText(productDetails.getDescription().substring(0, 80) + "...");
        } else {
            holder.foodDescription.setText(productDetails.getDescription());
        }

        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        /**
         * Remove item from cart
         */
        holder.removeCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Snackbar.make(v.getRootView(), "Deleting...", Snackbar.LENGTH_LONG).show();

                String myPhone;
                myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
                DatabaseReference myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);

                myCartRef.child(productDetails.getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        listdata.remove(holder.getAdapterPosition());
                        notifyItemRemoved(holder.getAdapterPosition());
                        Snackbar.make(v.getRootView(), "Deleted", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });

        /**
         * View image click listener
         */
        holder.foodPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(context, ViewImage.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("imageURL", productDetails.getImageURL());
                context.startActivity(slideactivity);
            }
        });

//        if(productDetails.getDistance() < 1.0){
//            holder.distanceAway.setText(productDetails.getDistance()*1000 + "m away");
//        } else {
//            holder.distanceAway.setText(productDetails.getDistance() + "km away");
//
//        }

        /**
         * Load image url onto imageview
         */
        try {
            //Load food image
            Picasso.with(context).load(productDetails.getImageURL()).fit().centerCrop()
                    .placeholder(R.drawable.menu)
                    .error(R.drawable.menu)
                    .into(holder.foodPic);
        } catch (Exception e){

        }

    }

    /**
     * @https://medium.com/better-programming/android-recyclerview-with-beautiful-animations-5e9b34dbb0fa
     */
    private void setAnimation(View itemView, int i) {
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

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        TextView foodPrice, foodDescription, foodName, restaurantName,distanceAway;
        ImageView foodPic;
        CardView cardView;
        ImageButton removeCart;

        public MyHolder(View itemView) {
            super(itemView);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            foodName = itemView.findViewById(R.id.foodName);
            foodDescription = itemView.findViewById(R.id.foodDescription);
            foodPic = itemView.findViewById(R.id.foodPic);
            cardView = itemView.findViewById(R.id.card_view);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            distanceAway = itemView.findViewById(R.id.distanceAway);
            removeCart = itemView.findViewById(R.id.removeCart);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            final String myPhone = user.getPhoneNumber(); //Current logged in user phone number

            //Long Press
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    return false;
                }
            });

        }
    }


}