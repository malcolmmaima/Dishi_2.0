package com.malcolmmaima.dishiapp.Model;

public class UserModel {
    String phone;
    String account_type;
    String bio;
    int delivery_charge;
    int deliveryChargeLimit;
    Double distance; //useful in our FragmentRestaurants fragment
    String email;
    String firstname;
    String gender;
    String lastname;
    Boolean liveStatus;
    Boolean chatNotification;
    Boolean orderNotification;
    Boolean socialNotification;
    Boolean shareOrders;
    Boolean syncContacts;
    String phoneVisibility;
    String accountPrivacy;
    String locationType;
    String profilePic;
    String profilePicSmall;
    String profilePicMedium;
    String profilePicBig;
    String signupDate;
    String verified;
    Integer followers;
    Integer following;

    public long itemCount; //Not part of our user model, just a temporary data holder to be passed from OrdersFragment to orders adapter
    public String restaurantPhone; //Not part of our user model, just a temporary data holder to be passed
    public String riderPhone; //temp data holder for use in RiderFragment. will be passed to adapter
    public String timeStamp; //A temporary data holder for our inbox, we want to pass the most recent DM timestamp so as to display in chatListAdapter
    public String message; //Serves the same purpose as above variable

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Integer getFollowers() {
        return followers;
    }

    public void setFollowers(Integer followers) {
        this.followers = followers;
    }

    public Integer getFollowing() {
        return following;
    }

    public void setFollowing(Integer following) {
        this.following = following;
    }

    public String getAccount_type() {
        return account_type;
    }

    public void setAccount_type(String account_type) {
        this.account_type = account_type;
    }

    public String getBio(){
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public int getDelivery_charge() {
        return delivery_charge;
    }

    public void setDelivery_charge(int delivery_charge) {
        this.delivery_charge = delivery_charge;
    }

    public int getDeliveryChargeLimit() {
        return deliveryChargeLimit;
    }

    public void setDeliveryChargeLimit(int deliveryChargeLimit) {
        this.deliveryChargeLimit = deliveryChargeLimit;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getGender(){
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Boolean getLiveStatus() {
        return liveStatus;
    }

    public void setLiveStatus(Boolean liveStatus) {
        this.liveStatus = liveStatus;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public String getProfilePicSmall() {
        return profilePicSmall;
    }

    public void setProfilePicSmall(String profilePicSmall) {
        this.profilePicSmall = profilePicSmall;
    }

    public String getProfilePicMedium() {
        return profilePicMedium;
    }

    public void setProfilePicMedium(String profilePicMedium) {
        this.profilePicMedium = profilePicMedium;
    }

    public String getProfilePicBig() {
        return profilePicBig;
    }

    public void setProfilePicBig(String profilePicBig) {
        this.profilePicBig = profilePicBig;
    }

    public String getSignupDate() {
        return signupDate;
    }

    public void setSignupDate(String signupDate) {
        this.signupDate = signupDate;
    }

    public String getVerified() {
        return verified;
    }

    public void setVerified(String verified) {
        this.verified = verified;
    }

    public Boolean getChatNotification() {
        return chatNotification;
    }

    public void setChatNotification(Boolean chatNotification) {
        this.chatNotification = chatNotification;
    }

    public Boolean getOrderNotification() {
        return orderNotification;
    }

    public void setOrderNotification(Boolean orderNotification) {
        this.orderNotification = orderNotification;
    }

    public Boolean getSocialNotification() {
        return socialNotification;
    }

    public void setSocialNotification(Boolean socialNotification) {
        this.socialNotification = socialNotification;
    }

    public Boolean getShareOrders() {
        return shareOrders;
    }

    public void setShareOrders(Boolean shareOrders) {
        this.shareOrders = shareOrders;
    }

    public Boolean getSyncContacts() {
        return syncContacts;
    }

    public void setSyncContacts(Boolean syncContacts) {
        this.syncContacts = syncContacts;
    }

    public String getPhoneVisibility() {
        return phoneVisibility;
    }

    public void setPhoneVisibility(String phoneVisibility) {
        this.phoneVisibility = phoneVisibility;
    }

    public String getAccountPrivacy() {
        return accountPrivacy;
    }

    public void setAccountPrivacy(String accountPrivacy) {
        this.accountPrivacy = accountPrivacy;
    }
}
