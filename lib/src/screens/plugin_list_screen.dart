// lib/screens/plugin_list_screen.dart

import 'package:flutter/cupertino.dart';
import 'package:chaoffice/src/screens/custom_app_bar.dart';
import 'package:provider/provider.dart';
import 'package:chaoffice/src/core/plugin_manager.dart';

class PluginListScreen extends StatelessWidget {
  const PluginListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final pluginManager = Provider.of<PluginManager>(context);

    return CupertinoPageScaffold(
      navigationBar: const CustomAppBar(
        title: 'Plugins',
        showBackButton: true,
      ),
      child: SafeArea(
        child: ListView.builder(
          itemCount: pluginManager.plugins.length,
          itemBuilder: (context, index) {
            final plugin = pluginManager.plugins[index];
            return CupertinoListTile(
              title: Text(plugin.name),
              subtitle: Text(plugin.version),
              trailing: CupertinoSwitch(
                value: plugin.isActive,
                onChanged: (value) {
                  if (value) {
                    pluginManager.loadPlugin(plugin.id);
                  } else {
                    pluginManager.unloadPlugin(plugin.id);
                  }
                },
              ),
            );
          },
        ),
      ),
    );
  }
}
