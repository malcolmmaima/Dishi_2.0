package com.malcolmmaima.dishi.Model;

public class NotificationModel {
    public String key;
    public String from;
    public String type;
    public String image;
    public String message;
    public String timeStamp;
    public Boolean seen;
    public String author; //This applies to status updates
    public String postedTo; //This applies to status updates
    public String statusKey;

    public String getStatusKey() {
        return statusKey;
    }

    public void setStatusKey(String statusKey) {
        this.statusKey = statusKey;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public String getPostedTo() {
        return postedTo;
    }

    public void setPostedTo(String postedTo) {
        this.postedTo = postedTo;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }
}
