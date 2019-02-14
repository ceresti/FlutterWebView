import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_native_web/flutter_native_web.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  WebController webController;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    FlutterNativeWeb flutterWebView = new FlutterNativeWeb(
      onWebCreated: onWebCreated,
      gestureRecognizers: <Factory<OneSequenceGestureRecognizer>>[
        Factory<OneSequenceGestureRecognizer>(
          () => TapGestureRecognizer(),
        ),
      ].toSet(),
    );

    return new MaterialApp(
      home: new Scaffold(
          appBar: new AppBar(
            title: const Text('Native WebView as Widget'),
          ),
          body: new SingleChildScrollView(
            child: new Column(
              children: <Widget>[
                new Text('Native WebView as Widget\n\n'),
                new Container(
                  child: flutterWebView,
                  height: 300.0,
                  width: 500.0,
                ),
              ],
            ),
          )),
    );
  }

  void onWebCreated(WebController webController) {
    this.webController = webController;
    this.webController.setShouldOverrideUrlLoadingRegEx = r".*google.*";
    this.webController.loadUrl("https://google.com/");
    this.webController.onPageStarted.listen((url) {
      print("Loading $url");
      if (RegExp(r".*google.*").hasMatch(url)) {
        this.webController.loadUrl("about:blank");
      }
    });
    this.webController.onPageFinished.listen((url) async {
      print("Finished loading $url");
      var result = await webController.evaluateJavaScript("1+1;");
      assert(result == "2");
    });
  }
}
