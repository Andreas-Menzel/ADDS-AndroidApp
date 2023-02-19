package com.andreasmenzel.adds;

import com.andreasmenzel.adds.DataClasses.Product;
import com.andreasmenzel.adds.DataClasses.ProductList;
import com.andreasmenzel.adds.Manager.CommunicationManager;

public class MyApplication extends android.app.Application {

    private static CommunicationManager communicationManagerRegisterAccount = null;
    private static CommunicationManager communicationManagerActivateAccount = null;
    private static CommunicationManager communicationManagerAuthenticateAccount = null;

    private static CommunicationManager communicationManagerProduct = null;
    private static CommunicationManager communicationManagerProductList = null;


    public MyApplication() {

    }


    public static CommunicationManager getCommunicationManagerRegisterAccountNotNull() {
        return communicationManagerRegisterAccount != null ? communicationManagerRegisterAccount : new CommunicationManager(CommunicationManager.RequestTypes.registerAccount);
    }

    public static CommunicationManager getCommunicationManagerActivateAccountNotNull() {
        return communicationManagerActivateAccount != null ? communicationManagerActivateAccount : new CommunicationManager(CommunicationManager.RequestTypes.activateAccount);
    }

    public static CommunicationManager getCommunicationManagerAuthenticateAccountNotNull() {
        return communicationManagerAuthenticateAccount != null ? communicationManagerAuthenticateAccount : new CommunicationManager(CommunicationManager.RequestTypes.authenticateAccount);
    }

    public static CommunicationManager getCommunicationManagerProductNotNull(Product product) {
        return communicationManagerProduct != null ? communicationManagerProduct : new CommunicationManager(product);
    }

    public static CommunicationManager getCommunicationManagerProductListNotNull(ProductList productList) {
        return communicationManagerProductList != null ? communicationManagerProductList : new CommunicationManager(productList);
    }
}
