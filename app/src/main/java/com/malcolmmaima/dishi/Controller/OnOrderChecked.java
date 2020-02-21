package com.malcolmmaima.dishi.Controller;

public interface OnOrderChecked {
    void onItemChecked(Boolean value, int position, String price, int quantity);
}
