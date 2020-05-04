package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.malcolmmaima.dishi.Controller.Interface.OnOrderChecked;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ViewOrderAdapter extends RecyclerView.Adapter<ViewOrderAdapter.MyHolder>{

    Context context;
    List<ProductDetailsModel> listdata;
    long DURATION = 200;

    OnOrderChecked onOrderChecked;

    public ViewOrderAdapter(Context context, List<ProductDetailsModel> listdata, OnOrderChecked onOrderChecked) {
        this.listdata = listdata;
        this.context = context;
        this.onOrderChecked = onOrderChecked;
    }

    @Override
    public ViewOrderAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_order_adapter,parent,false);

        ViewOrderAdapter.MyHolder myHolder = new ViewOrderAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final ViewOrderAdapter.MyHolder holder, final int position) {
        final ProductDetailsModel productDetailsModel = listdata.get(position);

        if(productDetailsModel.getConfirmed() == true){
            holder.checkBox.setChecked(true);
            holder.checkBox.setEnabled(false);
        }

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        int price = productDetailsModel.getQuantity() * Integer.parseInt(productDetailsModel.getPrice());
        holder.foodPrice.setText("Ksh "+price);
        holder.foodName.setText(productDetailsModel.getName());

        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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

        if(!productDetailsModel.accountType.equals("2")){
            holder.checkBox.setEnabled(false);
        }
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onOrderChecked.onItemChecked(isChecked, position, productDetailsModel.getPrice(), productDetailsModel.getQuantity());
            }
        });

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
        TextView foodPrice, foodName, quantity;
        ImageView foodPic;
        CardView cardView;
        CheckBox checkBox;

        public MyHolder(View itemView) {
            super(itemView);
            foodPrice = itemView.findViewById(R.id.foodPrice);
            foodName = itemView.findViewById(R.id.foodName);
            foodPic = itemView.findViewById(R.id.foodPic);
            cardView = itemView.findViewById(R.id.card_view);
            quantity = itemView.findViewById(R.id.quantity);
            checkBox = itemView.findViewById(R.id.checkBox);

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