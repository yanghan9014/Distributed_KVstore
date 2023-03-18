package com.daniel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

import static com.daniel.Utils.MAX_REQUEST_SIZE;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class Server extends Thread
{
    private static DatagramSocket socket;
    private static ExecutorService executor;
    private static ExecutorService cacheExecutor = newSingleThreadExecutor();
    private KVCache cache;
    private KVStore store;
    private int overloadWaitTime;
    private boolean running;
    public Server(int port, int numThread, int cacheSize, int cacheTimeout, int overloadWaitTime) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.executor = newFixedThreadPool(numThread);
        this.cache = new KVCache(cacheSize, cacheTimeout);
        this.store = new KVStore();
        this.overloadWaitTime = overloadWaitTime;
        this.running = true;

    }
    public void run() {
        while (this.running) {
            byte[] recBuf = new byte[MAX_REQUEST_SIZE];
            DatagramPacket recPacket = new DatagramPacket(recBuf, recBuf.length);
            try {
                socket.receive(recPacket);
                ServerWorker w = new ServerWorker(this.socket, recPacket, this.cache, this.store, overloadWaitTime);
                this.executor.submit(w);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
