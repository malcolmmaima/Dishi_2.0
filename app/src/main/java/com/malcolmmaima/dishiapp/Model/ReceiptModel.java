package com.malcolmmaima.dishiapp.Model;

public class ReceiptModel {
    public String key;
    public String initiatedOn;
    public String deliveredOn;
    public String orderID;
    public String paymentMethod;
    public String restaurant;
    public String customer;
    public Boolean seen;
    public Integer deliveryCharge;

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

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    public Integer getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(Integer deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }
}
