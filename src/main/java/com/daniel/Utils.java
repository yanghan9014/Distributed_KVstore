package com.daniel;

import ca.NetSysLab.ProtocolBuffers.Message.Msg;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Utils {
    public static int MAX_REQUEST_SIZE = 16000;
    public static long calculateCheckSum(byte[] id, byte[] payload) {
        byte[] checkSum = new byte[id.length + payload.length];
        ByteBuffer buffer = ByteBuffer.wrap(checkSum);
        buffer.put(id);
        buffer.put(payload);

        CRC32 crc = new CRC32();
        crc.update(checkSum);
        return crc.getValue();
    }
    public static boolean verifyCheckSum(Msg msg){
        long checkSum = calculateCheckSum(msg.getMessageID().toByteArray(), msg.getPayload().toByteArray());
        return checkSum == msg.getCheckSum();
    }
    public static long getFreeMemory() {
        return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory();
    }
    public static boolean lowMemory(long threshold) {
        return getFreeMemory() < threshold;
    }
}
