package com.malcolmmaima.dishi.View.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.malcolmmaima.dishi.Model.ExplorePostsModel;
import com.malcolmmaima.dishi.View.Activities.WPPostDetails;
import com.malcolmmaima.dishi.R;

import java.util.ArrayList;

public class ExplorePostAdapter extends RecyclerView.Adapter {

    private ArrayList<ExplorePostsModel> dataset;
    private Context mContext;
    public ExplorePostAdapter(ArrayList<ExplorePostsModel> mlist, Context context) {
        this.dataset = mlist;
        this.mContext = context;
    }

    public static class ImageTypeViewHolder extends RecyclerView.ViewHolder{


        TextView title, subtitle;
        ImageView imageView;
        public ImageTypeViewHolder(View itemView) {
            super(itemView);

            this.title = (TextView)  itemView.findViewById(R.id.title);
            this.subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            this.imageView = (ImageView) itemView.findViewById(R.id.Icon);
        }
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from( parent.getContext()).inflate(R.layout.postdetails, parent, false);
        return new ImageTypeViewHolder(view) ;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final ExplorePostsModel object = dataset.get(position);

        ( (ImageTypeViewHolder) holder).title.setText( object.title );
        ( (ImageTypeViewHolder) holder).subtitle.setText( object.subtitle );

        ( (ImageTypeViewHolder) holder).title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WPPostDetails.class);
                intent.putExtra("itemPosition", position);
                mContext.startActivity(intent);
            }
        });
        ( (ImageTypeViewHolder) holder).subtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WPPostDetails.class);
                intent.putExtra("itemPosition", position);
                mContext.startActivity(intent);
            }
        });
        ( (ImageTypeViewHolder) holder).imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WPPostDetails.class);
                intent.putExtra("itemPosition", position);
                mContext.startActivity(intent);
            }
        });

        /// dataset.get(position)
    }

    @Override
    public int getItemCount() {
        return dataset.size() ;
    }
}
