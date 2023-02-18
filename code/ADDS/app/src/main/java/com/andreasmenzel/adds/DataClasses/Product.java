package com.andreasmenzel.adds.DataClasses;

public class Product {

    private String id;
    private String name;
    private String description;


    public Product(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GETTERS AND SETTERS                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the id of the product.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the product.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the product.
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

}
