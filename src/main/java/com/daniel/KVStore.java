package com.daniel;

import com.google.protobuf.ByteString;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class KVStore {
    private ConcurrentHashMap<Key, byte[]> store = new ConcurrentHashMap<>(2048);
    public void put(byte[] key, byte[] value, int version) {
        byte[] valVer = new byte[value.length + 4];
        ByteBuffer buffer = ByteBuffer.wrap(valVer);
        buffer.putInt(version);
        buffer.put(value);
        this.store.put(new Key(key), valVer);
    }
    public byte[] get(byte[] key) {
        return this.store.get(new Key(key));
    }
    public byte[] remove(byte[] key) {
        // @return:
        return this.store.remove(new Key(key));
    }
    public void wipeout() {
        this.store.clear();
    }
}
