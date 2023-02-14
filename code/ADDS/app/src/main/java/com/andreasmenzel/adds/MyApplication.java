package com.andreasmenzel.adds;

public class MyApplication extends android.app.Application {

    private static CommunicationManager communicationManager;


    public MyApplication() {
        communicationManager = new CommunicationManager();
    }


    public static CommunicationManager getCommunicationManager() {
        return communicationManager;
    }

}
