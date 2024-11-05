// lib/src/core/app.dart

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:chaoffice/src/core/plugin_manager.dart';
import 'package:chaoffice/src/core/routes.dart';
import 'package:chaoffice/src/core/themes.dart';
import 'package:chaoffice/src/services/database_service.dart';
import 'package:chaoffice/src/services/auth_service.dart';
import 'package:provider/provider.dart';
import 'package:chaoffice/src/core/theme_provider.dart';

class Application extends StatefulWidget {
  final DatabaseService databaseService;
  final PluginManager pluginManager;

  const Application({
    super.key,
    required this.databaseService,
    required this.pluginManager,
  });

  @override
  ApplicationState createState() => ApplicationState();
}

class ApplicationState extends State<Application> {
  late AuthService _authService;
  late ThemeProvider _themeProvider;
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    _authService = AuthService(widget.databaseService);
    _themeProvider = ThemeProvider();
    _initializeServices();
  }

  Future<void> _initializeServices() async {
    try {
      await _authService.init();
      await _themeProvider.init();
      setState(() {
        _isInitialized = true;
      });
    } catch (e) {
      if (kDebugMode) {
        print('Error initializing services: $e');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (kDebugMode) {
      print("Application build method called");
    }

    if (!_isInitialized) {
      return const CupertinoApp(
        home: CupertinoPageScaffold(
          child: Center(child: CupertinoActivityIndicator()),
        ),
      );
    }

    return MultiProvider(
      providers: [
        Provider<DatabaseService>.value(value: widget.databaseService),
        ChangeNotifierProvider<AuthService>.value(value: _authService),
        ChangeNotifierProvider<PluginManager>.value(
            value: widget.pluginManager),
        ChangeNotifierProvider<ThemeProvider>.value(value: _themeProvider),
      ],
      child: Consumer2<AuthService, ThemeProvider>(
        builder: (context, authService, themeProvider, _) {
          if (kDebugMode) {
            print("Builder in Application called");
            print(
                "AuthService isAuthenticated: ${authService.isAuthenticated}");
            print("ThemeProvider isDarkMode: ${themeProvider.isDarkMode}");
          }

          return CupertinoApp(
            title: 'K Trade',
            theme: themeProvider.isDarkMode
                ? AppThemes.darkTheme
                : AppThemes.lightTheme,
            initialRoute: AppRoutes.login,
            onGenerateRoute: AppRoutes.generateRoute,
            localizationsDelegates: const [
              DefaultCupertinoLocalizations.delegate,
            ],
            supportedLocales: const [
              Locale('en', 'ar'),
            ],
            builder: (context, child) {
              if (kDebugMode) {
                print("CupertinoApp builder called");
              }
              return MediaQuery(
                data: MediaQuery.of(context).copyWith(
                  textScaler: const TextScaler.linear(1.0),
                ),
                child: child ?? const SizedBox.shrink(),
              );
            },
          );
        },
      ),
    );
  }
}
