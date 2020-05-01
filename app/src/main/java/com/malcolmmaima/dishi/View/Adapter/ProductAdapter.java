package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ReportAbuse;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProduct;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;


public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.MyHolder>{
    String TAG = "ProductAdapter";

    Context context;
    List<ProductDetailsModel> listdata;
    long DURATION = 200;

    public ProductAdapter(Context context, List<ProductDetailsModel> listdata) {
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
        final ProductDetailsModel productDetailsModel = listdata.get(position);
        int[] clickCount = new int[listdata.size()];
        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.foodPrice.setText("Ksh "+ productDetailsModel.getPrice());
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

        try {
            if (productDetailsModel.getDescription().length() > 89) {
                holder.foodDescription.setText(productDetailsModel.getDescription().substring(0, 80) + "...");
            } else {
                holder.foodDescription.setText(productDetailsModel.getDescription());
            }
        } catch (Exception e){

        }

        FirebaseUser  user = FirebaseAuth.getInstance().getCurrentUser();
        String myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //creating a popup menu
        PopupMenu popup = new PopupMenu(context, holder.productOptions);
        //inflating menu from xml resource
        popup.inflate(R.menu.product_options_menu);

        Menu myMenu = popup.getMenu();
        MenuItem favouriteOption = myMenu.findItem(R.id.favourite);
        MenuItem removeFavourite = myMenu.findItem(R.id.removefavourite);
        MenuItem reportOption = myMenu.findItem(R.id.report);

        try {
            reportOption.setVisible(true);
        } catch (Exception e){}

        DatabaseReference myFoodFavourites = FirebaseDatabase.getInstance().getReference("my_food_favourites/"+myPhone);
        DatabaseReference myUserDetails = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

        myUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    UserModel user = dataSnapshot.getValue(UserModel.class);
                    //Hide these two options if account type of logged in user is not 1 (customer)
                    if(!user.getAccount_type().equals("1")){
                        removeFavourite.setVisible(false);
                        favouriteOption.setVisible(false);
                    } else {
                        myFoodFavourites.child(productDetailsModel.getOwner()).child(productDetailsModel.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    try {
                                        removeFavourite.setVisible(true);
                                        favouriteOption.setVisible(false);
                                    } catch (Exception e){}
                                }
                                else {
                                    try {
                                        removeFavourite.setVisible(false);
                                        favouriteOption.setVisible(true);
                                    } catch (Exception e){}
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }

                } catch (Exception e){
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        holder.productOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.favourite:

                                //First lets check if the restaurant still has this particular product in their menu
                                DatabaseReference menuExistRef = FirebaseDatabase.getInstance().getReference("menus/"+ productDetailsModel.getOwner()+"/"+ productDetailsModel.getKey());
                                menuExistRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(!dataSnapshot.exists()){ //Does not exist!
                                            Snackbar.make(view.getRootView(), "No longer exists", Snackbar.LENGTH_LONG).show();
                                        } else {
                                            //Add to my favourites
                                            DatabaseReference myFoodFavourites = FirebaseDatabase.getInstance().getReference("my_food_favourites/"+myPhone);
                                            myFoodFavourites.child(productDetailsModel.getOwner()).child(productDetailsModel.getKey()).setValue("fav").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //Add to global restaurant likes
                                                    DatabaseReference favouritesTotalRef = FirebaseDatabase.getInstance().getReference("menus/"+ productDetailsModel.getOwner()+"/"+ productDetailsModel.getKey()+"/likes");
                                                    favouritesTotalRef.child(myPhone).setValue("fav").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            try {
                                                                removeFavourite.setVisible(true);
                                                                favouriteOption.setVisible(false);
                                                            } catch (Exception e){}
                                                            Snackbar.make(view.getRootView(), "Added", Snackbar.LENGTH_LONG).show();
                                                        }
                                                    });
                                                    //SafeToast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                                return true;

                            case R.id.removefavourite:
                                ////Remove from my favourites
                                myFoodFavourites.child(productDetailsModel.getOwner()).child(productDetailsModel.getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        DatabaseReference favouritesTotalRef = FirebaseDatabase.getInstance().getReference("menus/"+ productDetailsModel.getOwner()+"/"+ productDetailsModel.getKey()+"/likes");
                                        favouritesTotalRef.child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //remove favourite from restaurant's node as well
                                                try {
                                                    removeFavourite.setVisible(false);
                                                    favouriteOption.setVisible(true);
                                                } catch (Exception e){}
                                                Snackbar.make(view.getRootView(), "Removed", Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                });
                                return (true);
                            case R.id.report:
                                final AlertDialog reportProduct = new AlertDialog.Builder(context)
                                        //set message, title, and icon
                                        .setMessage("Report "+ productDetailsModel.getName()+"?")
                                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                        //set three option buttons
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                Intent slideactivity = new Intent(context, ReportAbuse.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                slideactivity.putExtra("type", "product");
                                                slideactivity.putExtra("owner", productDetailsModel.getOwner());
                                                slideactivity.putExtra("productKey", productDetailsModel.getKey());
                                                context.startActivity(slideactivity);
                                            }
                                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                //do nothing
                                            }
                                        })//setNegativeButton

                                        .create();
                                reportProduct.show();
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
                slideactivity.putExtra("key", productDetailsModel.getKey());
                slideactivity.putExtra("restaurant", productDetailsModel.getOwner());
                slideactivity.putExtra("restaurantName", holder.restaurantName.getText());
                slideactivity.putExtra("product", productDetailsModel.getName());
                slideactivity.putExtra("description", productDetailsModel.getDescription());
                slideactivity.putExtra("price", productDetailsModel.getPrice());
                slideactivity.putExtra("imageUrl", productDetailsModel.getImageURL());
                slideactivity.putExtra("distance", productDetailsModel.getDistance());
                slideactivity.putExtra("accType", productDetailsModel.accountType);

                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                context.startActivity(slideactivity, bndlanimation);
            }
        });

        /**
         * Add item to cart
         */

        if(!productDetailsModel.accountType.equals("1")){
            holder.addToCart.setVisibility(View.GONE);
        }

        clickCount[position] = 0;
        holder.addToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                clickCount[position]++;
                //Clearly this user wants to add multiple items of the same, take them to view product so they can add as many as they want
                if(clickCount[position] > 2){
                    clickCount[position] = 0; //reset back to zero
                    Intent slideactivity = new Intent(context, ViewProduct.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("key", productDetailsModel.getKey());
                    slideactivity.putExtra("restaurant", productDetailsModel.getOwner());
                    slideactivity.putExtra("restaurantName", holder.restaurantName.getText());
                    slideactivity.putExtra("product", productDetailsModel.getName());
                    slideactivity.putExtra("description", productDetailsModel.getDescription());
                    slideactivity.putExtra("price", productDetailsModel.getPrice());
                    slideactivity.putExtra("imageUrl", productDetailsModel.getImageURL());
                    slideactivity.putExtra("distance", productDetailsModel.getDistance());
                    slideactivity.putExtra("accType", productDetailsModel.accountType);

                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                    SafeToast.makeText(context, "Please add multiple from here", Toast.LENGTH_LONG).show();
                } else {
                    DatabaseReference menuExistRef = FirebaseDatabase.getInstance()
                            .getReference("menus/"+ productDetailsModel.getOwner()+"/"+ productDetailsModel.getKey());
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

                                //Check to see if this item already exists in cart, if yes, increment quantity
                                DatabaseReference cartItemExistsRef = FirebaseDatabase
                                        .getInstance().getReference("cart/"+myPhone);
                                cartItemExistsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(!dataSnapshot.hasChildren()){
                                            //cart is empty, add a fresh
                                            String myPhone;
                                            myPhone = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber(); //Current logged in user phone number
                                            DatabaseReference myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);

                                            //Get current date
                                            GetCurrentDate currentDate = new GetCurrentDate();
                                            String cartDate = currentDate.getDate();

                                            String key = myCartRef.push().getKey();
                                            ProductDetailsModel cartProduct = new ProductDetailsModel();
                                            cartProduct.setName(productDetailsModel.getName());
                                            cartProduct.setPrice(productDetailsModel.getPrice());
                                            cartProduct.setDescription(productDetailsModel.getDescription());
                                            cartProduct.setImageURL(productDetailsModel.getImageURL());
                                            cartProduct.setOwner(productDetailsModel.getOwner());
                                            cartProduct.setOriginalKey(productDetailsModel.getKey());
                                            cartProduct.setQuantity(1);
                                            cartProduct.setDistance(productDetailsModel.getDistance());
                                            cartProduct.setUploadDate(cartDate);

                                            myCartRef.child(key).setValue(cartProduct).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    try {
                                                        Snackbar.make(v.getRootView(), "Added to cart", Snackbar.LENGTH_LONG).show();
                                                    } catch (Exception e){

                                                    }
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    try {
                                                        Snackbar.make(v.getRootView(), "Something went wrong", Snackbar.LENGTH_LONG).show();
                                                    } catch(Exception er){
                                                        Log.e(TAG, "onFailure: ", er);
                                                    }
                                                }
                                            });
                                        } else {

                                            //Get current date
                                            GetCurrentDate currentDate = new GetCurrentDate();
                                            String cartDate = currentDate.getDate();

                                            String key = cartItemExistsRef.push().getKey();
                                            ProductDetailsModel cartProduct = new ProductDetailsModel();
                                            cartProduct.setName(productDetailsModel.getName());
                                            cartProduct.setPrice(productDetailsModel.getPrice());
                                            cartProduct.setDescription(productDetailsModel.getDescription());
                                            cartProduct.setImageURL(productDetailsModel.getImageURL());
                                            cartProduct.setOwner(productDetailsModel.getOwner());
                                            cartProduct.setOriginalKey(productDetailsModel.getKey());
                                            cartProduct.setQuantity(1);
                                            cartProduct.setDistance(productDetailsModel.getDistance());
                                            cartProduct.setUploadDate(cartDate);

                                            cartItemExistsRef.child(key).setValue(cartProduct).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    try {
                                                        Snackbar.make(v.getRootView(), "Added to cart", Snackbar.LENGTH_LONG).show();
                                                    } catch (Exception e){

                                                    }

                                                    for(DataSnapshot cartItem : dataSnapshot.getChildren()){
                                                        ProductDetailsModel cartProd = cartItem.getValue(ProductDetailsModel.class);

                                                        //Item already exists in cart, increment quantity
                                                        if(cartProd.getOriginalKey().equals(productDetailsModel.getKey())){
                                                            int currItemQuantity = cartProd.getQuantity();
                                                            currItemQuantity = currItemQuantity + 1;
                                                            int finalCurrItemQuantity = currItemQuantity;
                                                            cartProd.setQuantity(finalCurrItemQuantity);
                                                            cartItemExistsRef.child(key).setValue(cartProd).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    cartItemExistsRef.child(cartItem.getKey()).removeValue();
                                                                    try {
                                                                        Toast.makeText(context, cartProd.getName()+": "+ finalCurrItemQuantity, Toast.LENGTH_SHORT).show();
                                                                        //Snackbar.make(v.getRootView(), cartProd.getName()+": "+ finalCurrItemQuantity, Snackbar.LENGTH_LONG).show();
                                                                    } catch(Exception e){
                                                                        Log.e(TAG, "onSuccess: ", e);
                                                                    }
                                                                }
                                                            }).addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    try {
                                                                        Snackbar.make(v.getRootView(), "Something went wrong", Snackbar.LENGTH_LONG).show();
                                                                    } catch(Exception er){
                                                                        Log.e(TAG, "onFailure: ", er);
                                                                    }
                                                                }
                                                            });
                                                            //Log.d(TAG, "old qty: "+cartProd.getQuantity()+" new qty "+ currItemQuantity);
                                                        }
                                                    }
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    try {
                                                        Snackbar.make(v.getRootView(), "Something went wrong", Snackbar.LENGTH_LONG).show();
                                                    } catch(Exception er){
                                                        Log.e(TAG, "onFailure: ", er);
                                                    }
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

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };
                    menuExistRef.addListenerForSingleValueEvent(existsListener);
                }
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

        if(productDetailsModel.getDistance() < 1.0){
            holder.distanceAway.setText(productDetailsModel.getDistance()*1000 + "m away");
        } else {
            holder.distanceAway.setText(productDetailsModel.getDistance() + "km away");

        }

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