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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.AddMenu;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.squareup.picasso.Picasso;

import java.util.List;


public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MyHolder>{

    Context context;
    List<ProductDetailsModel> listdata;
    long DURATION = 200;

    public MenuAdapter(Context context, List<ProductDetailsModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public MenuAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_menu,parent,false);

        MenuAdapter.MyHolder myHolder = new MenuAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final MenuAdapter.MyHolder holder, final int position) {
        final ProductDetailsModel productDetailsModel = listdata.get(position);

        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.foodPrice.setText("Ksh "+ productDetailsModel.getPrice());
        holder.foodName.setText(productDetailsModel.getName());

        if(productDetailsModel.getDescription().length() > 89) {
            holder.foodDescription.setText(productDetailsModel.getDescription().substring(0, 80) + "...");
        } else {
            holder.foodDescription.setText(productDetailsModel.getDescription());
        }

        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(context, AddMenu.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("key", productDetailsModel.getKey());
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                context.startActivity(slideactivity, bndlanimation);
            }
        });

        /**
         * View image click listener
         */
        holder.foodPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(productDetailsModel.getImageUrlBig() != null){
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", productDetailsModel.getImageUrlBig());
                    context.startActivity(slideactivity);
                }

                else {
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", productDetailsModel.getImageURL());
                    context.startActivity(slideactivity);
                }

            }
        });

        /**
         * Load image url onto imageview
         */
        try {
            //Load food image
            if(productDetailsModel.getImageUrlSmall() != null){
                Picasso.with(context).load(productDetailsModel.getImageUrlSmall()).fit().centerCrop()
                        .placeholder(R.drawable.menu)
                        .error(R.drawable.menu)
                        .into(holder.foodPic);
            }

            else { //for older versions of the app or if something went wrong during image upload then use original image
                Picasso.with(context).load(productDetailsModel.getImageURL()).fit().centerCrop()
                        .placeholder(R.drawable.menu)
                        .error(R.drawable.menu)
                        .into(holder.foodPic);
            }


        } catch (Exception e){

        }

    }

    /**
     * @lhttps://medium.com/better-programming/android-recyclerview-with-beautiful-animations-5e9b34dbb0fa
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
        MyTextView_Roboto_Medium foodPrice , foodName;
        MyTextView_Roboto_Regular foodDescription;
        ImageView foodPic;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            foodName = itemView.findViewById(R.id.foodName);
            foodDescription = itemView.findViewById(R.id.foodDescription);
            foodPic = itemView.findViewById(R.id.foodPic);
            cardView = itemView.findViewById(R.id.card_view);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            final String myPhone = user.getPhoneNumber(); //Current logged in user phone number
            final DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference("menus/" + myPhone);

            //Long Press
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    //Delete Menu item

                    final AlertDialog deleteMenu = new AlertDialog.Builder(context)
                            .setMessage("Delete " + foodName.getText().toString())
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String key = listdata.remove(getAdapterPosition()).getKey();

                                    menuRef.child(key).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            try {
                                                notifyItemRemoved(getAdapterPosition());
                                                Snackbar.make(v.getRootView(), "Deleted", Snackbar.LENGTH_LONG).show();
                                            } catch (Exception e){}
                                        }
                                    });

                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                }
                            })
                            .create();
                    deleteMenu.show();
                    return false;
                }
            });

        }
    }


}