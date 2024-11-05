// lib/src/screens/sidebar_menu.dart

import 'package:chaoffice/src/core/themes.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class SideBarMenu extends StatelessWidget {
  const SideBarMenu({
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: ListView(
        children: [
          DrawerHeader(
            child: Image.asset(
              'assets/images/flutter_logo.png',
              width: 1,
              height: 1,
            ),
          ),
          const DrawerListTile(
            title: 'Home',
            icon: Icons.pie_chart_outline,
            route: 'Home',
          ),
          const DrawerListTile(
            title: 'Dashboard',
            icon: Icons.dashboard_outlined,
            route: 'Dashboard',
          ),
          const DrawerListTile(
            title: 'Plugins',
            icon: Icons.dashboard_customize_outlined,
            route: 'Plugins',
          ),
          const DrawerListTile(
            title: 'Documents',
            icon: Icons.document_scanner_outlined,
            route: 'logout',
          ),
          const DrawerListTile(
            title: 'Store',
            icon: Icons.storefront_outlined,
            route: 'logout',
          ),
          const DrawerListTile(
            title: 'Notification',
            icon: Icons.notification_add_outlined,
            route: 'logout',
          ),
          const DrawerListTile(
            title: 'Settings',
            icon: Icons.settings,
            route: 'logout',
          ),
          const DrawerListTile(
            title: 'log out',
            icon: Icons.logout,
            route: 'logout',
          ),
        ],
      ),
    );
  }
}

class DrawerListTile extends StatelessWidget {
  const DrawerListTile({
    super.key,
    required this.title,
    required this.icon,
    required this.route,
  });

  final String title, route;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: () {
        CupertinoButton(
            child: Text(title),
            onPressed: () {
              Navigator.of(context).pushReplacementNamed(route);
            });
      },
      horizontalTitleGap: 0.0,
      leading: Icon(icon),
      title: Text(title, style: AppThemes.getTextStyle(context)),
    );
  }
}
