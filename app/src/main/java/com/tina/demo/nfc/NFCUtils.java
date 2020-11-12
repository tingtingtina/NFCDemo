package com.tina.demo.nfc;

/*
 * Created by Tina
 * Date: 2020/11/6
 * Description：
 */
public class NFCUtils {

    /**
     * 字符串转字节数组
     * @param s 字符串
     * @return
     */
    public static byte[] stringToBytes(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("指令字符串长度必须为偶数 !!!");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[(i / 2)] = ((byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16)));
        }
        return data;
    }

    /**
     * 字节数组转16进制字符串
     * @param data
     * @return
     */
    public static String bytesToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte d : data) {
            sb.append(String.format("%02X", d));
        }
        return sb.toString();
    }

    /**
     * @param aid
     * @return
     */
    public static byte[] buildSelectApdu(String aid) {
        final String HEADER = "00A40400";
        return stringToBytes(HEADER + String.format("%02X", aid.length() / 2) + aid);
    }
}
