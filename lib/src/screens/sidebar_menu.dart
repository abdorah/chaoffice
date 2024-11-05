// lib/src/screens/sidebar_menu.dart

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
            icon: CupertinoIcons.chart_pie,
            route: 'Home',
          ),
          const DrawerListTile(
            title: 'Dashboard',
            icon: CupertinoIcons.square_grid_2x2,
            route: 'Dashboard',
          ),
          const DrawerListTile(
            title: 'Plugins',
            icon: CupertinoIcons.app_badge,
            route: 'Plugins',
          ),
          const DrawerListTile(
            title: 'Documents',
            icon: CupertinoIcons.doc,
            route: 'logout',
          ),
          const DrawerListTile(
            title: 'Store',
            icon: CupertinoIcons.collections,
            route: 'logout',
          ),
          const DrawerListTile(
            title: 'Notification',
            icon: CupertinoIcons.bell,
            route: 'logout',
          ),
          const DrawerListTile(
            title: 'Settings',
            icon: CupertinoIcons.settings,
            route: 'logout',
          ),
          const DrawerListTile(
            title: 'log out',
            icon: CupertinoIcons.person_alt_circle,
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
      title: Text(
        title,
        style: const TextStyle(color: Colors.white54),
      ),
    );
  }
}
