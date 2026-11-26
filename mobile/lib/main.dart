import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'theme/acorde_colors.dart';
import 'theme/acorde_theme.dart';

part 'screens.dart';

void main() => runApp(const AcordeApp());

class AcordeApp extends StatefulWidget {
  const AcordeApp({super.key});

  @override
  State<AcordeApp> createState() => _AcordeAppState();
}

class _AcordeAppState extends State<AcordeApp> {
  bool _darkMode = false;

  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'Acorde 2.0',
    theme: acordeTheme(Brightness.light),
    darkTheme: acordeTheme(Brightness.dark),
    themeMode: _darkMode ? ThemeMode.dark : ThemeMode.light,
    home: AuthenticationScreen(
      isDark: _darkMode,
      onToggleTheme: () => setState(() => _darkMode = !_darkMode),
    ),
  );
}

class AuthenticationScreen extends StatefulWidget {
  const AuthenticationScreen({
    required this.isDark,
    required this.onToggleTheme,
    super.key,
  });

  final bool isDark;
  final VoidCallback onToggleTheme;

  @override
  State<AuthenticationScreen> createState() => _AuthenticationScreenState();
}

class _AuthenticationScreenState extends State<AuthenticationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _regionController = TextEditingController(text: 'BR-SP');
  final _distanceController = TextEditingController();
  final _daysController = TextEditingController();
  final _periodStartController = TextEditingController();
  final _periodEndController = TextEditingController();
  late final String _baseUrl;
  String? _accessToken;
  String? _expiresAt;
  Map<String, dynamic>? _profile;
  List<Map<String, dynamic>> _memberships = [];
  List<Map<String, dynamic>> _members = [];
  List<Map<String, dynamic>> _activities = [];
  List<Map<String, dynamic>> _actions = [];
  String _mode = 'ONIBUS';
  String? _pendingIdempotencyKey;
  String? _selectedOrganizationId;
  String? _error;
  bool _busy = false;
  bool _showPassword = false;
  _Destination _destination = _Destination.home;
  _RecordFilter _recordFilter = _RecordFilter.all;
  _ActionFilter _actionFilter = _ActionFilter.open;
  bool _commuteOpen = false;
  String? _selectedActionId;
  Timer? _expirationTimer;
  String? _pendingFingerprint;

  @override
  void initState() {
    super.initState();
    const configured = String.fromEnvironment('ACORDE_API_URL');
    final defaultUrl = defaultTargetPlatform == TargetPlatform.android
        ? 'http://10.0.2.2:8080'
        : 'http://localhost:8080';
    _baseUrl = (configured.isEmpty ? defaultUrl : configured).replaceFirst(
      RegExp(r'/$'),
      '',
    );
  }

  @override
  void dispose() {
    _expirationTimer?.cancel();
    _emailController.dispose();
    _passwordController.dispose();
    _regionController.dispose();
    _distanceController.dispose();
    _daysController.dispose();
    _periodStartController.dispose();
    _periodEndController.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    final body = <String, dynamic>{
      'email': _emailController.text.trim(),
      'password': _passwordController.text,
      if (_selectedOrganizationId != null)
        'organizationId': _selectedOrganizationId,
    };
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/api/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(body),
      );
      if (!mounted) return;
      final result = jsonDecode(response.body) as Map<String, dynamic>;
      if (response.statusCode == 409 &&
          result['code'] == 'organization_selection_required') {
        setState(() {
          _memberships = (result['memberships'] as List<dynamic>)
              .cast<Map<String, dynamic>>();
          _error = 'Selecione a organização para continuar.';
        });
        return;
      }
      if (response.statusCode != 200) {
        throw Exception(result['message'] ?? 'Não foi possível entrar.');
      }
      setState(() {
        _accessToken = result['accessToken'] as String;
        _expiresAt = result['expiresAt'] as String;
        _profile = result['profile'] as Map<String, dynamic>;
        _memberships = [];
        _selectedOrganizationId = null;
        _showPassword = false;
        _passwordController.clear();
      });
      _scheduleExpiration(_expiresAt!);
      if (_profile!['role'] == 'GESTOR') await _loadMembers();
      if (_profile?['role'] == 'COLABORADOR') {
        await Future.wait([_loadActivities(), _loadActions()]);
      }
    } catch (error) {
      if (mounted && (_profile != null || _error == null)) {
        setState(() => _error = _messageFrom(error));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _loadMembers() async {
    final token = _accessToken;
    final response = await http.get(
      Uri.parse(
        '$_baseUrl/api/organizations/${_profile!['organizationId']}/members',
      ),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (!mounted || token != _accessToken) return;
    if (response.statusCode == 401) {
      _clearSession('A sessão expirou. Entre novamente.');
      return;
    }
    if (response.statusCode != 200) {
      throw Exception('Não foi possível carregar os membros da organização.');
    }
    final data = jsonDecode(response.body) as List<dynamic>;
    if (mounted) {
      setState(() => _members = data.cast<Map<String, dynamic>>());
    }
  }

  Future<void> _loadActivities() async {
    final token = _accessToken;
    final response = await http.get(
      Uri.parse('$_baseUrl/api/carbon/activities'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (!mounted || token != _accessToken) return;
    if (response.statusCode == 401) {
      _clearSession('A sessão expirou. Entre novamente.');
      return;
    }
    if (response.statusCode != 200) {
      throw Exception(
        'Não foi possível carregar o histórico de deslocamentos.',
      );
    }
    final data = jsonDecode(response.body) as List<dynamic>;
    if (mounted) {
      setState(() => _activities = data.cast<Map<String, dynamic>>());
    }
  }

  Future<void> _reprocessActivity(Map<String, dynamic> activity) async {
    final token = _accessToken;
    if (token == null || _busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final response = await http.post(
        Uri.parse(
          '$_baseUrl/api/carbon/activities/${activity['id']}/reprocess',
        ),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (!mounted || token != _accessToken) return;
      if (response.statusCode == 401) {
        _clearSession('A sessão expirou. Entre novamente.');
        return;
      }
      final updated = jsonDecode(response.body) as Map<String, dynamic>;
      if (response.statusCode != 200) {
        throw Exception(updated['message'] ?? 'Não foi possível recalcular.');
      }
      setState(() {
        _activities = [
          for (final item in _activities)
            if (item['id'] == updated['id']) updated else item,
        ];
        if (updated['state'] == 'VALIDA') _recordFilter = _RecordFilter.all;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            updated['state'] == 'VALIDA'
                ? 'Estimativa atualizada: ${_emissions(updated['emissionsKgCo2e'])}.'
                : 'O cálculo continua pendente. Confira o motivo no registro.',
          ),
        ),
      );
    } catch (error) {
      if (mounted && token == _accessToken) {
        setState(() => _error = _messageFrom(error));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _loadActions() async {
    final token = _accessToken;
    final response = await http.get(
      Uri.parse('$_baseUrl/api/actions'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (!mounted || token != _accessToken) return;
    if (response.statusCode == 401) {
      _clearSession('A sessão expirou. Entre novamente.');
      return;
    }
    if (response.statusCode != 200) {
      throw Exception('Não foi possível carregar as ações atribuídas.');
    }
    final data = jsonDecode(response.body) as List<dynamic>;
    if (mounted) setState(() => _actions = data.cast<Map<String, dynamic>>());
  }

  Future<void> _updateAction(Map<String, dynamic> action, String state) async {
    final token = _accessToken;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final response = await http.patch(
        Uri.parse('$_baseUrl/api/actions/${action['id']}/state'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({'state': state}),
      );
      if (!mounted || token != _accessToken) return;
      if (response.statusCode == 401) {
        _clearSession('A sessão expirou. Entre novamente.');
        return;
      }
      if (response.statusCode != 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        throw Exception(
          body['message'] ?? 'Não foi possível atualizar a ação.',
        );
      }
      final updated = jsonDecode(response.body) as Map<String, dynamic>;
      setState(
        () => _actions = [
          for (final item in _actions)
            if (item['id'] == updated['id']) updated else item,
        ],
      );
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Andamento da ação atualizado.')),
      );
    } catch (error) {
      if (mounted && token == _accessToken) {
        setState(() => _error = _messageFrom(error));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String _newIdempotencyKey() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final hex = bytes
        .map((byte) => byte.toRadixString(16).padLeft(2, '0'))
        .join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
  }

  Future<void> _submitCommute() async {
    final token = _accessToken;
    final startText = _periodStartController.text.trim();
    final endText = _periodEndController.text.trim();
    final start = DateTime.tryParse(startText);
    final end = DateTime.tryParse(endText);
    final distanceValue = _distanceController.text.trim().replaceAll(',', '.');
    final daysValue = _daysController.text.trim().replaceAll(',', '.');
    final distance = double.tryParse(distanceValue);
    final days = double.tryParse(daysValue);
    if (start == null ||
        end == null ||
        _isoDate(start) != startText ||
        _isoDate(end) != endText ||
        start.isAfter(end) ||
        distance == null ||
        !distance.isFinite ||
        distance <= 0 ||
        days == null ||
        !days.isFinite ||
        days <= 0 ||
        _regionController.text.trim().isEmpty) {
      setState(
        () => _error = 'Confira período, região, distância e dias informados.',
      );
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    final fingerprint = jsonEncode([
      startText,
      endText,
      _regionController.text.trim(),
      _mode,
      distanceValue,
      daysValue,
    ]);
    if (fingerprint != _pendingFingerprint) {
      _pendingIdempotencyKey = _newIdempotencyKey();
      _pendingFingerprint = fingerprint;
    }
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/api/carbon/activities/commutes'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'idempotencyKey': _pendingIdempotencyKey,
          'periodStart': startText,
          'periodEnd': endText,
          'region': _regionController.text.trim(),
          'mode': _mode,
          'roundTripDistanceKm': distanceValue,
          'commutingDays': daysValue,
        }),
      );
      if (!mounted || token != _accessToken) return;
      if (response.statusCode == 401) {
        _clearSession('A sessão expirou. Entre novamente.');
        return;
      }
      if (response.statusCode != 201) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        throw Exception(
          body['message'] ?? 'Não foi possível registrar o deslocamento.',
        );
      }
      _pendingIdempotencyKey = null;
      _pendingFingerprint = null;
      _distanceController.clear();
      _daysController.clear();
      final created = jsonDecode(response.body) as Map<String, dynamic>;
      setState(() {
        _activities = [
          created,
          ..._activities.where((activity) => activity['id'] != created['id']),
        ];
        _commuteOpen = false;
        _destination = _Destination.records;
        _recordFilter = _RecordFilter.all;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            created['state'] == 'PENDENTE'
                ? 'Deslocamento registrado. Cálculo pendente.'
                : 'Deslocamento registrado.',
          ),
        ),
      );
    } catch (error) {
      if (mounted && token == _accessToken) {
        setState(() => _error = _messageFrom(error));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _logout() async {
    if (_accessToken == null) return;
    setState(() => _busy = true);
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/api/auth/logout'),
        headers: {'Authorization': 'Bearer $_accessToken'},
      );
      if (response.statusCode != 204) {
        throw Exception('API não confirmou o logout.');
      }
      _clearSession(null);
    } catch (_) {
      _clearSession(
        'Não foi possível confirmar o logout na API. O token local foi removido e expira em até 15 minutos.',
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _scheduleExpiration(String expiresAt) {
    _expirationTimer?.cancel();
    final expiry = DateTime.parse(expiresAt).toLocal();
    final wait = expiry.difference(DateTime.now());
    _expirationTimer = Timer(wait.isNegative ? Duration.zero : wait, () {
      _clearSession('A sessão expirou. Entre novamente.');
    });
  }

  void _clearSession(String? message) {
    _expirationTimer?.cancel();
    if (!mounted) return;
    setState(() {
      _accessToken = null;
      _expiresAt = null;
      _profile = null;
      _members = [];
      _activities = [];
      _actions = [];
      _pendingIdempotencyKey = null;
      _pendingFingerprint = null;
      _destination = _Destination.home;
      _recordFilter = _RecordFilter.all;
      _actionFilter = _ActionFilter.open;
      _commuteOpen = false;
      _selectedActionId = null;
      _mode = 'ONIBUS';
      _regionController.text = 'BR-SP';
      _periodStartController.clear();
      _periodEndController.clear();
      _distanceController.clear();
      _daysController.clear();
      _error = message;
    });
  }

  String _messageFrom(Object error) =>
      error.toString().replaceFirst('Exception: ', '');

  void _changeView(VoidCallback change) => setState(change);

  @override
  Widget build(BuildContext context) => _buildScreen();
}
