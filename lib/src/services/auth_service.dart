// lib/services/auth_service.dart

import 'package:flutter/foundation.dart';
import 'package:chaoffice/src/services/database_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AuthService extends ChangeNotifier {
  final DatabaseService _databaseService;
  bool _isAuthenticated = false;
  String? _currentUser;
  bool _isLoading = true;

  AuthService(this._databaseService) {
    init();
  }

  bool get isAuthenticated => _isAuthenticated;
  String? get currentUser => _currentUser;
  bool get isLoading => _isLoading;

  Future<void> init() async {
    if (kDebugMode) {
      print("AuthService init started");
    }
    try {
      final prefs = await SharedPreferences.getInstance();
      // Clear the authentication state when the app starts
      await prefs.setBool('isAuthenticated', false);
      await prefs.remove('currentUser');
      _isAuthenticated = false;
      _currentUser = null;
      if (kDebugMode) {
        print("AuthService initialized: isAuthenticated=$_isAuthenticated, currentUser=$_currentUser");
      }
    } catch (e) {
      if (kDebugMode) {
        print('Error initializing AuthService: $e');
      }
      _isAuthenticated = false;
      _currentUser = null;
    } finally {
      _isLoading = false;
      if (kDebugMode) {
        print("AuthService init completed");
      }
      notifyListeners();
    }
  }

  Future<bool> login(String username, String password) async {
    try {
      final users = await _databaseService.getData('users');
      final user = users.firstWhere(
        (user) => user['username'] == username && user['password'] == password,
        orElse: () => {},
      );

      if (user.isNotEmpty) {
        _isAuthenticated = true;
        _currentUser = username;

        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool('isAuthenticated', true);
        await prefs.setString('currentUser', username);

        notifyListeners();
        return true;
      }
      return false;
    } catch (e) {
      if (kDebugMode) {
        print('Error during login: $e');
      }
      return false;
    }
  }

  Future<bool> userExists(String username) async {
    try {
      final users = await _databaseService.getData('users');
      return users.any((user) => user['username'] == username);
    } catch (e) {
      if (kDebugMode) {
        print('Error checking if user exists: $e');
      }
      return false;
    }
  }

  Future<void> logout() async {
    try {
      _isAuthenticated = false;
      _currentUser = null;

      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('isAuthenticated', false);
      await prefs.remove('currentUser');
    } catch (e) {
      if (kDebugMode) {
        print('Error during logout: $e');
      }
    } finally {
      notifyListeners();
    }
  }

  Future<bool> checkAuthentication() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      _isAuthenticated = prefs.getBool('isAuthenticated') ?? false;
      _currentUser = prefs.getString('currentUser');
      notifyListeners();
      return _isAuthenticated;
    } catch (e) {
      if (kDebugMode) {
        print('Error checking authentication: $e');
      }
      return false;
    }
  }
}
