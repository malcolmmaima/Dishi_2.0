package com.malcolmmaima.dishi.View.Adapter;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.R;

import java.util.List;


public class ReceiptItemAdapter extends RecyclerView.Adapter<ReceiptItemAdapter.MyViewHolder> {

    Context context;
    String TAG = "ReceiptItemAdapter";

    private List<ProductDetailsModel> OfferList;

    public class MyViewHolder extends RecyclerView.ViewHolder {



        MyTextView_Roboto_Regular iteamName,quantity,price;

        public MyViewHolder(View view) {
            super(view);
            iteamName= view.findViewById(R.id.iteamName);
            quantity= view.findViewById(R.id.quantity);
            price= view.findViewById(R.id.price);
        }

    }


    public ReceiptItemAdapter(Context mainActivityContacts, List<ProductDetailsModel> offerList) {
        this.OfferList = offerList;
        this.context = mainActivityContacts;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_receipt_item, parent, false);


        return new MyViewHolder(itemView);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        ProductDetailsModel lists = OfferList.get(position);

        holder.iteamName.setText(lists.getName());
        holder.quantity.setText(""+lists.getQuantity());
        holder.price.setText(lists.getPrice());

    }

    @Override
    public int getItemCount() {
        return OfferList.size();

    }

}
