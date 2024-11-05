// lib/models/plugin_model.dart

import 'package:flutter/cupertino.dart';

class PluginModel {
  final String id;
  final String name;
  final String version;
  final String description;
  final IconData icon;
  final bool isActive;
  final Widget Function() mainScreenBuilder;

  PluginModel({
    required this.id,
    required this.name,
    required this.version,
    required this.description,
    required this.icon,
    this.isActive = false,
    required this.mainScreenBuilder,
  });

  // Create a PluginModel from a Map (e.g., when loading from database)
  factory PluginModel.fromMap(Map<String, dynamic> map) {
    return PluginModel(
      id: map['id'],
      name: map['name'],
      version: map['version'],
      description: map['description'],
      icon: IconData(map['iconCodePoint'], fontFamily: 'CupertinoIcons'),
      isActive: map['isActive'] == 1,
      mainScreenBuilder: () =>
          const SizedBox(), // This needs to be handled separately
    );
  }

  // Convert PluginModel to a Map (e.g., when saving to database)
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'version': version,
      'description': description,
      'iconCodePoint': icon.codePoint,
      'isActive': isActive ? 1 : 0,
      // Note: mainScreenBuilder is not included as it's not easily serializable
    };
  }

  // Create a copy of the PluginModel with some fields changed
  PluginModel copyWith({
    String? id,
    String? name,
    String? version,
    String? description,
    IconData? icon,
    bool? isActive,
    Widget Function()? mainScreenBuilder,
  }) {
    return PluginModel(
      id: id ?? this.id,
      name: name ?? this.name,
      version: version ?? this.version,
      description: description ?? this.description,
      icon: icon ?? this.icon,
      isActive: isActive ?? this.isActive,
      mainScreenBuilder: mainScreenBuilder ?? this.mainScreenBuilder,
    );
  }

  @override
  String toString() {
    return 'PluginModel(id: $id, name: $name, version: $version, isActive: $isActive)';
  }
}
