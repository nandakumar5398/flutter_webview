// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebResourceErrorCompat;
import androidx.webkit.WebViewClientCompat;
import io.flutter.plugin.common.MethodChannel;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// We need to use WebViewClientCompat to get
// shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
// invoked by the webview on older Android devices, without it pages that use iframes will
// be broken when a navigationDelegate is set on Android version earlier than N.
class FlutterWebViewClient {
  private static final String TAG = "FlutterWebViewClient";
  private final MethodChannel methodChannel;
  private boolean hasNavigationDelegate;
  boolean hasProgressTracking;
  String htmlData = "<!DOCTYPE html><html><head><title>Maintenance</title><style type='text/css'>html, body{font-family: 'Montserrat', sans-serif !important;}.server{text-align: center;width: 80%;margin: 10% auto;}html{height: 100%;background-color: #FBFBFB;}.server p{color: #9E9E9E !important;font-size: 14px !important;}img{width: 180px;}a{color: #3F51B5 !important;}</style></head><body><div class='server'><img src=\"data:image/png;base64,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\" alt=\"Issue Occurred\" /><h2 style='color: #9E9E9E;font-weight: normal;'>Under Maintenance</h2><p>Unable to process your request please try again by restarting the application !!!</p><p>please contact <a href='mailto:info@tmsforce.com' target='_top'>info@tmsforce.com</a> for further clarifications</p></div></body></html>";
  String encodehtmlData = Base64.encodeToString(htmlData.getBytes(), Base64.NO_PADDING);

  FlutterWebViewClient(MethodChannel methodChannel) {
    this.methodChannel = methodChannel;
  }

  private static String errorCodeToString(int errorCode) {
    switch (errorCode) {
      case WebViewClient.ERROR_AUTHENTICATION:
        return "authentication";
      case WebViewClient.ERROR_BAD_URL:
        return "badUrl";
      case WebViewClient.ERROR_CONNECT:
        return "connect";
      case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
        return "failedSslHandshake";
      case WebViewClient.ERROR_FILE:
        return "file";
      case WebViewClient.ERROR_FILE_NOT_FOUND:
        return "fileNotFound";
      case WebViewClient.ERROR_HOST_LOOKUP:
        return "hostLookup";
      case WebViewClient.ERROR_IO:
        return "io";
      case WebViewClient.ERROR_PROXY_AUTHENTICATION:
        return "proxyAuthentication";
      case WebViewClient.ERROR_REDIRECT_LOOP:
        return "redirectLoop";
      case WebViewClient.ERROR_TIMEOUT:
        return "timeout";
      case WebViewClient.ERROR_TOO_MANY_REQUESTS:
        return "tooManyRequests";
      case WebViewClient.ERROR_UNKNOWN:
        return "unknown";
      case WebViewClient.ERROR_UNSAFE_RESOURCE:
        return "unsafeResource";
      case WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME:
        return "unsupportedAuthScheme";
      case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
        return "unsupportedScheme";
    }

    final String message =
        String.format(Locale.getDefault(), "Could not find a string for errorCode: %d", errorCode);
    throw new IllegalArgumentException(message);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
    if (!hasNavigationDelegate) {
      return false;
    }
    notifyOnNavigationRequest(
        request.getUrl().toString(), request.getRequestHeaders(), view, request.isForMainFrame());
    // We must make a synchronous decision here whether to allow the navigation or not,
    // if the Dart code has set a navigation delegate we want that delegate to decide whether
    // to navigate or not, and as we cannot get a response from the Dart delegate synchronously we
    // return true here to block the navigation, if the Dart delegate decides to allow the
    // navigation the plugin will later make an addition loadUrl call for this url.
    //
    // Since we cannot call loadUrl for a subframe, we currently only allow the delegate to stop
    // navigations that target the main frame, if the request is not for the main frame
    // we just return false to allow the navigation.
    //
    // For more details see: https://github.com/flutter/flutter/issues/25329#issuecomment-464863209
    return request.isForMainFrame();
  }

  boolean shouldOverrideUrlLoading(WebView view, String url) {
    if (!hasNavigationDelegate) {
      return false;
    }
    // This version of shouldOverrideUrlLoading is only invoked by the webview on devices with
    // webview versions  earlier than 67(it is also invoked when hasNavigationDelegate is false).
    // On these devices we cannot tell whether the navigation is targeted to the main frame or not.
    // We proceed assuming that the navigation is targeted to the main frame. If the page had any
    // frames they will be loaded in the main frame instead.
    Log.w(
        TAG,
        "Using a navigationDelegate with an old webview implementation, pages with frames or iframes will not work");
    notifyOnNavigationRequest(url, null, view, true);
    return true;
  }

  private void onPageStarted(WebView view, String url) {
    Log.w(
        TAG,
        "From web onPageStarted");
    Map<String, Object> args = new HashMap<>();
    args.put("url", url);
    methodChannel.invokeMethod("onPageStarted", args);
  }

  private void onPageFinished(WebView view, String url) {
    Map<String, Object> args = new HashMap<>();
    args.put("url", url);
    methodChannel.invokeMethod("onPageFinished", args);
  }

  void onLoadingProgress(int progress) {
    if (hasProgressTracking) {
      Map<String, Object> args = new HashMap<>();
      args.put("progress", progress);
      methodChannel.invokeMethod("onProgress", args);
    }
  }

