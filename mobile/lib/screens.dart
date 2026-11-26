part of 'main.dart';

enum _Destination {
  home('Início', Icons.home_outlined, Icons.home),
  records('Registros', Icons.receipt_long_outlined, Icons.receipt_long),
  actions('Ações', Icons.task_alt_outlined, Icons.task_alt),
  profile('Perfil', Icons.person_outline, Icons.person);

  const _Destination(this.label, this.icon, this.selectedIcon);
  final String label;
  final IconData icon;
  final IconData selectedIcon;
}

enum _RecordFilter {
  all('Todos'),
  calculated('Calculados'),
  pending('Pendentes');

  const _RecordFilter(this.label);
  final String label;
}

enum _ActionFilter {
  open('Abertas'),
  completed('Concluídas'),
  canceled('Canceladas');

  const _ActionFilter(this.label);
  final String label;
}

const _commuteModes = {
  'ONIBUS': 'Ônibus',
  'METRO': 'Metrô',
  'TREM': 'Trem',
  'CARRO': 'Carro',
  'MOTO': 'Moto',
  'BICICLETA': 'Bicicleta',
  'A_PE': 'A pé',
};

const _months = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
];

String _twoDigits(int value) => value.toString().padLeft(2, '0');

String _isoDate(DateTime date) =>
    '${date.year.toString().padLeft(4, '0')}-${_twoDigits(date.month)}-${_twoDigits(date.day)}';

String _displayDate(Object? value) {
  final date = DateTime.tryParse('$value');
  return date == null
      ? '—'
      : '${_twoDigits(date.day)}/${_twoDigits(date.month)}/${date.year}';
}

String _emissions(Object? value) =>
    value == null ? '—' : '${value.toString().replaceAll('.', ',')} kg CO₂e';

String _statusLabel(Object? status) => switch (status) {
  'VALIDA' => 'Calculado',
  'PENDENTE' => 'Pendente',
  'SUBSTITUIDA' => 'Substituído',
  'PLANEJADA' => 'Planejada',
  'EM_ANDAMENTO' => 'Em andamento',
  'CONCLUIDA' => 'Concluída',
  'CANCELADA' => 'Cancelada',
  _ => 'Indefinido',
};

bool _isOpen(Map<String, dynamic> action) =>
    action['state'] == 'PLANEJADA' || action['state'] == 'EM_ANDAMENTO';

extension _Screens on _AuthenticationScreenState {
  ThemeData get _theme => Theme.of(context);
  ColorScheme get _colors => _theme.colorScheme;

  List<Map<String, dynamic>> get _commutes =>
      _activities.where((item) => item['category'] == 'DESLOCAMENTO').toList()
        ..sort(
          (first, second) => (second['periodEnd'] as String).compareTo(
            first['periodEnd'] as String,
          ),
        );

  void _navigate(_Destination destination) {
    _changeView(() {
      _destination = destination;
      _commuteOpen = false;
      _selectedActionId = null;
      _error = null;
    });
  }

  void _openCommute() {
    final today = _isoDate(DateTime.now());
    _changeView(() {
      if (_periodStartController.text.isEmpty) {
        _periodStartController.text = today;
      }
      if (_periodEndController.text.isEmpty) _periodEndController.text = today;
      _error = null;
      _commuteOpen = true;
    });
  }

  Future<void> _refresh(Future<void> Function() loader) async {
    final token = _accessToken;
    _changeView(() {
      _busy = true;
      _error = null;
    });
    try {
      await loader();
    } catch (error) {
      if (mounted && token == _accessToken) {
        _changeView(() => _error = _messageFrom(error));
      }
    } finally {
      if (mounted) _changeView(() => _busy = false);
    }
  }

