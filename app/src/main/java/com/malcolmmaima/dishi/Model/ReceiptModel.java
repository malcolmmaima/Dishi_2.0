package com.malcolmmaima.dishi.Model;

public class ReceiptModel {
    public String key;
    public String initiatedOn;
    public String deliveredOn;
    public String orderID;
    public String paymentMethod;
    public String restaurant;
    public Boolean seen;

    public String getInitiatedOn() {
        return initiatedOn;
    }

    public void setInitiatedOn(String initiatedOn) {
        this.initiatedOn = initiatedOn;
    }

    public String getDeliveredOn() {
        return deliveredOn;
    }

    public void setDeliveredOn(String deliveredOn) {
        this.deliveredOn = deliveredOn;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(String restaurant) {
        this.restaurant = restaurant;
    }

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }
}
