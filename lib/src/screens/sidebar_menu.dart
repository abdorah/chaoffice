// lib/src/screens/sidebar_menu.dart

import 'package:flutter/cupertino.dart';
import 'package:chaoffice/src/core/routes.dart';
import 'package:chaoffice/src/services/auth_service.dart';
import 'package:provider/provider.dart';

class SidebarMenu extends StatelessWidget {
  const SidebarMenu({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const SizedBox(height: 50),
        _buildMenuItem(context, 'Home', AppRoutes.home),
        _buildMenuItem(context, 'Dashboard', AppRoutes.dashboard),
        _buildMenuItem(context, 'Plugins', AppRoutes.pluginList),
        const Spacer(),
        _buildMenuItem(context, 'Logout', AppRoutes.login, isLogout: true),
      ],
    );
  }

  Widget _buildMenuItem(BuildContext context, String title, String route,
      {bool isLogout = false}) {
    return CupertinoButton(
      child: Text(title),
      onPressed: () {
        if (isLogout) {
          context.read<AuthService>().logout();
          Navigator.of(context).pushReplacementNamed(route);
        } else {
          Navigator.of(context).pushReplacementNamed(route);
        }
      },
    );
  }
}
