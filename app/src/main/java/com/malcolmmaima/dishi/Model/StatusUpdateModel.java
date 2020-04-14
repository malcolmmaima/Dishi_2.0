package com.malcolmmaima.dishi.Model;

public class StatusUpdateModel {

    public String status;
    public String timePosted;
    public String key;
    public String author;
    public String postedTo;
    public String currentWall;
    public String commentKey;
    public String image;


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

    public String getCurrentWall() {
        return currentWall;
    }

    public void setCurrentWall(String currentWall) {
        this.currentWall = currentWall;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
