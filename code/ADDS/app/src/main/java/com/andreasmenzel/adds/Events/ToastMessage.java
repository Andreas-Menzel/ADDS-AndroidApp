package com.andreasmenzel.adds.Events;

/**
 * Event for creating a new Toast message.
 */
public class ToastMessage {

    private String message = "";

    public ToastMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the message that was passed in the constructor.
     *
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

}
