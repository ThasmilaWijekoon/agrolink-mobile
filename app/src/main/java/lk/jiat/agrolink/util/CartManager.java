package lk.jiat.agrolink.util;

import java.util.ArrayList;

import lk.jiat.agrolink.model.Product;


public class CartManager {

    public static ArrayList<Product> cartList = new ArrayList<>();

    public static void addToCart(Product product) {
        cartList.add(product);
    }

    public static ArrayList<Product> getCart() {
        return cartList;
    }
}