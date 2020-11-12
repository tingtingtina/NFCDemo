package com.tina.demo.nfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

/*
 * Created by Tina
 * Date: 2020/11/5
 * Description：
 */
public class MyHostApduService extends HostApduService {
    public final static String TAG = "TinaNFC";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"service start");
    }

    // 正确信号
    private byte[] SELECT_OK = NFCUtils.stringToBytes("1000");

    // 错误信号
    private byte[] UNKNOWN_ERROR = NFCUtils.stringToBytes("0000");

    /**
     * 接收到 NFC 读卡器发送的应用协议数据单元 (APDU) 调用
     * 注意：此方法回调在UI线程,若进行联网操作时，需开子线程
     * 并先返回null，当子线程有数据结果后，再进行回调返回处理
     */
    //只要 NFC 读取器向您的服务发送应用协议数据单元 (APDU)，系统就会调用 processCommandApdu()
    // 该应用级协议为半双工：NFC 读取器会向您发送命令 APDU，反之它会等待您发送响应 APDU。
    @Override
    public byte[] processCommandApdu(byte[] bytes, Bundle bundle) {
        Log.d(TAG, "--->processCommandApdu");

        final String AID = "F001020304050608";

        // 将指令转换成 byte[]
        byte[] selectAPDU = NFCUtils.buildSelectApdu(AID);

        // 判断是否和读卡器发来的数据相同
        if (Arrays.equals(selectAPDU, bytes)) {
            // 直接模拟返回16位卡号
            String account = "6222222200000001";

            // 获取卡号 byte[]
            byte[] accountBytes = account.getBytes();

            // 处理欲返回的响应数据
            return concatArrays(accountBytes, SELECT_OK);
        } else {
            return UNKNOWN_ERROR;
        }
    }

    /**
     * Android 会继续将读取器的新 APDU 转发到您的服务，直到发生下列任一情况：
     * <p>
     * NFC 读取器发送另一个“SELECT AID”APDU，操作系统会将其解析为其他服务；
     * NFC 读取器和设备之间的 NFC 链接断开。
     *
     * @param i
     */
    @Override
    public void onDeactivated(int i) {
        if (i == HostApduService.DEACTIVATION_DESELECTED) {
            Toast.makeText(getApplicationContext(), "已选择其它应用", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "连接断开", Toast.LENGTH_LONG).show();
        }
    }

    private byte[] concatArrays(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