  Widget _buildScreen() {
    final profile = _profile;
    if (profile == null) return _loginScreen();
    if (profile['role'] == 'GESTOR') return _managerScreen(profile);
    if (_commuteOpen) {
      return _detailScreen('Registrar deslocamento', _commuteForm());
    }
    final selected = _actions
        .where((item) => item['id'] == _selectedActionId)
        .firstOrNull;
    if (selected != null) return _actionDetail(selected);

    final title = switch (_destination) {
      _Destination.home => null,
      _Destination.records => 'Meus registros',
      _Destination.actions => 'Minhas ações',
      _Destination.profile => 'Meu perfil',
    };
    final children = switch (_destination) {
      _Destination.home => _home(profile),
      _Destination.records => _records(),
      _Destination.actions => _assignedActions(),
      _Destination.profile => _profileContent(profile),
    };

    return Scaffold(
      appBar: title == null
          ? null
          : AppBar(
              title: Text(title),
              centerTitle: true,
              leading: IconButton(
                tooltip: 'Voltar',
                onPressed: () => _navigate(_Destination.home),
                icon: const BackButtonIcon(),
              ),
              actions: [
                if (_destination == _Destination.records)
                  IconButton(
                    tooltip: 'Atualizar registros',
                    onPressed: _busy ? null : () => _refresh(_loadActivities),
                    icon: const Icon(Icons.refresh),
                  ),
                if (_destination == _Destination.actions)
                  IconButton(
                    tooltip: 'Atualizar ações',
                    onPressed: _busy ? null : () => _refresh(_loadActions),
                    icon: const Icon(Icons.refresh),
                  ),
              ],
            ),
      body: SafeArea(
        bottom: false,
        child: _ScreenList(
          key: ValueKey(_destination),
          children: [
            if (_busy)
              const LinearProgressIndicator(semanticsLabel: 'Carregando'),
            if (_error != null) _errorMessage(),
            ...children,
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _destination.index,
        onDestinationSelected: _busy
            ? null
            : (index) => _navigate(_Destination.values[index]),
        destinations: [
          for (final item in _Destination.values)
            NavigationDestination(
              icon: Icon(item.icon),
              selectedIcon: Icon(
                item.selectedIcon,
                color: _colors.onSecondaryContainer,
              ),
              label: item.label,
            ),
        ],
      ),
    );
  }

  Widget _loginScreen() => Scaffold(
    body: Stack(
      children: [
        Positioned.fill(
          child: IgnorePointer(
            child: Align(
              alignment: Alignment.bottomCenter,
              child: CustomPaint(
                painter: _BandsPainter(),
                child: const SizedBox(width: double.infinity, height: 100),
              ),
            ),
          ),
        ),
        SafeArea(
          child: _ScreenList(
            children: [
              Align(alignment: Alignment.centerRight, child: _themeButton()),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 24, 16, 22),
                child: Center(
                  child: Image.asset(
                    widget.isDark
                        ? 'assets/branding/acorde-horizontal-inverso.png'
                        : 'assets/branding/acorde-horizontal-principal.png',
                    width: 228,
                    height: 58,
                    fit: BoxFit.contain,
                    semanticLabel: 'Acorde 2.0',
                  ),
                ),
              ),
              _Surface(
                padding: const EdgeInsets.all(22),
                child: Form(
                  key: _formKey,
                  child: AutofillGroup(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _heading(
                          'Entre na sua conta',
                          _theme.textTheme.headlineSmall,
                        ),
                        const SizedBox(height: 18),
                        TextFormField(
                          controller: _emailController,
                          enabled: !_busy,
                          keyboardType: TextInputType.emailAddress,
                          autofillHints: const [AutofillHints.username],
                          textInputAction: TextInputAction.next,
                          decoration: const InputDecoration(
                            labelText: 'E-mail',
                            hintText: 'seu.email@organizacao.com',
                          ),
                          validator: (value) =>
                              value == null || value.trim().isEmpty
                              ? 'Informe seu e-mail.'
                              : null,
                          onChanged: (_) {
                            if (_memberships.isNotEmpty) {
                              _changeView(() {
                                _memberships = [];
                                _selectedOrganizationId = null;
                                _error = null;
                              });
                            }
                          },
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _passwordController,
                          enabled: !_busy,
                          obscureText: !_showPassword,
                          autofillHints: const [AutofillHints.password],
                          decoration: InputDecoration(
                            labelText: 'Senha',
                            suffixIcon: IconButton(
                              tooltip: _showPassword
                                  ? 'Ocultar senha'
                                  : 'Mostrar senha',
                              onPressed: _busy
                                  ? null
                                  : () => _changeView(
                                      () => _showPassword = !_showPassword,
                                    ),
                              icon: Icon(
                                _showPassword
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                              ),
                            ),
                          ),
                          validator: (value) => value == null || value.isEmpty
                              ? 'Informe sua senha.'
                              : null,
                          onFieldSubmitted: (_) {
                            if (!_busy) _login();
                          },
                        ),
                        if (_memberships.isNotEmpty) ...[
                          const SizedBox(height: 16),
                          DropdownButtonFormField<String>(
                            initialValue: _selectedOrganizationId,
                            isExpanded: true,
                            decoration: const InputDecoration(
                              labelText: 'Organização',
                            ),
                            items: [
                              for (final membership in _memberships)
                                DropdownMenuItem(
                                  value: membership['organizationId'] as String,
                                  child: Text(
                                    membership['organizationName'] as String,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                            ],
                            onChanged: _busy
                                ? null
                                : (value) => _changeView(
                                    () => _selectedOrganizationId = value,
                                  ),
                            validator: (value) => value == null
                                ? 'Selecione uma organização.'
                                : null,
                          ),
                        ],
                        if (_error != null) ...[
                          const SizedBox(height: 12),
                          _errorMessage(),
                        ],
                        const SizedBox(height: 18),
                        FilledButton(
                          onPressed: _busy ? null : _login,
                          child: Text(_busy ? 'Entrando…' : 'Entrar'),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 80),
            ],
          ),
        ),
      ],
    ),
  );

  List<Widget> _home(Map<String, dynamic> profile) {
    final records = _commutes;
    final latest = records
        .where(
          (item) =>
              item['state'] == 'VALIDA' &&
              item['current'] == true &&
              item['emissionsKgCo2e'] != null,
        )
        .firstOrNull;
    final givenName = (profile['name'] as String)
        .trim()
        .split(RegExp(r'\s+'))
        .first;
    final openCount = _actions.where(_isOpen).length;

    return [
      const SizedBox(height: 12),
      _heading('Olá, $givenName!', _theme.textTheme.headlineMedium),
      Text(
        'Acompanhe seus registros e ações ambientais.',
        style: TextStyle(color: _colors.onSurfaceVariant),
      ),
      const SizedBox(height: 15),
      _Surface(
        tinted: true,
        padding: const EdgeInsets.all(18),
        child: Stack(
          children: [
            Positioned.fill(
              child: Align(
                alignment: Alignment.centerRight,
                child: ExcludeSemantics(
                  child: Icon(
                    Icons.cloud_outlined,
                    size: 78,
                    color: _colors.primary.withValues(alpha: .13),
                  ),
                ),
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _heading(
                  'Suas emissões registradas',
                  _theme.textTheme.titleMedium,
                ),
                const SizedBox(height: 4),
                Text(
                  'Último deslocamento calculado',
                  style: TextStyle(color: _colors.onSecondaryContainer),
                ),
                const SizedBox(height: 12),
                Text(
                  latest == null
                      ? 'Sem cálculo válido'
                      : _emissions(latest['emissionsKgCo2e']),
                  style: _theme.textTheme.headlineSmall,
                ),
                const SizedBox(height: 4),
                Text(
                  latest == null
                      ? 'Consulte o estado dos seus registros.'
                      : _period(latest),
                  style: TextStyle(color: _colors.onSecondaryContainer),
                ),
              ],
            ),
          ],
        ),
      ),
      const SizedBox(height: 11),
      IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              child: _shortcut(
                Icons.directions_car_outlined,
                'Registrar deslocamento',
                _openCommute,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _shortcut(
                Icons.fact_check_outlined,
                'Minhas ações',
                () => _navigate(_Destination.actions),
              ),
            ),
          ],
        ),
      ),
      const SizedBox(height: 12),
      _Surface(
        onTap: _busy ? null : () => _navigate(_Destination.actions),
        child: Row(
          children: [
            const _IconTile(Icons.task_alt),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _heading(
                    'Ações atribuídas a você',
                    _theme.textTheme.titleMedium,
                  ),
                  const SizedBox(height: 3),
                  Text(
                    _busy ? 'Carregando…' : '$openCount em aberto',
                    style: TextStyle(color: _colors.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right),
          ],
        ),
      ),
    ];
  }

  Widget _shortcut(IconData icon, String label, VoidCallback tap) => _Surface(
    tinted: true,
    onTap: _busy ? null : tap,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _IconTile(icon),
        const SizedBox(height: 8),
        _heading(label, _theme.textTheme.titleMedium),
      ],
    ),
  );

  Widget _detailScreen(String title, List<Widget> children, {Widget? footer}) =>
      PopScope(
        canPop: false,
        onPopInvokedWithResult: (didPop, _) {
          if (!didPop && !_busy) _closeDetail();
        },
        child: Scaffold(
          appBar: AppBar(
            title: Text(title),
            centerTitle: true,
            leading: IconButton(
              tooltip: 'Voltar',
              onPressed: _busy ? null : _closeDetail,
              icon: const BackButtonIcon(),
            ),
          ),
          body: SafeArea(
            bottom: footer == null,
            child: _ScreenList(
              children: [
                ...children,
                if (_error != null) ...[
                  const SizedBox(height: 12),
                  _errorMessage(),
                ],
              ],
            ),
          ),
          bottomNavigationBar: footer == null
              ? null
              : SafeArea(
                  top: false,
                  minimum: const EdgeInsets.fromLTRB(18, 8, 18, 16),
                  child: Center(
                    heightFactor: 1,
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 524),
                      child: SizedBox(width: double.infinity, child: footer),
                    ),
                  ),
                ),
        ),
      );

  void _closeDetail() => _changeView(() {
    _commuteOpen = false;
    _selectedActionId = null;
    _error = null;
  });

  List<Widget> _commuteForm() => [
    const Text('Informe seu deslocamento casa–trabalho no período.'),
    const SizedBox(height: 16),
    Row(
      children: [
        Expanded(
          child: _commuteField(
            _periodStartController,
            'Data inicial',
            hint: 'AAAA-MM-DD',
            type: TextInputType.datetime,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _commuteField(
            _periodEndController,
            'Data final',
            hint: 'AAAA-MM-DD',
            type: TextInputType.datetime,
          ),
        ),
      ],
    ),
    const SizedBox(height: 16),
    DropdownButtonFormField<String>(
      initialValue: _mode,
      isExpanded: true,
      decoration: const InputDecoration(
        labelText: 'Modalidade',
        prefixIcon: Icon(Icons.directions_car_outlined),
      ),
      items: [
        for (final mode in _commuteModes.entries)
          DropdownMenuItem(value: mode.key, child: Text(mode.value)),
      ],
      onChanged: _busy
          ? null
          : (value) {
              if (value != null) _changeView(() => _mode = value);
            },
    ),
    const SizedBox(height: 16),
    _commuteField(
      _distanceController,
      'Distância de ida e volta (km)',
      hint: 'Ex.: 12,0',
    ),
    const SizedBox(height: 16),
    _commuteField(_daysController, 'Dias com deslocamento', hint: 'Ex.: 1'),
    const SizedBox(height: 16),
    _commuteField(
      _regionController,
      'Região',
      hint: 'Ex.: BR-SP',
      type: TextInputType.text,
    ),
    const SizedBox(height: 15),
    _Surface(
      tinted: true,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline, size: 20, color: _colors.primary),
          const SizedBox(width: 9),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _heading('Cálculo automático', _theme.textTheme.titleMedium),
                const SizedBox(height: 4),
                const Text(
                  'A estimativa depende de um fator compatível. Se o fator estiver indisponível, o registro fica pendente.',
                ),
              ],
            ),
          ),
        ],
      ),
    ),
    const SizedBox(height: 16),
    FilledButton(
      onPressed: _busy ? null : _submitCommute,
      child: Text(_busy ? 'Enviando…' : 'Registrar'),
    ),
  ];

  Widget _commuteField(
    TextEditingController controller,
    String label, {
    String? hint,
    TextInputType type = const TextInputType.numberWithOptions(decimal: true),
  }) => TextField(
    controller: controller,
    enabled: !_busy,
    keyboardType: type,
    textInputAction: TextInputAction.next,
    decoration: InputDecoration(labelText: label, hintText: hint),
  );

  List<Widget> _records() {
    final visible = _commutes.where(
      (item) => switch (_recordFilter) {
        _RecordFilter.all => true,
        _RecordFilter.calculated => item['state'] == 'VALIDA',
        _RecordFilter.pending => item['state'] == 'PENDENTE',
      },
    );
    final groups = <String, List<Map<String, dynamic>>>{};
    for (final item in visible) {
      final date = DateTime.tryParse(item['periodEnd'] as String);
      final label = date == null
          ? 'Sem período'
          : '${_months[date.month - 1]}/${date.year}';
      groups.putIfAbsent(label, () => []).add(item);
    }

    return [
      FilledButton.icon(
        onPressed: _busy ? null : _openCommute,
        icon: const Icon(Icons.add),
        label: const Text('Registrar deslocamento'),
      ),
      const SizedBox(height: 12),
      Wrap(
        spacing: 8,
        runSpacing: 4,
        children: [
          for (final filter in _RecordFilter.values)
            ChoiceChip(
              label: Text(filter.label),
              selected: filter == _recordFilter,
              showCheckmark: false,
              onSelected: (selected) {
                if (selected) _changeView(() => _recordFilter = filter);
              },
            ),
        ],
      ),
      const SizedBox(height: 12),
      if (groups.isEmpty && !_busy && _error == null)
        const _EmptyCard(
          Icons.receipt_long_outlined,
          'Nenhum registro disponível neste filtro.',
        ),
      for (final group in groups.entries) ...[
        _heading(group.key, _theme.textTheme.titleMedium),
        const SizedBox(height: 9),
        for (final item in group.value) ...[
          _recordCard(item),
          const SizedBox(height: 8),
        ],
        const SizedBox(height: 10),
      ],
    ];
  }

  Widget _recordCard(Map<String, dynamic> item) => _Surface(
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _IconTile(Icons.directions_car_outlined),
        const SizedBox(width: 11),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _heading(
                'Deslocamento casa–trabalho',
                _theme.textTheme.titleMedium,
              ),
              const SizedBox(height: 4),
              Text(
                _emissions(item['emissionsKgCo2e']),
                style: _theme.textTheme.titleLarge,
              ),
              const SizedBox(height: 4),
              Wrap(
                spacing: 10,
                runSpacing: 4,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [Text(_period(item)), _StatusPill(item['state'])],
              ),
              if (item['justification'] case final String detail) ...[
                const SizedBox(height: 5),
                Text(detail, style: TextStyle(color: _colors.onSurfaceVariant)),
              ],
              if (item['canReprocess'] == true && item['state'] == 'PENDENTE')
                TextButton(
                  onPressed: _busy ? null : () => _reprocessActivity(item),
                  child: const Text('Recalcular estimativa'),
                ),
            ],
          ),
        ),
      ],
    ),
  );

  String _period(Map<String, dynamic> item) {
    final start = _displayDate(item['periodStart']);
    final end = _displayDate(item['periodEnd']);
    return start == end ? end : '$start a $end';
  }

  List<Widget> _assignedActions() {
    final visible = _actions.where(
      (item) => switch (_actionFilter) {
        _ActionFilter.open => _isOpen(item),
        _ActionFilter.completed => item['state'] == 'CONCLUIDA',
        _ActionFilter.canceled => item['state'] == 'CANCELADA',
      },
    );
    return [
      Wrap(
        spacing: 6,
        runSpacing: 4,
        children: [
          for (final filter in _ActionFilter.values)
            ChoiceChip(
              label: Text(
                filter == _ActionFilter.open
                    ? 'Abertas (${_actions.where(_isOpen).length})'
                    : filter.label,
              ),
              selected: filter == _actionFilter,
              showCheckmark: false,
              onSelected: (selected) {
                if (selected) _changeView(() => _actionFilter = filter);
              },
            ),
        ],
      ),
      const SizedBox(height: 12),
      if (visible.isEmpty && !_busy && _error == null)
        const _EmptyCard(
          Icons.fact_check_outlined,
          'Nenhuma ação atribuída neste filtro.',
        ),
      for (final item in visible) ...[
        _Surface(
          onTap: _busy
              ? null
              : () => _changeView(() {
                  _selectedActionId = item['id'] as String;
                  _error = null;
                }),
          child: Row(
            children: [
              const _IconTile(Icons.assignment_outlined),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _heading(
                      item['title'] as String,
                      _theme.textTheme.titleMedium,
                    ),
                    const SizedBox(height: 6),
                    Text('Prazo: ${_displayDate(item['dueDate'])}'),
                    const SizedBox(height: 7),
                    _StatusPill(item['state']),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
        const SizedBox(height: 9),
      ],
    ];
  }

  Widget _actionDetail(Map<String, dynamic> action) {
    final state = action['state'];
    final next = switch (state) {
      'PLANEJADA' => 'EM_ANDAMENTO',
      'EM_ANDAMENTO' => 'CONCLUIDA',
      _ => null,
    };
    final steps = ['PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA'];
    final currentStep = steps.indexOf(state);

    return _detailScreen(
      'Detalhe da ação',
      [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const _IconTile(Icons.task_alt),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _heading(
                    action['title'] as String,
                    _theme.textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 8),
                  _StatusPill(state),
                ],
              ),
            ),
          ],
        ),
        if (action['description'] case final String description
            when description.isNotEmpty) ...[
          const SizedBox(height: 18),
          Text(description),
        ],
        const SizedBox(height: 22),
        _heading('Prazo', _theme.textTheme.titleMedium),
        const SizedBox(height: 7),
        Row(
          children: [
            const Icon(Icons.calendar_today_outlined, size: 19),
            const SizedBox(width: 8),
            Text(_displayDate(action['dueDate'])),
          ],
        ),
        const SizedBox(height: 22),
        _Surface(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _heading('Andamento da ação', _theme.textTheme.titleMedium),
              const SizedBox(height: 8),
              if (currentStep < 0)
                Text(_statusLabel(state))
              else
                for (final (index, step) in steps.indexed)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 5),
                    child: Row(
                      children: [
                        Icon(
                          index < currentStep
                              ? Icons.check_circle_outline
                              : index == currentStep
                              ? Icons.radio_button_checked
                              : Icons.radio_button_unchecked,
                          size: 19,
                          color: index <= currentStep
                              ? _colors.primary
                              : _colors.onSurfaceVariant,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            index == currentStep
                                ? '${_statusLabel(step)} · atual'
                                : _statusLabel(step),
                          ),
                        ),
                      ],
                    ),
                  ),
            ],
          ),
        ),
      ],
      footer: next == null
          ? null
          : FilledButton(
              onPressed: _busy ? null : () => _updateAction(action, next),
              child: Text(
                _busy
                    ? 'Atualizando…'
                    : next == 'EM_ANDAMENTO'
                    ? 'Iniciar ação'
                    : 'Concluir ação',
              ),
            ),
    );
  }

  Widget _managerScreen(Map<String, dynamic> profile) => Scaffold(
    appBar: AppBar(title: const Text('Meu perfil')),
    body: SafeArea(
      child: _ScreenList(
        children: [
          if (_error != null) _errorMessage(),
          ..._profileContent(profile),
          const SizedBox(height: 16),
          _heading('Membros da organização', _theme.textTheme.titleMedium),
          const SizedBox(height: 8),
          for (final member in _members)
            ListTile(
              title: Text(member['name'] as String),
              subtitle: Text('${member['email']} · ${member['role']}'),
            ),
          OutlinedButton(
            onPressed: _busy ? null : () => _refresh(_loadMembers),
            child: const Text('Atualizar membros'),
          ),
        ],
      ),
    ),
  );

  List<Widget> _profileContent(Map<String, dynamic> profile) => [
    _Surface(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const _IconTile(Icons.person_outline),
          const SizedBox(height: 10),
          _heading(profile['name'] as String, _theme.textTheme.titleLarge),
          Text(profile['email'] as String),
          const Divider(height: 24),
          _heading(
            profile['organizationName'] as String,
            _theme.textTheme.titleMedium,
          ),
          Text(profile['role'] == 'GESTOR' ? 'Gestor' : 'Colaborador'),
        ],
      ),
    ),
    const SizedBox(height: 12),
    OutlinedButton.icon(
      onPressed: widget.onToggleTheme,
      icon: Icon(
        widget.isDark ? Icons.light_mode_outlined : Icons.dark_mode_outlined,
      ),
      label: Text(widget.isDark ? 'Ativar tema claro' : 'Ativar tema escuro'),
    ),
    const SizedBox(height: 8),
    FilledButton(onPressed: _busy ? null : _logout, child: const Text('Sair')),
  ];

  Widget _themeButton() => IconButton(
    tooltip: widget.isDark ? 'Ativar tema claro' : 'Ativar tema escuro',
    onPressed: widget.onToggleTheme,
    icon: Icon(
      widget.isDark ? Icons.light_mode_outlined : Icons.dark_mode_outlined,
    ),
  );

  Widget _errorMessage() => Semantics(
    liveRegion: true,
    child: DecoratedBox(
      decoration: BoxDecoration(
        color: _colors.errorContainer,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Text(_error!, style: TextStyle(color: _colors.onErrorContainer)),
      ),
    ),
  );
}

