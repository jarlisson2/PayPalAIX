package com.jdl.PayPal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesActivities;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.UsesServices;
import com.google.appinventor.components.annotations.androidmanifest.ActivityElement;
import com.google.appinventor.components.annotations.androidmanifest.ServiceElement;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.ActivityResultListener;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;

import org.json.JSONException;

import java.math.BigDecimal;

@DesignerComponent(version = 1, description = "PayPal <br> Developed by Jarlisson", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "aiwebres/icon.png", helpUrl = "https://github.com/jarlisson2/PayPalAIX")
@UsesServices(services = {
        @ServiceElement(name = "com.paypal.android.sdk.payments.PayPalService", exported = "false") })
@UsesActivities(activities = { @ActivityElement(name = "com.paypal.android.sdk.payments.PaymentActivity"),
        @ActivityElement(name = "com.paypal.android.sdk.payments.PaymentMethodActivity"),
        @ActivityElement(name = "com.paypal.android.sdk.payments.PaymentConfirmActivity"),
        @ActivityElement(name = "com.paypal.android.sdk.payments.LoginActivity"),
        @ActivityElement(name = "com.paypal.android.sdk.payments.PayPalFuturePaymentActivity"),
        @ActivityElement(name = "com.paypal.android.sdk.payments.FuturePaymentConsentActivity"),
        @ActivityElement(name = "com.paypal.android.sdk.payments.FuturePaymentInfoActivity"),
        @ActivityElement(name = "com.paypal.android.sdk.payments.PayPalProfileSharingActivity"),
        @ActivityElement(name = "com.paypal.android.sdk.payments.ProfileSharingConsentActivity") })

@UsesPermissions(permissionNames = "android.permission.ACCESS_NETWORK_STATE, android.permission.INTERNET")
@UsesLibraries("paypal-sdk.jar")
@SimpleObject(external = true)

public class PayPal extends AndroidNonvisibleComponent implements OnDestroyListener, ActivityResultListener {
    private static final String TAG = "ExtensionPayPal";
    private static PayPalConfiguration config;
    public Activity activity;
    public Context context;
    private int requestCode = 0;

    public PayPal(final ComponentContainer container) {
        super(container.$form());
        context = (Context) container.$context();
        activity = (Activity) context;
        form.registerForOnDestroy(this);

    }

    @SimpleFunction(description = "Configure PayPal by entering your credentials.")
    public void PayPalConfig(String clientId, String merchantName, String environment) {
        config = new PayPalConfiguration().environment(environment).clientId(clientId).merchantName(merchantName);
    }

    @SimpleFunction(description = "Calls the PayPal payment screen.")
    public void MakePayment(String value, String currency, String description) {
        PayPalPayment thingToBuy = new PayPalPayment(new BigDecimal(value), currency, description,
                PayPalPayment.PAYMENT_INTENT_SALE);
        Intent intent = new Intent((Context) form, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, thingToBuy);
        if (requestCode == 0)
            requestCode = form.registerForActivityResult(this);
        activity.startActivityForResult(intent, requestCode);
    }

    @SimpleEvent(description = "Event triggered if payment is confirmed.")
    public void PaymentConfirmed(String response) {
        EventDispatcher.dispatchEvent(this, "PaymentConfirmed", response);
    }

    @SimpleEvent(description = "Event triggered if payment is canceled.")
    public void PaymentCanceled() {
        EventDispatcher.dispatchEvent(this, "PaymentCanceled");
    }

    @SimpleEvent(description = "If there is an error during payment, this event is called with an error message.")
    public void PaymentError(String message) {
        EventDispatcher.dispatchEvent(this, "PaymentError", message);
    }

    @Override
    public void resultReturned(int requestCode, int resultCode, Intent data) {
        if (requestCode == this.requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirm != null) {
                    try {
                        Log.i(TAG, confirm.toJSONObject().toString(4));
                        Log.i(TAG, confirm.getPayment().toJSONObject().toString(4));
                        PaymentConfirmed("[" + confirm.toJSONObject().toString(4) + ","
                                + confirm.getPayment().toJSONObject().toString(4) + "]");
                    } catch (JSONException e) {
                        Log.e(TAG, "an extremely unlikely failure occurred: ", e);
                        PaymentError("An extremely unlikely failure occurred: " + e.getMessage());
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.i(TAG, "The user canceled.");
                PaymentCanceled();
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Log.i(TAG, "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
                PaymentError("An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
            }
        }
    }

    @Override
    public void onDestroy() {
        activity.stopService(new Intent(activity, PayPalService.class));
    }

}