import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'theme/acorde_colors.dart';

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
    title: 'Acorde 2.0',
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: AcordeColors.verdeProfundo),
      scaffoldBackgroundColor: AcordeColors.offWhite,
      useMaterial3: true,
    ),
    darkTheme: ThemeData(
      colorScheme: ColorScheme.fromSeed(
        seedColor: AcordeColors.verdeProfundo,
        brightness: Brightness.dark,
      ),
      useMaterial3: true,
    ),
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
        _passwordController.clear();
      });
      _scheduleExpiration(_expiresAt!);
      if (_profile!['role'] == 'GESTOR') await _loadMembers();
      if (_profile!['role'] == 'COLABORADOR') {
        await _loadActivities();
        await _loadActions();
      }
    } catch (error) {
      setState(() => _error = _messageFrom(error));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _loadMembers() async {
    final response = await http.get(
      Uri.parse(
        '$_baseUrl/api/organizations/${_profile!['organizationId']}/members',
      ),
      headers: {'Authorization': 'Bearer $_accessToken'},
    );
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
    final response = await http.get(
      Uri.parse('$_baseUrl/api/carbon/activities'),
      headers: {'Authorization': 'Bearer $_accessToken'},
    );
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

  Future<void> _loadActions() async {
    final response = await http.get(
      Uri.parse('$_baseUrl/api/actions'),
      headers: {'Authorization': 'Bearer $_accessToken'},
    );
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
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final response = await http.patch(
        Uri.parse('$_baseUrl/api/actions/${action['id']}/state'),
        headers: {
          'Authorization': 'Bearer $_accessToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({'state': state}),
      );
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
      await _loadActions();
    } catch (error) {
      setState(() => _error = _messageFrom(error));
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
    final start = DateTime.tryParse(_periodStartController.text.trim());
    final end = DateTime.tryParse(_periodEndController.text.trim());
    final distanceValue = _distanceController.text.trim().replaceAll(',', '.');
    final daysValue = _daysController.text.trim().replaceAll(',', '.');
    final distance = double.tryParse(distanceValue);
    final days = double.tryParse(daysValue);
    if (start == null ||
        end == null ||
        start.isAfter(end) ||
        distance == null ||
        distance <= 0 ||
        days == null ||
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
      _periodStartController.text.trim(),
      _periodEndController.text.trim(),
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
          'Authorization': 'Bearer $_accessToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'idempotencyKey': _pendingIdempotencyKey,
          'periodStart': _periodStartController.text.trim(),
          'periodEnd': _periodEndController.text.trim(),
          'region': _regionController.text.trim(),
          'mode': _mode,
          'roundTripDistanceKm': distanceValue,
          'commutingDays': daysValue,
        }),
      );
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
      await _loadActivities();
    } catch (error) {
      setState(() => _error = _messageFrom(error));
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
      _error = message;
    });
  }

  String _messageFrom(Object error) =>
      error.toString().replaceFirst('Exception: ', '');

  @override
  Widget build(BuildContext context) {
    final profile = _profile;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Acorde 2.0'),
        actions: [
          IconButton(
            tooltip: widget.isDark ? 'Ativar tema claro' : 'Ativar tema escuro',
            onPressed: widget.onToggleTheme,
            icon: Icon(widget.isDark ? Icons.light_mode : Icons.dark_mode),
          ),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 520),
            child: ListView(
              padding: const EdgeInsets.all(24),
              shrinkWrap: true,
              children: [
                Image.asset(
                  widget.isDark
                      ? 'assets/branding/acorde-horizontal-inverso.png'
                      : 'assets/branding/acorde-horizontal-principal.png',
                  width: 300,
                  height: 90,
                  fit: BoxFit.contain,
                  semanticLabel: 'Acorde — gestão em sintonia',
                ),
                const SizedBox(height: 24),
                if (profile == null) _loginForm() else _accountCard(profile),
                if (_error != null) ...[
                  const SizedBox(height: 16),
                  Semantics(
                    liveRegion: true,
                    child: Text(
                      _error!,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _loginForm() => Form(
    key: _formKey,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('Entrar', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 16),
        TextFormField(
          controller: _emailController,
          keyboardType: TextInputType.emailAddress,
          autofillHints: const [AutofillHints.username],
          decoration: const InputDecoration(
            labelText: 'E-mail',
            border: OutlineInputBorder(),
          ),
          validator: (value) => value == null || value.trim().isEmpty
              ? 'Informe seu e-mail.'
              : null,
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _passwordController,
          obscureText: true,
          autofillHints: const [AutofillHints.password],
          decoration: const InputDecoration(
            labelText: 'Senha',
            border: OutlineInputBorder(),
          ),
          validator: (value) =>
              value == null || value.isEmpty ? 'Informe sua senha.' : null,
          onFieldSubmitted: (_) => _busy ? null : _login(),
        ),
        if (_memberships.isNotEmpty) ...[
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _selectedOrganizationId,
            decoration: const InputDecoration(
              labelText: 'Organização',
              border: OutlineInputBorder(),
            ),
            items: _memberships
                .map(
                  (membership) => DropdownMenuItem(
                    value: membership['organizationId'] as String,
                    child: Text(
                      '${membership['organizationName']} · ${membership['role']}',
                    ),
                  ),
                )
                .toList(),
            onChanged: (value) =>
                setState(() => _selectedOrganizationId = value),
            validator: (value) =>
                value == null ? 'Selecione uma organização.' : null,
          ),
        ],
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _busy ? null : _login,
          child: Text(_busy ? 'Aguarde…' : 'Entrar'),
        ),
      ],
    ),
  );

  Widget _accountCard(Map<String, dynamic> profile) => Card(
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Sessão autenticada',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 12),
          Text('${profile['name']} · ${profile['email']}'),
          const SizedBox(height: 8),
          Text('${profile['organizationName']} · ${profile['role']}'),
          const SizedBox(height: 8),
          Text('Expira às ${DateTime.parse(_expiresAt!).toLocal()}'),
          if (profile['role'] == 'GESTOR') ...[
            const Divider(height: 28),
            Text(
              'Membros da organização',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            for (final member in _members)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(member['name'] as String),
                subtitle: Text('${member['email']} · ${member['role']}'),
              ),
            OutlinedButton(
              onPressed: _busy ? null : _loadMembers,
              child: const Text('Atualizar membros'),
            ),
          ],
          if (profile['role'] == 'COLABORADOR') ...[
            const Divider(height: 28),
            Text(
              'Registrar deslocamento',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            const Text(
              'Informe seus deslocamentos casa–trabalho. A estimativa depende de fator compatível aprovado.',
            ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              initialValue: _mode,
              decoration: const InputDecoration(
                labelText: 'Modalidade',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'ONIBUS', child: Text('Ônibus')),
                DropdownMenuItem(value: 'METRO', child: Text('Metrô')),
                DropdownMenuItem(value: 'TREM', child: Text('Trem')),
                DropdownMenuItem(value: 'CARRO', child: Text('Carro')),
                DropdownMenuItem(value: 'MOTO', child: Text('Moto')),
                DropdownMenuItem(value: 'BICICLETA', child: Text('Bicicleta')),
                DropdownMenuItem(value: 'A_PE', child: Text('A pé')),
              ],
              onChanged: _busy
                  ? null
                  : (value) => setState(() => _mode = value ?? 'ONIBUS'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _regionController,
              decoration: const InputDecoration(
                labelText: 'Região (ex.: BR-SP)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _periodStartController,
              keyboardType: TextInputType.datetime,
              decoration: const InputDecoration(
                labelText: 'Início do período (AAAA-MM-DD)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _periodEndController,
              keyboardType: TextInputType.datetime,
              decoration: const InputDecoration(
                labelText: 'Fim do período (AAAA-MM-DD)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _distanceController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(
                labelText: 'Distância total de ida e volta (km)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _daysController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(
                labelText: 'Dias com deslocamento no período',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: _busy ? null : _submitCommute,
              child: Text(_busy ? 'Enviando…' : 'Registrar deslocamento'),
            ),
            const SizedBox(height: 20),
            Text(
              'Meu histórico',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            if (_activities.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Text('Nenhum registro disponível.'),
              ),
            for (final activity in _activities)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(
                  '${activity['category'] == 'DESLOCAMENTO' ? 'Deslocamento' : 'Energia'} · ${activity['state']}',
                ),
                subtitle: Text(
                  '${activity['periodStart']} a ${activity['periodEnd']} · ${activity['quantity']} ${activity['unit']}${activity['justification'] == null ? '' : '\n${activity['justification']}'}',
                ),
                isThreeLine: activity['justification'] != null,
                trailing: activity['emissionsKgCo2e'] == null
                    ? null
                    : Text('${activity['emissionsKgCo2e']} kg CO₂e'),
              ),
            OutlinedButton(
              onPressed: _busy ? null : _loadActivities,
              child: const Text('Atualizar histórico'),
            ),
            const Divider(height: 28),
            Text(
              'Ações atribuídas',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            if (_actions.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Text('Nenhuma ação atribuída disponível.'),
              ),
            for (final action in _actions)
              Card(
                child: ListTile(
                  title: Text(action['title'] as String),
                  subtitle: Text(
                    '${action['state']} · Prazo ${action['dueDate']}${action['description'] == null ? '' : '\n${action['description']}'}',
                  ),
                  isThreeLine: action['description'] != null,
                  trailing: action['state'] == 'PLANEJADA'
                      ? TextButton(
                          onPressed: _busy
                              ? null
                              : () => _updateAction(action, 'EM_ANDAMENTO'),
                          child: const Text('Iniciar'),
                        )
                      : action['state'] == 'EM_ANDAMENTO'
                      ? TextButton(
                          onPressed: _busy
                              ? null
                              : () => _updateAction(action, 'CONCLUIDA'),
                          child: const Text('Concluir'),
                        )
                      : const Icon(Icons.check_circle_outline),
                ),
              ),
            OutlinedButton(
              onPressed: _busy ? null : _loadActions,
              child: const Text('Atualizar ações'),
            ),
          ],
          const SizedBox(height: 12),
          FilledButton.tonal(
            onPressed: _busy ? null : _logout,
            child: const Text('Sair'),
          ),
        ],
      ),
    ),
  );
}
