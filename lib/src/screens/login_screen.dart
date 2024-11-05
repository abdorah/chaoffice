// lib/src/screens/login_screen.dart

import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import 'package:chaoffice/src/services/auth_service.dart';
import 'package:chaoffice/src/core/routes.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  LoginScreenState createState() => LoginScreenState();
}

class LoginScreenState extends State<LoginScreen> {
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _isLoading = false;

  void _showErrorDialog(String message) {
    if (!mounted) return; // Add this check
    showCupertinoDialog(
      context: context,
      builder: (BuildContext context) => CupertinoAlertDialog(
        title: const Text('Login Failed'),
        content: Text(message),
        actions: <CupertinoDialogAction>[
          CupertinoDialogAction(
            isDefaultAction: true,
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  Future<void> _handleLogin() async {
    if (_usernameController.text.isEmpty || _passwordController.text.isEmpty) {
      _showErrorDialog('Please enter both username and password');
      return;
    }

    setState(() => _isLoading = true);

    try {
      final authService = context.read<AuthService>();

      final userExists = await authService.userExists(_usernameController.text);
      if (!userExists) {
        _showErrorDialog('User does not exist');
        return;
      }

      final success = await authService.login(
        _usernameController.text,
        _passwordController.text,
      );

      if (!mounted) return;
      if (success) {
        Navigator.of(context).pushReplacementNamed(AppRoutes.home);
      } else {
        _showErrorDialog('Invalid username or password');
      }
    } catch (e) {
      _showErrorDialog('An error occurred. Please try again.');
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        middle: Text(
          'Login',
          style: CupertinoTheme.of(context).textTheme.navTitleTextStyle,
        ),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CupertinoTextField(
                controller: _usernameController,
                placeholder: 'Username',
                autocorrect: false,
                keyboardType: TextInputType.text,
                textInputAction: TextInputAction.next,
              ),
              const SizedBox(height: 16),
              CupertinoTextField(
                controller: _passwordController,
                placeholder: 'Password',
                obscureText: true,
                autocorrect: false,
                textInputAction: TextInputAction.done,
              ),
              const SizedBox(height: 32),
              CupertinoButton.filled(
                onPressed: _isLoading ? null : _handleLogin,
                child: const Text('Login'),
              ),
              if (_isLoading) const CupertinoActivityIndicator(),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }
}
