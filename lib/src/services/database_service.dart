// lib/services/database_service.dart

import 'package:flutter/foundation.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'dart:convert';
import 'package:flutter/services.dart' show rootBundle;

class DatabaseService {
  static final DatabaseService _instance = DatabaseService._internal();
  factory DatabaseService() => _instance;
  DatabaseService._internal();

  static Database? _database;
  Map<String, dynamic>? _queries;

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await initDatabase();
    return _database!;
  }

  Future<void> loadQueries() async {
    if (kDebugMode) {
      print("Loading queries");
    }
    try {
      final String jsonString =
          await rootBundle.loadString('assets/queries.json');
      _queries = json.decode(jsonString);
      if (kDebugMode) {
        print("Queries loaded successfully");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Error loading queries: $e");
      }
    }
  }

  String getQuery(String key) {
    return _queries?[key] ?? '';
  }

  Future<Database> initDatabase() async {
    if (kDebugMode) {
      print("Initializing database");
    }
    await loadQueries();
    String path = join(await getDatabasesPath(), 'chaoffice.db');
    if (kDebugMode) {
      print("Database path: $path");
    }
    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDb,
    );
  }

  Future<void> _createDb(Database db, int version) async {
    if (kDebugMode) {
      print("Creating database tables");
    }
    try {
      await db.execute(getQuery('create_plugins_table'));
      await db.execute(getQuery('create_users_table'));

      // Add a default user
      await db
          .rawInsert(getQuery('insert_default_user'), ['admin', 'password']);
      if (kDebugMode) {
        print("Database tables created successfully");
      }
    } catch (e) {
      if (kDebugMode) {
        print('Error creating database: $e');
      }
    }
  }

  Future<int> insertData(String table, Map<String, dynamic> data) async {
    try {
      final db = await database;
      return await db.insert(table, data,
          conflictAlgorithm: ConflictAlgorithm.replace);
    } catch (e) {
      if (kDebugMode) {
        print('Error inserting data into $table: $e');
      }
      return -1;
    }
  }

  Future<List<Map<String, dynamic>>> getData(String table) async {
    try {
      final db = await database;
      return await db.query(table);
    } catch (e) {
      if (kDebugMode) {
        print('Error getting data from $table: $e');
      }
      return [];
    }
  }

  Future<int> updateData(String table, Map<String, dynamic> data,
      String whereClause, List<dynamic> whereArgs) async {
    try {
      final db = await database;
      return await db.update(table, data,
          where: whereClause, whereArgs: whereArgs);
    } catch (e) {
      if (kDebugMode) {
        print('Error updating data in $table: $e');
      }
      return -1;
    }
  }

  Future<int> deleteData(
      String table, String whereClause, List<dynamic> whereArgs) async {
    try {
      final db = await database;
      return await db.delete(table, where: whereClause, whereArgs: whereArgs);
    } catch (e) {
      if (kDebugMode) {
        print('Error deleting data from $table: $e');
      }
      return -1;
    }
  }
}