  private void onWebResourceError(
      final int errorCode, final String description, final String failingUrl) {
    Log.w(
            TAG,
            "onWebResourceError");
    final Map<String, Object> args = new HashMap<>();
    args.put("errorCode", errorCode);
    args.put("description", description);
    args.put("errorType", FlutterWebViewClient.errorCodeToString(errorCode));
    args.put("failingUrl", failingUrl);
    methodChannel.invokeMethod("onWebResourceError", args);
  }

  private void notifyOnNavigationRequest(
      String url, Map<String, String> headers, WebView webview, boolean isMainFrame) {
    HashMap<String, Object> args = new HashMap<>();
    args.put("url", url);
    args.put("isForMainFrame", isMainFrame);
    if (isMainFrame) {
      methodChannel.invokeMethod(
          "navigationRequest", args, new OnNavigationRequestResult(url, headers, webview));
    } else {
      methodChannel.invokeMethod("navigationRequest", args);
    }
  }

  // This method attempts to avoid using WebViewClientCompat due to bug
  // https://bugs.chromium.org/p/chromium/issues/detail?id=925887. Also, see
  // https://github.com/flutter/flutter/issues/29446.
  WebViewClient createWebViewClient(boolean hasNavigationDelegate) {
    this.hasNavigationDelegate = hasNavigationDelegate;

    if (!hasNavigationDelegate || android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return internalCreateWebViewClient();
    }

    return internalCreateWebViewClientCompat();
  }

  private WebViewClient internalCreateWebViewClient() {
    return new WebViewClient() {
      @TargetApi(Build.VERSION_CODES.N)
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return FlutterWebViewClient.this.shouldOverrideUrlLoading(view, request);
      }

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        FlutterWebViewClient.this.onPageStarted(view, url);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        FlutterWebViewClient.this.onPageFinished(view, url);
      }

      @TargetApi(Build.VERSION_CODES.M)
      @Override
      public void onReceivedError(
          WebView view, WebResourceRequest request, WebResourceError error) {
        Log.w(
                "Build.VERSION_CODES.M",
                "onRecievedErro");
        if (request.isForMainFrame() && error != null) {
          view.loadData(encodehtmlData, "text/html", "base64");
//          try{
//            view.loadUrl("file:///android_asset/www/error.html?errorCode=\" + errorCode + \"&errorDescription=\" + description");
//          }catch(Exception e){
//            Log.w(
//                    "Web Page not available exception",
//                    e);
//            view.loadData(htmlData, "text/html", "UTF-8");
//          }
        }
//        FlutterWebViewClient.this.onWebResourceError(
//            error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
      }

      @Override
      public void onReceivedError(
          WebView view, int errorCode, String description, String failingUrl) {
        Log.w(
                "NOn m",
                "onRecievedErro");
        FlutterWebViewClient.this.onWebResourceError(errorCode, description, failingUrl);
      }

      @Override
      public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
        // Deliberately empty. Occasionally the webview will mark events as having failed to be
        // handled even though they were handled. We don't want to propagate those as they're not
        // truly lost.
      }
    };
  }

  private WebViewClientCompat internalCreateWebViewClientCompat() {
    return new WebViewClientCompat() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return FlutterWebViewClient.this.shouldOverrideUrlLoading(view, request);
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return FlutterWebViewClient.this.shouldOverrideUrlLoading(view, url);
      }

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        FlutterWebViewClient.this.onPageStarted(view, url);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        FlutterWebViewClient.this.onPageFinished(view, url);
      }

      // This method is only called when the WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR feature is
      // enabled. The deprecated method is called when a device doesn't support this.
      @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
      @SuppressLint("RequiresFeature")
      @Override
      public void onReceivedError(
          WebView view, WebResourceRequest request, WebResourceErrorCompat error) {
        Log.w(
                "RequiresFeature",
                "onRecievedErro");
        FlutterWebViewClient.this.onWebResourceError(
            error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
      }

      @Override
      public void onReceivedError(
          WebView view, int errorCode, String description, String failingUrl) {
        Log.w(
                "below RequiresFeature",
                "onRecievedErro");
        FlutterWebViewClient.this.onWebResourceError(errorCode, description, failingUrl);
      }

      @Override
      public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
        // Deliberately empty. Occasionally the webview will mark events as having failed to be
        // handled even though they were handled. We don't want to propagate those as they're not
        // truly lost.
      }
    };
  }

  private static class OnNavigationRequestResult implements MethodChannel.Result {
    private final String url;
    private final Map<String, String> headers;
    private final WebView webView;

    private OnNavigationRequestResult(String url, Map<String, String> headers, WebView webView) {
      this.url = url;
      this.headers = headers;
      this.webView = webView;
    }

    @Override
    public void success(Object shouldLoad) {
      Boolean typedShouldLoad = (Boolean) shouldLoad;
      if (typedShouldLoad) {
        loadUrl();
      }
    }

    @Override
    public void error(String errorCode, String s1, Object o) {
      throw new IllegalStateException("navigationRequest calls must succeed");
    }

    @Override
    public void notImplemented() {
      throw new IllegalStateException(
          "navigationRequest must be implemented by the webview method channel");
    }

    private void loadUrl() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        webView.loadUrl(url, headers);
      } else {
        webView.loadUrl(url);
      }
    }
  }
}
