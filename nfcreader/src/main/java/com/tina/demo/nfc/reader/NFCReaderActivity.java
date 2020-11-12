package com.tina.demo.nfc.reader;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;

public class NFCReaderActivity extends AppCompatActivity {

    private final String TAG = "TinaNFC_reader";
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mIntentFilter;
    private String[][] mTechList;
    private TextView mTvView;

    // 卡片返回来的正确信号
    private final byte[] SELECT_OK = NFCUtils.stringToBytes("1000");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvView = findViewById(R.id.textView);
        nfcCheck();
        init();
    }

    private void init() {
        // NFCActivity 一般设置为: SingleTop模式 ，并且锁死竖屏，以避免屏幕旋转Intent丢失
        Intent intent = new Intent(this, NFCReaderActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // 私有的请求码
        final int REQUEST_CODE = 1 << 16;

        final int FLAG = 0;
//        mPendingIntent = PendingIntent.getActivity(NFCReaderActivity.this, REQUEST_CODE, intent, FLAG);
        mPendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this,
                        getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // 三种过滤器
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mIntentFilter = new IntentFilter[]{ndef, tech, tag};

        // 只针对ACTION_TECH_DISCOVERED
        mTechList = new String[][]{
                {IsoDep.class.getName()}, {NfcA.class.getName()}, {NfcB.class.getName()},
                {NfcV.class.getName()}, {NfcF.class.getName()}, {Ndef.class.getName()}};
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // IsoDep卡片通信的工具类，Tag就是卡
        discoverTag((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        SparseArray
    }

    public void discoverTag(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            String info = "读取卡信息失败";
            toast(info);
            return;
        }
        try {
            // NFC与卡进行连接
            isoDep.connect();

//            final String AID = "F001020304050608";
            final String AID = "A000000333010101";
            //转换指令为byte[]
            byte[] command = NFCUtils.buildSelectApdu(AID);

            // 发送指令
            byte[] result = isoDep.transceive(command);

            // 截取响应数据
            int resultLength = result.length;
            byte[] statusWord = {result[resultLength - 2], result[resultLength - 1]};
            byte[] payload = Arrays.copyOf(result, resultLength - 2);

            // 检验响应数据
            if (Arrays.equals(SELECT_OK, statusWord)) {
                String accountNumber = new String(payload, "UTF-8");
                Log.e(TAG, "----> " + accountNumber);
                mTvView.setText(accountNumber);
            } else {
                String info = NFCUtils.bytesToString(result);
                Log.e(TAG, "----> error" + info);
                mTvView.setText(info);
            }
        } catch (IOException e) {
            Log.d(TAG,"Error--->" +  e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 开启检测,检测到卡后，onNewIntent() 执行 4.4 之前
     * enableForegroundDispatch()只能在onResume() 方法中，否则会报：
     * Foreground dispatch can only be enabled when your activity is resumed
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter == null) return;
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilter, mTechList);
        enableReaderMode();
        Log.e("NFC----", IsoDep.class.getName());
    }

    @TargetApi(19)
    private void enableReaderMode() {
        int READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
        if (mNfcAdapter != null) {
            mNfcAdapter.enableReaderMode(this, new NfcAdapter.ReaderCallback() {
                @Override
                public void onTagDiscovered(final Tag tag) {
                    Log.d(TAG, "tag----> " + tag);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            discoverTag(tag);
                        }
                    });

                }
            }, READER_FLAGS, null);
        }
    }

    /**
     * 关闭检测
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter == null) return;
        mNfcAdapter.disableForegroundDispatch(this);
        disableReaderMode();
    }

    @TargetApi(19)
    private void disableReaderMode() {
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(this);
        }
    }

    /**
     * 检测是否支持 NFC
     */
    private void nfcCheck() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            String info = "手机不支付NFC功能";
            toast(info);
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            String info = "手机NFC功能没有打开";
            toast(info);
            Intent setNfc = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivity(setNfc);
        } else {
            String info = "手机NFC功能正常";
            toast(info);
        }
    }

    private void toast(String info) {
        Toast.makeText(NFCReaderActivity.this, info, Toast.LENGTH_SHORT).show();
    }

    //https://blog.csdn.net/qq_34206198/article/details/51781651
}
