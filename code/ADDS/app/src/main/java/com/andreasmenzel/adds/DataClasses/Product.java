package com.andreasmenzel.adds.DataClasses;

import com.andreasmenzel.adds.Manager.CommunicationManager;
import com.andreasmenzel.adds.MyApplication;
import com.andreasmenzel.adds.ResponseAnalyzer;

public class Product {

    CommunicationManager communicationManager;
    ResponseAnalyzer responseAnalyzer;

    private String id;
    private String name;
    private String description;


    public Product(String id, String name, String description) {
        communicationManager = MyApplication.getCommunicationManagerProductNotNull(this);
        responseAnalyzer = communicationManager.getResponseAnalyzer();

        this.id = id;
        this.name = name;
        this.description = description;
    }


    public void updateProductInfo() {
        communicationManager.updateProductInfo(id);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GETTERS AND SETTERS                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the id of the product.
     *
     * @return id.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id of the product.
     *
     * @param id id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the name of the product.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the product.
     *
     * @param name name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description of the product.
     *
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the product;
     *
     * @param description description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
