// lib/screens/dashboard_screen.dart

import 'package:flutter/cupertino.dart';
import 'package:chaoffice/src/core/routes.dart';
import 'package:chaoffice/src/services/auth_service.dart';
import 'package:chaoffice/src/screens/custom_app_bar.dart';
import 'package:provider/provider.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: const CustomAppBar(
        title: 'Dashboard',
        showBackButton: true,
      ),
      child: SafeArea(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text(
                'Dashboard',
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 20),
              CupertinoButton(
                child: const Text('Plugin List'),
                onPressed: () {
                  Navigator.of(context).pushNamed(AppRoutes.pluginList);
                },
              ),
              const SizedBox(height: 20),
              CupertinoButton(
                child: const Text('Logout'),
                onPressed: () {
                  context.read<AuthService>().logout();
                  Navigator.of(context).pushReplacementNamed(AppRoutes.login);
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
