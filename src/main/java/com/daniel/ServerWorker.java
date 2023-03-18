package com.daniel;

import ca.NetSysLab.ProtocolBuffers.KeyValueRequest.KVRequest;
import ca.NetSysLab.ProtocolBuffers.KeyValueResponse.KVResponse;
import ca.NetSysLab.ProtocolBuffers.Message.Msg;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.daniel.Utils.calculateCheckSum;

public class ServerWorker implements Runnable
{
    private final DatagramSocket socket;
    private final DatagramPacket recPacket;
    private final KVStore store;
    private final KVCache cache;
    private KVRequest kvReq;
    private final int overloadWaitTime;
    ServerWorker(DatagramSocket socket, DatagramPacket recPacket, KVCache cache, KVStore store, int overloadWaitTime){
        this.socket = socket;
        this.recPacket = recPacket;
        this.cache = cache;
        this.store = store;
        this.overloadWaitTime = overloadWaitTime;
    }
    public void run() {
        KVRequest kvReq;
        Msg.Builder resMsgBuilder = Msg.newBuilder();
        KVResponse.Builder kvResBuilder = KVResponse.newBuilder();
        int err = ErrorCode.SUCCESS.getCode();

        byte[] cacheValue;
        // Parse request and respond directly if found in cache
        try {
            Msg msg = Msg.parseFrom(Arrays.copyOf(this.recPacket.getData(), this.recPacket.getLength()));
            if (!Utils.verifyCheckSum(msg)) {
                // CheckSum failed. Just exit.
                System.out.println("Checksum failed");
                return;
            }
            resMsgBuilder.setMessageID(msg.getMessageID());
            kvReq = KVRequest.parseFrom(msg.getPayload());
            cacheValue = this.cache.getIfPresent(new Key(msg.getMessageID().toByteArray()));
            if (cacheValue != null) {
                resMsgBuilder.setPayload(ByteString.copyFrom(cacheValue));
                resMsgBuilder.setCheckSum(calculateCheckSum(msg.getMessageID().toByteArray(), cacheValue));

                byte[] response = resMsgBuilder.build().toByteArray();
                DatagramPacket resPacket = new DatagramPacket(response, response.length, this.recPacket.getAddress(), this.recPacket.getPort());
                this.socket.send(resPacket);
                return;
            } else if (this.cache.size() > 1000 && Utils.lowMemory(Utils.MAX_REQUEST_SIZE * 200L)) {
                throw new TemporarySystemOverloadException();
            } else if (kvReq.getCommand() == 0x01 && Utils.lowMemory(Utils.MAX_REQUEST_SIZE * 100L)) {
                throw new OutOfMemoryError("Out of memory");
            } else if (Utils.lowMemory(Utils.MAX_REQUEST_SIZE * 100L)) {
                throw new OutOfMemoryError("Out of memory");
            }
            int command = kvReq.getCommand();
            byte[] key;
            byte[] value;
            int version = 0;

            switch (command) {
                case 0x01:
                    // Put: This is a put operation.
                    key = kvReq.getKey().toByteArray();
                    if (key.length > 32) {
                        err = ErrorCode.INVALID_KEY.getCode();
                        break;
                    }
                    value = kvReq.getValue().toByteArray();
                    if (value.length > 10000) {
                        err = ErrorCode.INVALID_VALUE.getCode();
                        break;
                    }
                    if (kvReq.hasVersion())
                        version = kvReq.getVersion();

                    this.store.put(key, value, version);
                    break;
                case 0x02:
                    // Get: This is a get operation.
                    key = kvReq.getKey().toByteArray();
                    if (key.length > 32) {
                        err = ErrorCode.INVALID_KEY.getCode();
                        break;
                    }
                    byte[] valVer;
                    valVer = this.store.get(key);
                    if (valVer == null) {
                        err = ErrorCode.NON_EXISTENT_KEY.getCode();
                        break;
                    }
                    ByteBuffer buffer = ByteBuffer.wrap(valVer);
                    version = buffer.getInt();
                    value = new byte[valVer.length - 4];
                    buffer.get(value);

                    kvResBuilder.setValue(ByteString.copyFrom(value));
                    kvResBuilder.setVersion(version);
                    break;
                case 0x03:
                    // Remove: This is a remove operation.
                    key = kvReq.getKey().toByteArray();
                    if (key.length > 32) {
                        err = ErrorCode.INVALID_KEY.getCode();
                        break;
                    }
                    valVer = this.store.remove(key);
                    if (valVer == null) {
                        err = ErrorCode.NON_EXISTENT_KEY.getCode();
                        break;
                    }
                    break;
                case 0x04:
                    // Shutdown: shuts-down the node (used for testing and management).
                    // The expected behaviour is that your implementation immediately calls System.exit().
                    System.exit(0);
                    break;
                case 0x05:
                    // Wipeout: deletes all keys stored in the node (used for testing)
                    this.store.wipeout();
                    this.cache.wipeout();
                    break;
                case 0x06:
                    // IsAlive: does nothing but replies with success if the node is alive.
                    break;
                case 0x07:
                    // GetPID: the node is expected to reply with the processID of the Java process
                    long pid = ProcessHandle.current().pid();
                    kvResBuilder.setPid((int) pid);
                    break;
                case 0x08:
                    // GetMembershipCount: the node is expected to reply with the count of the currently active members based on your membership protocol.  (this will be used later, for now you are expected to return 1)
                    kvResBuilder.setMembershipCount(1);
                    break;
                default:
                    err = ErrorCode.UNRECOGNIZED_COMMAND.getCode();
            }
        } catch (OutOfMemoryError e){
            System.out.println("Out of space: " + Utils.getFreeMemory()/1024 + " kB left");
            err = ErrorCode.OUT_OF_SPACE.getCode();
            System.gc();
        } catch (TemporarySystemOverloadException e) {
            System.out.println("Temporary overload: " + Utils.getFreeMemory()/1024 + " kB left");
            err = ErrorCode.TEMPORARY_OVERLOAD.getCode();
            kvResBuilder.setOverloadWaitTime(this.overloadWaitTime);
            System.gc();
        } catch (InvalidProtocolBufferException e) {
            err = ErrorCode.INTERNAL_FAILURE.getCode();
            throw new RuntimeException(e);
        } catch (IOException e) {
            err = ErrorCode.INTERNAL_FAILURE.getCode();
            System.out.println("Something went wrong");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            kvResBuilder.setErrCode(err);
            KVResponse kvRes = kvResBuilder.build();
            this.cache.put(new Key(resMsgBuilder.getMessageID().toByteArray()), kvRes.toByteString().toByteArray());

            long checkSum = Utils.calculateCheckSum(resMsgBuilder.getMessageID().toByteArray(), kvRes.toByteArray());

            Msg resMsg = resMsgBuilder.setPayload(kvRes.toByteString())
                    .setCheckSum(checkSum)
                    .build();
            byte[] s = resMsg.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(s, s.length, this.recPacket.getAddress(), this.recPacket.getPort());
            try {
                this.socket.send(sendPacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
