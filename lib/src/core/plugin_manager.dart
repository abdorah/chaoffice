// lib/core/plugin_manager.dart

import 'package:flutter/foundation.dart';
import 'package:chaoffice/src/models/plugin_model.dart';
import 'package:chaoffice/src/services/database_service.dart';
import 'package:collection/collection.dart';

class PluginManager extends ChangeNotifier {
  final DatabaseService _databaseService;
  List<PluginModel> _plugins = [];

  PluginManager(this._databaseService);

  List<PluginModel> get plugins => _plugins;

  Future<void> initPlugins() async {
    if (kDebugMode) {
      print("Initializing plugins");
    }
    try {
      final pluginMaps = await _databaseService.getData('plugins');
      _plugins = pluginMaps.map((map) => PluginModel.fromMap(map)).toList();
      if (kDebugMode) {
        print("Plugins initialized: ${_plugins.length} plugins loaded");
      }
      notifyListeners();
    } catch (e) {
      if (kDebugMode) {
        print("Error initializing plugins: $e");
      }
    }
  }

  Future<void> loadPlugin(String pluginId) async {
    if (kDebugMode) {
      print("Loading plugin: $pluginId");
    }
    final index = _plugins.indexWhere((plugin) => plugin.id == pluginId);
    if (index != -1) {
      final updatedPlugin = _plugins[index].copyWith(isActive: true);
      _plugins[index] = updatedPlugin;
      await _databaseService.updateData(
        'plugins',
        {'isActive': 1},
        'id = ?',
        [pluginId],
      );
      if (kDebugMode) {
        print("Plugin loaded: $pluginId");
      }
      notifyListeners();
    } else {
      if (kDebugMode) {
        print("Plugin not found: $pluginId");
      }
    }
  }

  Future<void> unloadPlugin(String pluginId) async {
    if (kDebugMode) {
      print("Unloading plugin: $pluginId");
    }
    final index = _plugins.indexWhere((plugin) => plugin.id == pluginId);
    if (index != -1) {
      final updatedPlugin = _plugins[index].copyWith(isActive: false);
      _plugins[index] = updatedPlugin;
      await _databaseService.updateData(
        'plugins',
        {'isActive': 0},
        'id = ?',
        [pluginId],
      );
      if (kDebugMode) {
        print("Plugin unloaded: $pluginId");
      }
      notifyListeners();
    } else {
      if (kDebugMode) {
        print("Plugin not found: $pluginId");
      }
    }
  }

  Future<void> addPlugin(PluginModel plugin) async {
    if (kDebugMode) {
      print("Adding plugin: ${plugin.name}");
    }
    _plugins.add(plugin);
    await _databaseService.insertData('plugins', plugin.toMap());
    if (kDebugMode) {
      print("Plugin added: ${plugin.name}");
    }
    notifyListeners();
  }

  Future<void> removePlugin(String pluginId) async {
    if (kDebugMode) {
      print("Removing plugin: $pluginId");
    }
    _plugins.removeWhere((plugin) => plugin.id == pluginId);
    await _databaseService.deleteData('plugins', 'id = ?', [pluginId]);
    if (kDebugMode) {
      print("Plugin removed: $pluginId");
    }
    notifyListeners();
  }

  PluginModel? getPlugin(String pluginId) {
    return _plugins.firstWhereOrNull((plugin) => plugin.id == pluginId);
  }

  List<PluginModel> getActivePlugins() {
    return _plugins.where((plugin) => plugin.isActive).toList();
  }

  Future<void> updatePlugin(PluginModel updatedPlugin) async {
    if (kDebugMode) {
      print("Updating plugin: ${updatedPlugin.name}");
    }
    final index =
        _plugins.indexWhere((plugin) => plugin.id == updatedPlugin.id);
    if (index != -1) {
      _plugins[index] = updatedPlugin;
      await _databaseService.updateData(
        'plugins',
        updatedPlugin.toMap(),
        'id = ?',
        [updatedPlugin.id],
      );
      if (kDebugMode) {
        print("Plugin updated: ${updatedPlugin.name}");
      }
      notifyListeners();
    } else {
      if (kDebugMode) {
        print("Plugin not found: ${updatedPlugin.id}");
      }
    }
  }
}
