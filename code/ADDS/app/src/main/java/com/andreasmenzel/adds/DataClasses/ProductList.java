package com.andreasmenzel.adds.DataClasses;

import com.andreasmenzel.adds.Manager.CommunicationManager;
import com.andreasmenzel.adds.MyApplication;
import com.andreasmenzel.adds.ResponseAnalyzer;

import java.util.LinkedList;
import java.util.List;

public class ProductList {

    private final CommunicationManager communicationManager;
    private final ResponseAnalyzer responseAnalyzer;

    private String warehouseID = null;
    private LinkedList<Product> products = new LinkedList<Product>();


    public ProductList(String warehouseID) {
        communicationManager = MyApplication.getCommunicationManagerProductListNotNull(this);
        responseAnalyzer = communicationManager.getResponseAnalyzer();

        this.warehouseID = warehouseID;
    }


    public void updateProductList() {
        communicationManager.updateProductList(warehouseID);
    }


    public void clearProducts() {
        products.clear();
    }


    public void addProduct(Product product) {
        products.add(product);
    }


    public LinkedList<Product> getProducts() {
        return products;
    }

}
