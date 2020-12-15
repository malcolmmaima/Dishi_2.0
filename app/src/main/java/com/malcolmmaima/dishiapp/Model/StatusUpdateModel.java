package com.malcolmmaima.dishiapp.Model;

public class StatusUpdateModel {

    public String status;
    public String timePosted;
    public String key;
    public String author;
    public String postedTo;
    public String commentKey;
    public String imageShare;
    public String imageShareSmall;
    public String imageShareMedium;
    public String imageShareBig;
    public String receiptKey; // temporary holder for passing receipt key for status FoodShare post types
    public  String vendorPhone; //same as above
    public String type; //temporary data holder we'll use in 'search posts' we want to determin if NewsFeedAdapter is triggered from FragmenSearchPosts
    //so that we set # to unclickable to take care of redundant behaviour

    public String getCommentKey() {
        return commentKey;
    }

    public void setCommentKey(String commentKey) {
        this.commentKey = commentKey;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setTimePosted(String timePosted) {
        this.timePosted = timePosted;
    }

    public String getTimePosted() {
        return timePosted;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setPostedTo(String postedTo) {
        this.postedTo = postedTo;
    }

    public String getPostedTo() {
        return postedTo;
    }

    public String getImageShare() {
        return imageShare;
    }

    public void setImageShare(String imageShare) {
        this.imageShare = imageShare;
    }

    public String getReceiptKey() {
        return receiptKey;
    }

    public void setReceiptKey(String receiptKey) {
        this.receiptKey = receiptKey;
    }

    public String getVendorPhone() {
        return vendorPhone;
    }

    public void setVendorPhone(String vendorPhone) {
        this.vendorPhone = vendorPhone;
    }

    public String getImageShareSmall() {
        return imageShareSmall;
    }

    public void setImageShareSmall(String imageShareSmall) {
        this.imageShareSmall = imageShareSmall;
    }

    public String getImageShareMedium() {
        return imageShareMedium;
    }

    public void setImageShareMedium(String imageShareMedium) {
        this.imageShareMedium = imageShareMedium;
    }

    public String getImageShareBig() {
        return imageShareBig;
    }

    public void setImageShareBig(String imageShareBig) {
        this.imageShareBig = imageShareBig;
    }
}
