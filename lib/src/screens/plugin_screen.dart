// lib/screens/plugin_screen.dart

import 'package:flutter/cupertino.dart';

class PluginScreen extends StatelessWidget {
  final String pluginName;

  const PluginScreen({super.key, required this.pluginName});

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        middle: Text(pluginName),
      ),
      child: SafeArea(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'Plugin: $pluginName',
                style:
                    const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 20),
              const Text('This is a placeholder for the plugin content.'),
            ],
          ),
        ),
      ),
    );
  }
}
