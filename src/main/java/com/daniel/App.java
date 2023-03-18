package com.daniel;

public class App 
{
    public static void main(String[] args) {
        int port = 8888;
        int numThread = 7;
        int cacheSize = 10000;
        int cacheTimeout = 1000;
        int overloadWaitTime = 500;

        try {
            new Server(port, numThread, cacheSize, cacheTimeout, overloadWaitTime).start();
            System.out.println("Server running on port: " + port);
            System.out.println("Number of threads: " + numThread);
            System.out.println("Cache size: " + cacheSize);
            System.out.println("Cache expiration (ms): " + cacheTimeout);
            System.out.println("Temporary overload wait time (ms): " + overloadWaitTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
