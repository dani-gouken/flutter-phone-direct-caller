package com.yanisalfian.flutterphonedirectcaller;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.telephony.CarrierConfigManager.EXTRA_SLOT_INDEX;

/**
 * FlutterPhoneDirectCallerPlugin
 */
public class FlutterPhoneDirectCallerPlugin implements MethodCallHandler, PluginRegistry.RequestPermissionsResultListener {
    private Registrar registrar;
    public static final int CALL_REQ_CODE = 0;
    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final String CALL_PHONE = android.Manifest.permission.CALL_PHONE;
    private String number;
    private Integer simSlot;
    private final static String simSlotName[] = {
            "extra_asus_dial_use_dualsim",
            "com.Android.phone.extra.slot",
            "slot",
            "simslot",
            "sim_slot",
            "subscription",
            "Subscription",
            "phone",
            "com.Android.phone.DialingMode",
            "simSlot",
            "slot_id",
            "simId",
            "simnum",
            "phone_type",
            "slotId",
            "slotIdx"
    };


    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_phone_direct_caller");
        FlutterPhoneDirectCallerPlugin flutterPhoneDirectCallerPlugin = new FlutterPhoneDirectCallerPlugin(registrar);
        channel.setMethodCallHandler(flutterPhoneDirectCallerPlugin);
        registrar.addRequestPermissionsResultListener(flutterPhoneDirectCallerPlugin);
    }

    private FlutterPhoneDirectCallerPlugin(Registrar registrar) {
        this.registrar = registrar;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("callNumber")) {
            this.number = call.argument("number");
            this.simSlot = call.argument("simSlot");
            Log.d("Caller", "Message");
            this.number = this.number.replaceAll("#", "%23");
            if (!this.number.startsWith("tel:")) {
                this.number = String.format("tel:%s", this.number);
            }
            if (getPermissionStatus() != 1) {
                requestsPermission();
            } else {
                if (callNumber(this.number, this.simSlot)) {
                    result.success(true);
                } else {
                    result.success(false);
                }
            }
        } else {
            result.notImplemented();
        }
    }

    private void requestsPermission() {
        Activity activity = registrar.activity();
        ActivityCompat.requestPermissions(activity, new String[]{CALL_PHONE, READ_PHONE_STATE}, CALL_REQ_CODE);
    }

    private int getPermissionStatus() {
        Activity activity = registrar.activity();
        if (
                (ContextCompat.checkSelfPermission(registrar.activity(), CALL_PHONE) == PackageManager.PERMISSION_DENIED) ||
                        (ContextCompat.checkSelfPermission(registrar.activity(), READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
        ) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, CALL_PHONE) ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_PHONE_STATE)
            ) {
                return -1;
            } else {
                return 0;
            }
        } else {
            return 1;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean callNumber(String number, Integer simSlot) {

        try {
            Intent intent = new Intent(isTelephonyEnabled() ? Intent.ACTION_CALL : Intent.ACTION_VIEW);
            intent.setData(Uri.parse(number));
            if (simSlot != null) {
                TelecomManager telecomManager = (TelecomManager) this.registrar.activeContext().getSystemService(Context.TELECOM_SERVICE);
                if (ActivityCompat.checkSelfPermission(this.registrar.activeContext(), READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    throw new Exception("permission missing");
                }
                List<PhoneAccountHandle> phoneAccountHandleList = telecomManager.getCallCapablePhoneAccounts();
                intent.putExtra("com.android.phone.force.slot", true);
                intent.putExtra("Cdma_Supp", true);
                intent.putExtra("com.android.phone.extra.slot", simSlot); //For sim 1
                for (String s : simSlotName)
                    intent.putExtra(s, simSlot); //0 or 1 according to sim.......
                if (phoneAccountHandleList != null && phoneAccountHandleList.size() > 0)
                    intent.putExtra("Android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandleList.get(simSlot));
            }
            registrar.activity().startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.d("Caller", "error: " + e.getMessage());
            return false;
        }
    }

    private boolean isTelephonyEnabled() {
        TelephonyManager tm = (TelephonyManager) registrar.activity().getSystemService(Context.TELEPHONY_SERVICE);
        return tm != null && tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] strings, int[] ints) {
        for (int r : ints) {
            if (r == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        switch (requestCode) {
            case CALL_REQ_CODE:
                callNumber(this.number, this.simSlot);
                break;
        }
        return true;
    }
}
