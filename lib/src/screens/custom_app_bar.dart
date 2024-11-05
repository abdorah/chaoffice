// lib/widgets/custom_app_bar.dart

import 'package:flutter/cupertino.dart';

class CustomAppBar extends StatelessWidget implements ObstructingPreferredSizeWidget {
  final String title;
  final bool showBackButton;
  final VoidCallback? onBackPressed;

  const CustomAppBar({
    super.key,
    required this.title,
    this.showBackButton = true,
    this.onBackPressed,
  });

  @override
  Widget build(BuildContext context) {
    return CupertinoNavigationBar(
      middle: Text(title),
      leading: showBackButton
          ? CupertinoButton(
              padding: EdgeInsets.zero,
              onPressed: onBackPressed ?? () => Navigator.of(context).pop(),
              child: const Icon(CupertinoIcons.back),
            )
          : null,
    );
  }

  @override
  Size get preferredSize => const Size.fromHeight(44);

  @override
  bool shouldFullyObstruct(BuildContext context) => true;
}
