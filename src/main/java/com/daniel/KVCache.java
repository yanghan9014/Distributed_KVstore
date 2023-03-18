package com.daniel;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class KVCache
{
    Cache<Key, byte[]> cache;

    public byte[] getIfPresent(Key key) {
        return this.cache.getIfPresent(key);
    }
    public KVCache(int cacheSize, int cacheTimeout) {
        this.cache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(cacheTimeout, TimeUnit.MILLISECONDS).build();
    }
    public void put(Key key, byte[] payload) {
        this.cache.put(key, payload);
    }
    public void wipeout() {
        this.cache.invalidateAll();
    }
    public long size() {
        return this.cache.size();
    }
}
