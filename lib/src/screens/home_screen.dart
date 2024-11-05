// lib/screens/home_screen.dart

import 'package:flutter/cupertino.dart';
import 'package:chaoffice/src/screens/sidebar_menu.dart';
import 'package:chaoffice/src/core/theme_provider.dart';
import 'package:chaoffice/src/screens/custom_app_bar.dart';
import 'package:provider/provider.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<ThemeProvider>(
      builder: (context, themeProvider, child) {
        return CupertinoPageScaffold(
          navigationBar: const CustomAppBar(
            title: 'Home',
            showBackButton: false,
          ),
          child: SafeArea(
            child: Row(
              children: [
                const SizedBox(width: 200, child: SidebarMenu()),
                Expanded(
                  child: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Welcome to K Trade',
                          style: CupertinoTheme.of(context).textTheme.navTitleTextStyle,
                        ),
                        const SizedBox(height: 20),
                        CupertinoButton(
                          child: Text(themeProvider.isDarkMode ? 'Switch to Light Theme' : 'Switch to Dark Theme'),
                          onPressed: () {
                            themeProvider.toggleTheme();
                          },
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
