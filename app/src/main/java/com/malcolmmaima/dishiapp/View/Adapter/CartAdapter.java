package com.malcolmmaima.dishiapp.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
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
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Model.ProductDetailsModel;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Activities.SearchActivity;
import com.malcolmmaima.dishiapp.View.Activities.ViewImage;
import com.malcolmmaima.dishiapp.View.Activities.ViewProduct;
import com.squareup.picasso.Picasso;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.util.List;


public class CartAdapter extends RecyclerView.Adapter<CartAdapter.MyHolder>{

    Context context;
    List<ProductDetailsModel> listdata;
    long DURATION = 200;
    String TAG = "CartAdapter";
    HashTagHelper mTextHashTagHelper;

    public CartAdapter(Context context, List<ProductDetailsModel> listdata) {
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
        final ProductDetailsModel productDetailsModel = listdata.get(position);

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.checkBox.setEnabled(false);
        holder.checkBox.setVisibility(View.GONE);
        holder.outOfStock.setVisibility(View.GONE);

        int price = productDetailsModel.getQuantity() * Integer.parseInt(productDetailsModel.getPrice());
        holder.foodPrice.setText("Ksh "+price);
        holder.foodName.setText(productDetailsModel.getName());

        //Fetch restaurant user details
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users/"+ productDetailsModel.getOwner());
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


        //Check item status from vendor on whether item is out of stock or nah
        DatabaseReference menuItemRef = FirebaseDatabase.getInstance().getReference("menus/"+productDetailsModel.getOwner()+"/"+productDetailsModel.getOriginalKey());
        menuItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    try {
                        ProductDetailsModel menuProduct = dataSnapshot.getValue(ProductDetailsModel.class);
                        try {
                            if (menuProduct.getOutOfStock() == true) {
                                productDetailsModel.setOutOfStock(true);
                                holder.checkBox.setVisibility(View.VISIBLE);
                                holder.checkBox.setChecked(true);
                                holder.outOfStock.setVisibility(View.VISIBLE);
                            } else {
                                productDetailsModel.setOutOfStock(false);
                                holder.checkBox.setVisibility(View.GONE);
                                holder.outOfStock.setVisibility(View.GONE);
                            }
                        } catch (Exception e) {

                        }
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        try {
            if (productDetailsModel.getDescription().length() > 89) {
                holder.foodDescription.setText(productDetailsModel.getDescription().substring(0, 80) + "...");
            } else {
                holder.foodDescription.setText(productDetailsModel.getDescription());
            }
        } catch (Exception e){
            Log.e(TAG, "onBindViewHolder: ", e);
        }

        try {
            Linkify.addLinks(holder.foodDescription, Linkify.ALL);
        } catch (Exception e){
            Log.e(TAG, "onDataChange: ", e);
        }

        //handle hashtags
        if(productDetailsModel.getDescription().contains("#")){
            mTextHashTagHelper = HashTagHelper.Creator.create(context.getResources().getColor(R.color.colorPrimary),
                    new HashTagHelper.OnHashTagClickListener() {
                        @Override
                        public void onHashTagClicked(String hashTag) {
                            String searchHashTag = "#"+hashTag;
                            Intent slideactivity = new Intent(context, SearchActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            slideactivity.putExtra("searchString", searchHashTag);
                            slideactivity.putExtra("goToFragment", 1);
                            context.startActivity(slideactivity);
                        }
                    });

            mTextHashTagHelper.handle(holder.foodDescription);
        }

        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent slideactivity = new Intent(context, ViewProduct.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("key", productDetailsModel.getOriginalKey());
                    slideactivity.putExtra("restaurant", productDetailsModel.getOwner());
                    slideactivity.putExtra("restaurantName", holder.restaurantName.getText());
                    slideactivity.putExtra("product", productDetailsModel.getName());
                    slideactivity.putExtra("description", productDetailsModel.getDescription());
                    slideactivity.putExtra("price", productDetailsModel.getPrice());
                    slideactivity.putExtra("imageUrl", productDetailsModel.getImageURL());
                    slideactivity.putExtra("distance", productDetailsModel.getDistance());
                    slideactivity.putExtra("accType", "1"); // this cart adapter is ony accessible to customers when they view their cart
                    slideactivity.putExtra("outOfStock", productDetailsModel.getOutOfStock());

                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation, R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                }
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

                myCartRef.child(productDetailsModel.getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        try {
                            //Comment out since i changed to a live listener in MyCart
//                            listdata.remove(holder.getAdapterPosition());
//                            notifyItemRemoved(holder.getAdapterPosition());
                            Snackbar.make(v.getRootView(), "Deleted", Snackbar.LENGTH_LONG).show();
                        } catch(Exception e){
                            Snackbar.make(v.getRootView(), "Something went wrong!", Snackbar.LENGTH_LONG).show();
                        }
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
                slideactivity.putExtra("imageURL", productDetailsModel.getImageURL());
                context.startActivity(slideactivity);
            }
        });

        holder.quantity.setText("Quantity: "+ productDetailsModel.getQuantity() + " x Ksh "+ productDetailsModel.getPrice());

        /**
         * Load image url onto imageview
         */
        try {
            //Load food image
            Picasso.with(context).load(productDetailsModel.getImageURL()).fit().centerCrop()
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

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        MyTextView_Roboto_Medium foodName, foodPrice;
        MyTextView_Roboto_Regular foodDescription, restaurantName,quantity, outOfStock;
        ImageView foodPic;
        CardView cardView;
        ImageButton removeCart;
        CheckBox checkBox;

        public MyHolder(View itemView) {
            super(itemView);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            foodName = itemView.findViewById(R.id.foodName);
            foodDescription = itemView.findViewById(R.id.foodDescription);
            foodPic = itemView.findViewById(R.id.foodPic);
            cardView = itemView.findViewById(R.id.card_view);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            quantity = itemView.findViewById(R.id.quantity);
            removeCart = itemView.findViewById(R.id.removeCart);
            checkBox = itemView.findViewById(R.id.checkBox);
            outOfStock = itemView.findViewById(R.id.outOfStock);

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