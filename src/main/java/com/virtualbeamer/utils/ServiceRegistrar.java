package com.virtualbeamer.utils;

import com.virtualbeamer.services.MainService;

import java.io.IOException;

final public class ServiceRegistrar {
    public static void registerServices() throws IOException {
        MainService.getInstance();
    }
}
