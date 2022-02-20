package com.virtual.beamer.utils;

import com.virtual.beamer.models.User;

import java.io.IOException;

final public class ServiceRegistrar {
    public static void registerServices() throws IOException {
        User.getInstance();
    }
}
