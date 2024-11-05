// lib/main.dart

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:chaoffice/src/core/app.dart';
import 'package:chaoffice/src/core/plugin_manager.dart';
import 'package:chaoffice/src/services/database_service.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  if (kDebugMode) {
    print("WidgetsFlutterBinding initialized");
  }

  sqfliteFfiInit();
  databaseFactory = databaseFactoryFfi;
  if (kDebugMode) {
    print("SQLite initialized");
  }

  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  if (kDebugMode) {
    print("Orientation set");
  }

  final databaseService = DatabaseService();
  await databaseService.initDatabase();
  if (kDebugMode) {
    print("Database initialized");
  }

  final pluginManager = PluginManager(databaseService);
  await pluginManager.initPlugins();
  if (kDebugMode) {
    print("PluginManager initialized");
  }

  if (kDebugMode) {
    print("Running app");
  }
  runApp(Application(
    databaseService: databaseService,
    pluginManager: pluginManager,
  ));
}
