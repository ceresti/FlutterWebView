package io.github.ponnamkarthik.flutterwebview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.platform.PlatformView;

import static io.flutter.plugin.common.MethodChannel.MethodCallHandler;


public class FlutterWeb implements PlatformView, MethodCallHandler {

    private Context context;
    private Registrar registrar;
    private WebView webView;
    private String url = "";
    private MethodChannel channel;
    private EventChannel.EventSink onPageFinishEvent;
    private EventChannel.EventSink onPageStartEvent;

    private Pattern shouldOverrideUrlLoadingRegEx;


    @SuppressLint("SetJavaScriptEnabled")
    FlutterWeb(Context context, Registrar registrar, int id) {
        this.context = context;
        this.registrar = registrar;
        webView = getWebView(registrar);

        channel = new MethodChannel(registrar.messenger(), "ponnamkarthik/flutterwebview_" + id);
        final EventChannel onPageFinishEventChannel = new EventChannel(registrar.messenger(), "ponnamkarthik/flutterwebview_stream_pagefinish_" + id);
        final EventChannel onPageStartEvenetChannel = new EventChannel(registrar.messenger(), "ponnamkarthik/flutterwebview_stream_pagestart_" + id);

        onPageFinishEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                onPageFinishEvent = eventSink;
            }

            @Override
            public void onCancel(Object o) {

            }
        });
        onPageStartEvenetChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                onPageStartEvent = eventSink;
            }

            @Override
            public void onCancel(Object o) {

            }
        });
        channel.setMethodCallHandler(this);
    }

    @Override
    public View getView() {
        return webView;
    }

    @Override
    public void dispose() {

    }

    private WebView getWebView(Registrar registrar) {
        WebView webView = new WebView(registrar.context());
        webView.setWebViewClient(new CustomWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        return webView;
    }


    private class CustomWebViewClient extends WebViewClient {
        @SuppressWarnings("deprecated")
        @Override
        public boolean shouldOverrideUrlLoading(WebView wv, String url) {
            if (url.startsWith("http") || url.startsWith("https") || url.startsWith("ftp")) {
                return false;
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                registrar.activity().startActivity(intent);
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (onPageStartEvent != null) {
                onPageStartEvent.success(url);
            }
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (onPageFinishEvent != null) {
                onPageFinishEvent.success(url);
            }
            super.onPageFinished(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (shouldOverrideUrlLoadingRegEx != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return shouldOverrideUrlLoadingRegEx.matcher(request.getUrl().toString()).matches();
                }
            }
            return super.shouldOverrideUrlLoading(view, request);
//            HashMap<String, Object> map = new HashMap<>();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                map.put("url", request.getUrl().toString());
//            }
//            final boolean[] result = {super.shouldOverrideUrlLoading(view, request)};
//            final Semaphore waiter = new Semaphore(1);
//            waiter.acquireUninterruptibly();
//            channel.invokeMethod("shouldOverrideUrlLoading", map, new MethodChannel.Result() {
//                @Override
//                public void success(Object o) {
//                    result[0] = (boolean) o;
//                    waiter.release();
//                }
//
//                @Override
//                public void error(String s, String s1, Object o) {
//
//                }
//
//                @Override
//                public void notImplemented() {
//
//                }
//            });
//            while (!waiter.tryAcquire()) {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            return result[0];
        }
    }

    @Override
    public void onMethodCall(MethodCall call, final MethodChannel.Result result) {
        switch (call.method) {
            case "loadUrl":
                String url = call.arguments.toString();
                webView.loadUrl(url);
                result.success(null);
                break;
            case "loadData":
                String html = call.arguments.toString();
                webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
                result.success(null);
                break;
            case "evaluate":
                String javaScript = call.arguments.toString();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(javaScript, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            result.success(value);
                        }
                    });
                    return;
                }
                result.error("OSIsTooOld", "This call requires kitkat or greater.", null);
                break;
            case "setShouldOverrideUrlLoadingRegEx":
                shouldOverrideUrlLoadingRegEx = Pattern.compile((String)call.arguments);
                result.success(null);
                break;
            default:
                result.notImplemented();
        }

    }

}