Widget _heading(String label, TextStyle? style) =>
    Semantics(header: true, child: Text(label, style: style));

class _ScreenList extends StatelessWidget {
  const _ScreenList({required this.children, super.key});
  final List<Widget> children;

  @override
  Widget build(BuildContext context) => Center(
    child: ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 560),
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 12, 18, 24),
        children: children,
      ),
    ),
  );
}

class _Surface extends StatelessWidget {
  const _Surface({
    required this.child,
    this.onTap,
    this.tinted = false,
    this.padding = const EdgeInsets.all(13),
  });
  final Widget child;
  final VoidCallback? onTap;
  final bool tinted;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      color: tinted ? theme.colorScheme.secondaryContainer : null,
      clipBehavior: Clip.antiAlias,
      child: onTap == null
          ? Padding(padding: padding, child: child)
          : InkWell(
              onTap: onTap,
              child: Padding(padding: padding, child: child),
            ),
    );
  }
}

class _IconTile extends StatelessWidget {
  const _IconTile(this.icon);
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: colors.secondaryContainer,
        borderRadius: BorderRadius.circular(10),
      ),
      child: SizedBox.square(
        dimension: 40,
        child: Icon(icon, size: 22, color: colors.onSecondaryContainer),
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard(this.icon, this.text);
  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => _Surface(
    padding: const EdgeInsets.all(22),
    child: Column(
      children: [
        _IconTile(icon),
        const SizedBox(height: 9),
        Text(text, textAlign: TextAlign.center),
      ],
    ),
  );
}

