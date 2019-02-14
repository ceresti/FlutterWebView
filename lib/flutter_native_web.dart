import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

typedef void WebViewCreatedCallback(WebController controller);

class FlutterNativeWeb extends StatefulWidget {
  const FlutterNativeWeb(
      {Key key, @required this.onWebCreated, this.gestureRecognizers})
      : super(key: key);

  final Set<Factory<OneSequenceGestureRecognizer>> gestureRecognizers;

  final WebViewCreatedCallback onWebCreated;

  @override
  State createState() => new _FlutterNativeWebState();
}

class _FlutterNativeWebState extends State<FlutterNativeWeb> {
  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: 'ponnamkarthik/flutterwebview',
        onPlatformViewCreated: onPlatformCreated,
        gestureRecognizers: widget.gestureRecognizers,
        creationParamsCodec: const StandardMessageCodec(),
//        creationParamsCodec: const StandardMessageCodec(),
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(
        viewType: 'ponnamkarthik/flutterwebview',
        onPlatformViewCreated: onPlatformCreated,
        gestureRecognizers: widget.gestureRecognizers,
        // creationParams: _CreationParams.fromWidget(widget).toMap(),
        creationParamsCodec: const StandardMessageCodec(),
      );
    }

    return new Text(
        '$defaultTargetPlatform is not yet supported by this plugin');
  }

  Future<void> onPlatformCreated(id) async {
    if (widget.onWebCreated == null) {
      return;
    }
    widget.onWebCreated(new WebController.init(id));
  }
}

class WebController {
  WebController.init(int id) {
    _channel = new MethodChannel('ponnamkarthik/flutterwebview_$id');
    _pageFinished =
        EventChannel('ponnamkarthik/flutterwebview_stream_pagefinish_$id');
    _pageStarted =
        EventChannel('ponnamkarthik/flutterwebview_stream_pagestart_$id');
    _channel.setMethodCallHandler(methodCallHandler);
  }

  MethodChannel _channel;
  EventChannel _pageFinished;
  EventChannel _pageStarted;
  FutureOr<bool> Function(String url) shouldLoadUrlHandler;

  set setShouldOverrideUrlLoadingRegEx(String value) {
    _channel.invokeMethod("setShouldOverrideUrlLoadingRegEx", value);
  }

  Future<void> loadUrl(String url) async {
    assert(url != null);
    return _channel.invokeMethod('loadUrl', url);
  }

  Future<void> loadData(String html) async {
    assert(html != null);
    return _channel.invokeMethod('loadData', html);
  }

  Future<String> evaluateJavaScript(String theScript) async {
    assert(theScript != null);
    var result = await _channel.invokeMethod("evaluate", theScript);
    return result as String;
  }

  Stream<String> get onPageFinished {
    var url = _pageFinished
        .receiveBroadcastStream()
        .map<String>((element) => element);
    return url;
  }

  Stream<String> get onPageStarted {
    var url =
        _pageStarted.receiveBroadcastStream().map<String>((element) => element);
    return url;
  }

  Future<dynamic> methodCallHandler(MethodCall call) async {
    switch (call.method) {
      case "shouldOverrideUrlLoading":
        // TODO implement properly on the android side
        var map = call.arguments; // as Map<String, dynamic>;
        var url = map["url"];
        if (shouldLoadUrlHandler != null) {
          return shouldLoadUrlHandler(url);
        }
        return false;
    }
  }
}
