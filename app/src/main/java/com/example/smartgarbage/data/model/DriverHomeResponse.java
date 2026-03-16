package com.example.smartgarbage.data.model;

public class DriverHomeResponse {
    private String message;
    private Driver driver;

    public String getMessage() { return message; }
    public Driver getDriver()  { return driver; }

    public static class Driver {
        private int id;
        private String name;
        private String email;

        public int    getId()    { return id; }
        public String getName()  { return name; }
        public String getEmail() { return email; }
    }
}