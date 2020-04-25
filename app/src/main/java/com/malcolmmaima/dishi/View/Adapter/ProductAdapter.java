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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
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

import io.fabric.sdk.android.services.common.SafeToast;


public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.MyHolder>{

    Context context;
    List<ProductDetails> listdata;
    long DURATION = 200;

    public ProductAdapter(Context context, List<ProductDetails> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public ProductAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_product,parent,false);

        ProductAdapter.MyHolder myHolder = new ProductAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final ProductAdapter.MyHolder holder, final int position) {
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

        try {
            if (productDetails.getDescription().length() > 89) {
                holder.foodDescription.setText(productDetails.getDescription().substring(0, 80) + "...");
            } else {
                holder.foodDescription.setText(productDetails.getDescription());
            }
        } catch (Exception e){

        }

        holder.productOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //creating a popup menu
                PopupMenu popup = new PopupMenu(context, holder.productOptions);
                //inflating menu from xml resource
                popup.inflate(R.menu.product_options_menu);
                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.favourite:
                                //handle menu1 click
                                Toast.makeText(context, "clicked option 1", Toast.LENGTH_SHORT).show();
                                return true;
                            case R.id.report:
                                //handle menu2 click
                                Toast.makeText(context, "clicked option 2", Toast.LENGTH_SHORT).show();
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                //displaying the popup
                popup.show();

            }
        });

        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(context, ViewProduct.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("key", productDetails.getKey());
                slideactivity.putExtra("restaurant", productDetails.getOwner());
                slideactivity.putExtra("restaurantName", holder.restaurantName.getText());
                slideactivity.putExtra("product", productDetails.getName());
                slideactivity.putExtra("description", productDetails.getDescription());
                slideactivity.putExtra("price", productDetails.getPrice());
                slideactivity.putExtra("imageUrl", productDetails.getImageURL());
                slideactivity.putExtra("distance", productDetails.getDistance());
                slideactivity.putExtra("accType", productDetails.accountType);

                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                context.startActivity(slideactivity, bndlanimation);
            }
        });

        /**
         * Add item to cart
         */

        if(!productDetails.accountType.equals("1")){
            holder.addToCart.setVisibility(View.GONE);
        }

        holder.addToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DatabaseReference menuExistRef = FirebaseDatabase.getInstance()
                        .getReference("menus/"+productDetails.getOwner()+"/"+productDetails.getKey());
                ValueEventListener existsListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists()){ //Does not exist!

                            SafeToast.makeText(context, "Item no longer exists", Toast.LENGTH_LONG).show();
                        }

                        else {
                            try {
                                Snackbar.make(v.getRootView(), "Adding...", Snackbar.LENGTH_LONG).show();
                            } catch(Exception e){

                            }

                            String myPhone;
                            myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
                            DatabaseReference myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);

                            //Get current date
                            GetCurrentDate currentDate = new GetCurrentDate();
                            String cartDate = currentDate.getDate();

                            String key = myCartRef.push().getKey();
                            ProductDetails cartProduct = new ProductDetails();
                            cartProduct.setName(productDetails.getName());
                            cartProduct.setPrice(productDetails.getPrice());
                            cartProduct.setDescription(productDetails.getDescription());
                            cartProduct.setImageURL(productDetails.getImageURL());
                            cartProduct.setOwner(productDetails.getOwner());
                            cartProduct.setOriginalKey(productDetails.getKey());
                            cartProduct.setQuantity(1);
                            cartProduct.setDistance(productDetails.getDistance());
                            cartProduct.setUploadDate(cartDate);

                            myCartRef.child(key).setValue(cartProduct).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    try {
                                        Snackbar.make(v.getRootView(), "Added to cart", Snackbar.LENGTH_LONG).show();
                                    } catch (Exception e){

                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };
                menuExistRef.addListenerForSingleValueEvent(existsListener);

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

        if(productDetails.getDistance() < 1.0){
            holder.distanceAway.setText(productDetails.getDistance()*1000 + "m away");
        } else {
            holder.distanceAway.setText(productDetails.getDistance() + "km away");

        }

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
        TextView foodPrice, foodDescription, foodName, restaurantName,distanceAway, productOptions;
        ImageView foodPic;
        CardView cardView;
        ImageButton addToCart;

        public MyHolder(View itemView) {
            super(itemView);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            foodName = itemView.findViewById(R.id.foodName);
            foodDescription = itemView.findViewById(R.id.foodDescription);
            foodPic = itemView.findViewById(R.id.foodPic);
            cardView = itemView.findViewById(R.id.card_view);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            distanceAway = itemView.findViewById(R.id.distanceAway);
            addToCart = itemView.findViewById(R.id.addToCart);
            productOptions = itemView.findViewById(R.id.productOptions);

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