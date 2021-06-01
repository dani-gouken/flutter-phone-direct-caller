import 'dart:async';

import 'package:flutter/services.dart';

class FlutterPhoneDirectCaller {
  static const MethodChannel _channel =
      const MethodChannel('flutter_phone_direct_caller');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool?> callNumber(String number, {int? simSlot}) async {
    return await directCall(number, simSlot: simSlot);
  }

  static Future<bool?> directCall(String number, {int? simSlot}) async {
    final bool? result = await _channel.invokeMethod(
        'callNumber', <String, Object?>{'number': number, 'simSlot': simSlot});
    return result;
  }
}
