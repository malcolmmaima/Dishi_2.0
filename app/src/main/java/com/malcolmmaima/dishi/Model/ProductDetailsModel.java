package com.malcolmmaima.dishi.Model;

public class ProductDetailsModel {

    public String name;
    public String price;
    public String description;
    public String imageURL;
    public String imageUrlSmall;
    public String imageUrlMedium;
    public String imageUrlBig;
    public  String storageLocation;
    public Boolean outOfStock;
    public String key; //When deleting menu items from firebase, this key value will help delete individual items from the menu node
    public String owner;
    public int quantity;
    String uploadDate;
    Double distance;
    public String accountType; //Not an exclusive part of the product model. Just a temprary data holder for passing values between different states
    //below values are for our cart and orders, values from customer to restaurant
    public String originalKey; //Always keep track of the original key of the menu item from restaurant

    public String getOriginalKey() {
        return originalKey;
    }

    public void setOriginalKey(String originalKey) {
        this.originalKey = originalKey;
    }

    //below values are from restaurant back to customer (setting order statu to confirmed or decline
    public Boolean confirmed;

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageURL(String imageURL){
        this.imageURL = imageURL;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public Boolean getOutOfStock() {
        return outOfStock;
    }

    public void setOutOfStock(Boolean outOfStock) {
        this.outOfStock = outOfStock;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }


    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getImageUrlSmall() {
        return imageUrlSmall;
    }

    public void setImageUrlSmall(String imageUrlSmall) {
        this.imageUrlSmall = imageUrlSmall;
    }

    public String getImageUrlMedium() {
        return imageUrlMedium;
    }

    public void setImageUrlMedium(String imageUrlMedium) {
        this.imageUrlMedium = imageUrlMedium;
    }

    public String getImageUrlBig() {
        return imageUrlBig;
    }

    public void setImageUrlBig(String imageUrlBig) {
        this.imageUrlBig = imageUrlBig;
    }
}