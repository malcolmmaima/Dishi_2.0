package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.malcolmmaima.dishi.Model.ExplorePostsModel;
import com.malcolmmaima.dishi.View.Activities.WPPostDetails;
import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;

import java.util.ArrayList;

public class ExplorePostAdapter extends RecyclerView.Adapter {

    String TAG = "ExplorePostAdapter";
    private ArrayList<ExplorePostsModel> dataset;
    private Context mContext;
    long DURATION = 200;

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
        final View view = LayoutInflater.from( parent.getContext()).inflate(R.layout.postdetails, parent, false);
        return new ImageTypeViewHolder(view) ;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final ExplorePostsModel object = dataset.get(position);
        setAnimation(holder.itemView, position);
        ((ImageTypeViewHolder) holder).subtitle.setText(object.subtitle);
        ( (ImageTypeViewHolder) holder).title.setText( object.title );

        //Strip any raw html
        try {
            String plainSubtitle = new HtmlToPlainText().getPlainText(Jsoup.parse(object.subtitle));
            String plainTitle = new HtmlToPlainText().getPlainText(Jsoup.parse(object.title));

            ((ImageTypeViewHolder) holder).subtitle.setText(plainSubtitle);
            ( (ImageTypeViewHolder) holder).title.setText(plainTitle);
        } catch (Exception e){
            Log.e(TAG, "onBindViewHolder: ", e);
        }

        try {
            Log.d(TAG, "onBindViewHolder: " + object.Image);
            Picasso.with(mContext).load(object.Image).fit().centerCrop()
                    .placeholder(R.drawable.blog_post)
                    .error(R.drawable.blog_post)
                    .into(((ImageTypeViewHolder) holder).imageView);
        } catch (Exception e){
            Log.e(TAG, "onBindViewHolder: ", e);
        }

        ( (ImageTypeViewHolder) holder).title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WPPostDetails.class);
                intent.putExtra("title", object.title);
                intent.putExtra("url", dataset.get(position).url);
                mContext.startActivity(intent);
            }
        });
        ( (ImageTypeViewHolder) holder).subtitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WPPostDetails.class);
                intent.putExtra("title", object.title);
                intent.putExtra("url", dataset.get(position).url);
                mContext.startActivity(intent);
            }
        });
        ( (ImageTypeViewHolder) holder).imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WPPostDetails.class);
                intent.putExtra("title", object.title);
                intent.putExtra("url", dataset.get(position).url);
                mContext.startActivity(intent);
            }
        });

        /// dataset.get(position)
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
        return dataset.size() ;
    }
}