class _StatusPill extends StatelessWidget {
  const _StatusPill(this.status);
  final Object? status;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final (background, foreground) = switch (status) {
      'PENDENTE' => (const Color(0xfffff3d7), const Color(0xff745100)),
      'VALIDA' ||
      'EM_ANDAMENTO' ||
      'CONCLUIDA' => (colors.secondaryContainer, colors.onSecondaryContainer),
      _ => (colors.surfaceContainerHighest, colors.onSurface),
    };
    return DecoratedBox(
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
        child: Text(
          _statusLabel(status),
          style: TextStyle(
            fontSize: 12,
            color: foreground,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}

class _BandsPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final back = Path()
      ..moveTo(0, h * .35)
      ..cubicTo(w * .22, h * .22, w * .44, h * .87, w * .69, h * .52)
      ..quadraticBezierTo(w * .85, h * .18, w, h * .24)
      ..lineTo(w, h)
      ..lineTo(0, h)
      ..close();
    final middle = Path()
      ..moveTo(0, h * .64)
      ..cubicTo(w * .23, h * .1, w * .51, h * .75, w * .7, h * .64)
      ..quadraticBezierTo(w * .85, h * .65, w, h * .39)
      ..lineTo(w, h)
      ..lineTo(0, h)
      ..close();
    final front = Path()
      ..moveTo(0, h * .69)
      ..cubicTo(w * .24, h * .43, w * .4, h * .97, w * .67, h * .8)
      ..quadraticBezierTo(w * .86, h * .71, w, h * .63)
      ..lineTo(w, h)
      ..lineTo(0, h)
      ..close();
    canvas.drawPath(
      back,
      Paint()..color = AcordeColors.menta.withValues(alpha: .5),
    );
    canvas.drawPath(
      middle,
      Paint()..color = AcordeColors.verdeMedio.withValues(alpha: .62),
    );
    canvas.drawPath(front, Paint()..color = AcordeColors.verdeProfundo);
  }

  @override
  bool shouldRepaint(covariant _BandsPainter oldDelegate) => false;
}
