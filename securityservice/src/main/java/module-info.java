module com.udacity.securityservice {
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires com.udacity.imageservice;
    requires com.miglayout.swing;
    opens com.udacity.securityservice.data to com.google.gson;
    exports com.udacity.securityservice.service;
}