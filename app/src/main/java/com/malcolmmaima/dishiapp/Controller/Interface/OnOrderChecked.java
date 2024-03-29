package com.malcolmmaima.dishiapp.Controller.Interface;

/**
 * This interface passes data from the product adapter back to the parent activity holding the recyclerview
 */
public interface OnOrderChecked {
    void onItemChecked(Boolean value, int position, String price, int quantity);
}
