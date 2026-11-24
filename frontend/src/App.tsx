import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import './App.css'

type Membership = { organizationId: string; organizationName: string; memberId: string; role: string }
type Profile = { userId: string; name: string; email: string; organizationId: string; organizationName: string; memberId: string; role: 'GESTOR' | 'COLABORADOR' }
type Session = { accessToken: string; expiresAt: string; profile: Profile }
type Activity = { id: string; category: string; periodStart: string; periodEnd: string; quantity: number; unit: string; state: string; justification: string | null; revision: number | null; emissionsKgCo2e: number | null; current: boolean }
type Dashboard = { from: string | null; to: string | null; activityCount: number; pendingCount: number; emissionsKgCo2e: number | null; sources: { category: string; activityCount: number; validCount: number; pendingCount: number; emissionsKgCo2e: number | null }[]; periods: { periodStart: string; periodEnd: string; activityCount: number; validCount: number; pendingCount: number; emissionsKgCo2e: number | null }[]; coverageNote: string }
type Goal = { id: string; category: string; basePeriodStart: string; basePeriodEnd: string; baseValueKgCo2e: number; targetPercent: number; dueDate: string; state: string }
type Action = { id: string; goalId: string; responsibleMemberId: string; title: string; description: string | null; dueDate: string; state: string }
type Member = { id: string; name: string; email: string; role: string }
type ApiError = { message?: string; memberships?: Membership[] }
type Section = 'inicio' | 'energia' | 'registros' | 'metas-acoes'
const api = import.meta.env.VITE_API_BASE_URL ?? '/api'
const brand = '/branding/acorde-horizontal-principal.svg'

async function readJson<T>(response: Response): Promise<T> {
  const body = await response.json() as T & ApiError
  if (!response.ok) throw new Error(body.message ?? `Falha na API (${response.status})`)
  return body
}

function App() {
  const [session, setSession] = useState<Session | null>(null)
  const [section, setSection] = useState<Section>('inicio')
  const [activities, setActivities] = useState<Activity[]>([])
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [goals, setGoals] = useState<Goal[]>([])
  const [actions, setActions] = useState<Action[]>([])
  const [members, setMembers] = useState<Member[]>([])
  const [loading, setLoading] = useState(false)
  const [notice, setNotice] = useState<{ kind: 'error' | 'success'; text: string } | null>(null)
  const [dark, setDark] = useState(() => localStorage.getItem('acorde-theme') === 'dark')

  useEffect(() => {
    document.documentElement.dataset.theme = dark ? 'dark' : 'light'
    localStorage.setItem('acorde-theme', dark ? 'dark' : 'light')
  }, [dark])

  const logout = useCallback(async (message?: string) => {
    if (session) {
      try { await fetch(`${api}/auth/logout`, { method: 'POST', headers: { Authorization: `Bearer ${session.accessToken}` } }) } catch { /* remove token locally */ }
    }
    setSession(null); setActivities([]); setNotice(message ? { kind: 'error', text: message } : null)
  }, [session])

  const loadActivities = useCallback(async (active = session) => {
    if (!active) return
    setLoading(true)
    try {
      const response = await fetch(`${api}/carbon/activities`, { headers: { Authorization: `Bearer ${active.accessToken}` } })
      if (response.status === 401) { await logout('Sua sessão expirou. Entre novamente.'); return }
      setActivities(await readJson<Activity[]>(response))
    } catch (error) { setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Não foi possível carregar os registros.' }) }
    finally { setLoading(false) }
  }, [session, logout])

  const loadDashboard = useCallback(async (active = session) => {
    if (!active) return
    try {
      const response = await fetch(`${api}/dashboard`, { headers: { Authorization: `Bearer ${active.accessToken}` } })
      if (response.status === 401) { await logout('Sua sessão expirou. Entre novamente.'); return }
      setDashboard(await readJson<Dashboard>(response))
    } catch (error) { setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Não foi possível carregar o dashboard.' }) }
  }, [session, logout])

  const loadGoalsAndActions = useCallback(async (active = session) => {
    if (!active) return
    try {
      const headers = { Authorization: `Bearer ${active.accessToken}` }
      const [goalResponse, actionResponse, memberResponse] = await Promise.all([
        fetch(`${api}/goals`, { headers }), fetch(`${api}/actions`, { headers }),
        fetch(`${api}/organizations/${active.profile.organizationId}/members`, { headers }),
      ])
      if ([goalResponse, actionResponse, memberResponse].some((response) => response.status === 401)) {
        await logout('Sua sessão expirou. Entre novamente.'); return
      }
      const [goalList, actionList, memberList] = await Promise.all([
        readJson<Goal[]>(goalResponse), readJson<Action[]>(actionResponse), readJson<Member[]>(memberResponse),
      ])
      setGoals(goalList); setActions(actionList); setMembers(memberList)
    } catch (error) {
      setNotice({ kind: 'error', text: error instanceof Error ? error.message : 'Não foi possível carregar metas e ações.' })
    }
  }, [session, logout])

  useEffect(() => {
    if (!session) return
    const delay = Math.max(0, Date.parse(session.expiresAt) - Date.now())
    const timer = window.setTimeout(() => void logout('Sua sessão expirou. Entre novamente.'), delay)
    const initialLoad = window.setTimeout(() => { void loadActivities(session); void loadDashboard(session); void loadGoalsAndActions(session) }, 0)
    return () => { window.clearTimeout(timer); window.clearTimeout(initialLoad) }
  }, [session?.accessToken, session?.expiresAt, loadActivities, loadDashboard, loadGoalsAndActions, logout, session])

  if (!session) return <Login onLogin={(value) => { setSession(value); setSection('inicio') }} notice={notice} />

  const records = activities.filter((item) => item.category === 'ENERGIA_ELETRICA')
  const pending = dashboard?.pendingCount ?? 0
  const total = dashboard?.emissionsKgCo2e ?? null

  return <div className="app-shell">
    <aside className="sidebar">
      <img className="brand-logo" src={brand} alt="Acorde — Gestão em sintonia" />
      <p className="side-label">ESPAÇO DE TRABALHO</p>
      <nav aria-label="Navegação principal">
        <NavButton icon="⌂" active={section === 'inicio'} onClick={() => setSection('inicio')}>Visão geral</NavButton>
        <NavButton icon="ϟ" active={section === 'energia'} onClick={() => setSection('energia')}>Energia elétrica</NavButton>
        <NavButton icon="▤" active={section === 'registros'} onClick={() => setSection('registros')}>Registros</NavButton>
        <NavButton icon="◎" active={section === 'metas-acoes'} onClick={() => setSection('metas-acoes')}>Metas e ações</NavButton>
      </nav>
      <div className="sidebar-bottom"><div className="org-badge"><span className="org-dot">{session.profile.organizationName.slice(0, 1).toUpperCase()}</span><span><b>{session.profile.organizationName}</b><small>Plano de trabalho</small></span></div></div>
    </aside>
    <main className="workspace">
      <header className="topbar"><div className="breadcrumbs">Acorde <span>/</span> {sectionLabel(section)}</div><div className="top-actions"><button className="icon-button" onClick={() => setDark(!dark)} aria-label={dark ? 'Ativar modo claro' : 'Ativar modo escuro'}>{dark ? '☼' : '◐'}</button><div className="user-chip"><span className="avatar">{session.profile.name.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</span><span><b>{session.profile.name}</b><small>Gestor</small></span></div><button className="text-button" onClick={() => void logout()}>Sair</button></div></header>
      <div className="content">
        {notice ? <div className={`notice ${notice.kind}`} role="status"><span>{notice.kind === 'success' ? '✓' : '!'}</span>{notice.text}<button aria-label="Fechar aviso" onClick={() => setNotice(null)}>×</button></div> : null}
        {section === 'energia' ? <EnergyPage session={session} onCreated={async (message) => { setNotice({ kind: 'success', text: message }); await Promise.all([loadActivities(), loadDashboard()]) }} onError={(message) => setNotice({ kind: 'error', text: message })} /> : null}
        {section === 'registros' ? <RecordsPage records={records} loading={loading} reload={() => void loadActivities()} /> : null}
        {section === 'metas-acoes' ? <GoalsActionsPage session={session} goals={goals} actions={actions} members={members} onRefresh={() => void loadGoalsAndActions()} onError={(message) => setNotice({ kind: 'error', text: message })} onSuccess={(message) => { setNotice({ kind: 'success', text: message }); void loadGoalsAndActions() }} /> : null}
        {section === 'inicio' ? <Overview session={session} dashboard={dashboard} total={total} pending={pending} loading={loading || dashboard === null} goEnergy={() => setSection('energia')} goRecords={() => setSection('registros')} /> : null}
      </div>
    </main>
  </div>
}

function sectionLabel(section: Section) { return section === 'inicio' ? 'Visão geral' : section === 'energia' ? 'Energia elétrica' : section === 'registros' ? 'Registros' : 'Metas e ações' }

function NavButton({ icon, active, disabled, onClick, children }: { icon: string; active?: boolean; disabled?: boolean; onClick?: () => void; children: ReactNode }) {
  return <button className={`nav-item ${active ? 'active' : ''}`} disabled={disabled} onClick={onClick}><span className="nav-icon" aria-hidden="true">{icon}</span>{children}</button>
}

function Login({ onLogin, notice }: { onLogin: (session: Session) => void; notice: { kind: string; text: string } | null }) {
  const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [org, setOrg] = useState(''); const [memberships, setMemberships] = useState<Membership[]>([]); const [error, setError] = useState<string | null>(notice?.text ?? null); const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(null)
    try {
      const response = await fetch(`${api}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password, ...(org ? { organizationId: org } : {}) }) })
      const result = await response.json() as Session & ApiError
      if (response.status === 409 && result.memberships) { setMemberships(result.memberships); setError(result.message ?? 'Selecione uma organização.'); return }
      if (!response.ok) throw new Error(result.message ?? 'Não foi possível entrar.')
      if (result.profile.role !== 'GESTOR') throw new Error('A área Web está disponível para o perfil gestor.')
      onLogin(result)
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Falha de conexão com a API.') }
    finally { setBusy(false) }
  }
  return <main className="login-shell"><section className="login-visual"><img src={brand} alt="Acorde — Gestão em sintonia"/><div><span className="eyebrow">GESTÃO EM SINTONIA</span><h1>Clareza para<br/>agir com propósito.</h1><p>Acompanhe os registros operacionais de carbono da sua organização.</p></div><div className="visual-foot">Acorde 2.0 <span>·</span> Carbono operacional</div></section><section className="login-form-side"><form className="login-form" onSubmit={(event) => void submit(event)}><span className="eyebrow">BEM-VINDA(O) DE VOLTA</span><h2>Acesse sua conta</h2><p className="muted">Entre para gerenciar os dados da sua organização.</p><label htmlFor="login-email">E-mail</label><input id="login-email" autoComplete="username" type="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="voce@organizacao.com"/><label htmlFor="login-password">Senha</label><input id="login-password" autoComplete="current-password" type="password" required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Sua senha"/>{memberships.length > 0 ? <><label htmlFor="login-org">Organização</label><select id="login-org" value={org} onChange={(event) => setOrg(event.target.value)} required><option value="">Selecione uma organização</option>{memberships.map((item) => <option value={item.organizationId} key={item.organizationId}>{item.organizationName} · {item.role}</option>)}</select></> : null}{error ? <div className="form-error" role="alert">{error}</div> : null}<button className="primary-button" disabled={busy}>{busy ? 'Entrando…' : 'Entrar na plataforma'}</button><small className="login-note">Acesso seguro à sua organização</small></form></section></main>
}

function Overview({ session, dashboard, total, pending, loading, goEnergy, goRecords }: { session: Session; dashboard: Dashboard | null; total: number | null; pending: number; loading: boolean; goEnergy: () => void; goRecords: () => void }) {
  return <><div className="page-heading"><div><span className="eyebrow">RESUMO DA ORGANIZAÇÃO</span><h1>Olá, {session.profile.name.split(' ')[0]}</h1><p>Acompanhe os dados de carbono registrados pela sua organização.</p></div><button className="primary-button compact" onClick={goEnergy}>＋ Registrar energia</button></div><div className="metric-grid"><Metric label="Atividades monitoradas" value={loading ? '—' : (dashboard?.activityCount ?? 0).toLocaleString('pt-BR')} detail="energia e deslocamento" icon="ϟ"/><Metric label="Emissões calculadas" value={loading ? '—' : total === null ? 'Sem cálculo válido' : total.toLocaleString('pt-BR', { maximumFractionDigits: 2 })} unit={total === null ? undefined : 'kg CO₂e'} detail="somente cálculos válidos vigentes" icon="◌"/><Metric label="Aguardando fator" value={loading ? '—' : String(pending)} detail="sem estimativa até haver fator compatível" icon="◷"/></div><div className="section-heading"><div><h2>Fontes monitoradas</h2><p>Valores agregados a partir dos registros vigentes.</p></div></div><div className="metric-grid">{dashboard?.sources.map((source) => <Metric key={source.category} label={source.category === 'ENERGIA_ELETRICA' ? 'Energia elétrica' : 'Deslocamento casa–trabalho'} value={source.activityCount === 0 ? 'Sem dados' : source.emissionsKgCo2e === null ? 'Sem cálculo válido' : Number(source.emissionsKgCo2e).toLocaleString('pt-BR', { maximumFractionDigits: 2 })} unit={source.emissionsKgCo2e === null ? undefined : 'kg CO₂e'} detail={`${source.validCount} cálculos válidos · ${source.pendingCount} pendentes`} icon={source.category === 'ENERGIA_ELETRICA' ? 'ϟ' : '↗'}/>)}</div><div className="section-heading"><div><h2>Histórico por período informado</h2><p>Períodos mantidos conforme os intervalos registrados.</p></div><button className="link-button" onClick={goRecords}>Ver registros <span>→</span></button></div>{dashboard?.periods.length ? <div className="table-wrap"><table><thead><tr><th>Período</th><th>Atividades</th><th>Válidas</th><th>Pendentes</th><th>Emissões válidas</th></tr></thead><tbody>{dashboard.periods.map((period) => <tr key={`${period.periodStart}-${period.periodEnd}`}><td>{formatDate(period.periodStart)} — {formatDate(period.periodEnd)}</td><td>{period.activityCount}</td><td>{period.validCount}</td><td>{period.pendingCount}</td><td>{period.emissionsKgCo2e === null ? '— sem cálculo válido' : `${Number(period.emissionsKgCo2e).toLocaleString('pt-BR', { maximumFractionDigits: 2 })} kg CO₂e`}</td></tr>)}</tbody></table></div> : <div className="table-state empty">Sem períodos para apresentar.</div>}<div className="info-banner"><span className="info-icon">i</span><p>{dashboard?.coverageNote ?? 'Carregando dados parciais da API.'} Comparações de redução não são exibidas enquanto cobertura e metodologia comparáveis não puderem ser verificadas.</p></div></>
}

function Metric({ label, value, unit, detail, icon }: { label: string; value: string; unit?: string; detail: string; icon: string }) { return <article className="metric-card"><span className="metric-icon">{icon}</span><span className="metric-label">{label}</span><strong>{value}{unit ? <small> {unit}</small> : null}</strong><p>{detail}</p></article> }

function RecordsPage({ records, loading, reload }: { records: Activity[]; loading: boolean; reload: () => void }) { return <><div className="page-heading"><div><span className="eyebrow">DADOS DA ORGANIZAÇÃO</span><h1>Registros de energia</h1><p>Histórico autorizado de consumo e respectivos estados de cálculo.</p></div><button className="secondary-button compact" onClick={reload} disabled={loading}>↻ Atualizar</button></div><RecordsTable records={records} loading={loading} full/><div className="info-banner"><span className="info-icon">i</span><p>Emissões pendentes permanecem sem resultado até existir fator compatível com categoria, região, unidade e período.</p></div></> }

function RecordsTable({ records, loading, full = false }: { records: Activity[]; loading: boolean; full?: boolean }) {
  if (loading && records.length === 0) return <div className="table-state" role="status"><span className="spinner"/>Carregando registros da API…</div>
  if (!loading && records.length === 0) return <div className="table-state empty"><span className="empty-icon">ϟ</span><b>Nenhum registro de energia ainda</b><span>Quando o consumo for cadastrado, ele aparecerá aqui.</span></div>
  return <div className="table-wrap"><table><thead><tr><th>Período</th><th>Consumo</th><th>Estado</th><th>Emissões</th><th>Detalhe</th></tr></thead><tbody>{records.map((record) => <tr key={record.id}><td><b>{formatDate(record.periodStart)} — {formatDate(record.periodEnd)}</b></td><td>{Number(record.quantity).toLocaleString('pt-BR')} {record.unit}</td><td><span className={`status ${record.state === 'VALIDA' ? 'valid' : 'pending'}`}>{record.state === 'VALIDA' ? 'Calculado' : 'Pendente'}</span></td><td>{record.emissionsKgCo2e === null ? '—' : `${Number(record.emissionsKgCo2e).toLocaleString('pt-BR')} kg CO₂e`}</td><td><span className="row-note">{record.justification ?? (record.revision ? `Revisão ${record.revision}` : '—')}</span></td></tr>)}</tbody></table>{full ? <p className="table-foot">Exibindo {records.length} registros retornados pela API.</p> : null}</div>
}
function formatDate(date: string) { return new Date(`${date}T12:00:00`).toLocaleDateString('pt-BR') }

function EnergyPage({ session, onCreated, onError }: { session: Session; onCreated: (message: string) => void; onError: (message: string) => void }) {
  const [start, setStart] = useState(''); const [end, setEnd] = useState(''); const [region, setRegion] = useState(''); const [consumption, setConsumption] = useState(''); const [busy, setBusy] = useState(false)
  const idempotencyKey = useRef<string | null>(null)
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true)
    try {
      idempotencyKey.current ??= crypto.randomUUID()
      const response = await fetch(`${api}/carbon/activities/energy`, { method: 'POST', headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ idempotencyKey: idempotencyKey.current, periodStart: start, periodEnd: end, region: region.trim(), consumptionKwh: consumption }) })
      const record = await readJson<Activity>(response)
      onCreated(record.state === 'PENDENTE' ? 'Registro salvo. O cálculo está pendente porque não há fator compatível cadastrado.' : 'Consumo registrado e salvo.')
      idempotencyKey.current = null
      setStart(''); setEnd(''); setRegion(''); setConsumption('')
    } catch (error) { onError(error instanceof Error ? error.message : 'Não foi possível salvar o consumo.') }
    finally { setBusy(false) }
  }
  return <><div className="page-heading"><div><span className="eyebrow">ENTRADA DE DADOS</span><h1>Consumo de energia</h1><p>Registre o consumo elétrico da organização para o período informado.</p></div></div><div className="form-layout"><form className="data-form" onSubmit={(event) => void submit(event)}><div className="form-title"><span className="metric-icon">ϟ</span><div><h2>Novo registro</h2><p>Campos com * são obrigatórios.</p></div></div><div className="form-grid"><div className="field"><label htmlFor="consumption">Consumo (kWh) *</label><input id="consumption" type="number" min="0.000001" step="any" required value={consumption} onChange={(event) => { idempotencyKey.current = null; setConsumption(event.target.value) }} placeholder="Ex.: 1250,5"/><small>Informe o valor indicado na medição ou fatura.</small></div><div className="field"><label htmlFor="region">Região/rede elétrica *</label><input id="region" required maxLength={160} value={region} onChange={(event) => { idempotencyKey.current = null; setRegion(event.target.value) }} placeholder="Ex.: BR-SP"/><small>Usada para buscar fator compatível.</small></div><div className="field"><label htmlFor="period-start">Início do período *</label><input id="period-start" type="date" required value={start} onChange={(event) => { idempotencyKey.current = null; setStart(event.target.value) }} max={end || undefined}/></div><div className="field"><label htmlFor="period-end">Fim do período *</label><input id="period-end" type="date" required value={end} onChange={(event) => { idempotencyKey.current = null; setEnd(event.target.value) }} min={start || undefined}/></div></div><div className="form-actions"><span>Os dados são enviados com sua sessão autenticada.</span><button className="primary-button" disabled={busy}>{busy ? 'Salvando…' : 'Salvar registro'}</button></div></form><aside className="calc-card"><span className="calc-symbol">◌</span><h3>Cálculo rastreável</h3><p>A API procura um fator aprovado compatível com unidade, região, categoria e período.</p><div className="calc-equation">E = Q × FE</div><p className="calc-foot">Sem fator compatível, o registro permanece pendente. Nenhuma emissão zero será criada.</p></aside></div></>
}

function GoalsActionsPage({ session, goals, actions, members, onRefresh, onError, onSuccess }: {
  session: Session; goals: Goal[]; actions: Action[]; members: Member[];
  onRefresh: () => void; onError: (message: string) => void; onSuccess: (message: string) => void
}) {
  const [category, setCategory] = useState('ENERGIA_ELETRICA')
  const [baseStart, setBaseStart] = useState(''); const [baseEnd, setBaseEnd] = useState('')
  const [baseValue, setBaseValue] = useState(''); const [target, setTarget] = useState(''); const [goalDue, setGoalDue] = useState('')
  const [goalId, setGoalId] = useState(''); const [memberId, setMemberId] = useState('')
  const [title, setTitle] = useState(''); const [description, setDescription] = useState(''); const [actionDue, setActionDue] = useState('')
  const [busy, setBusy] = useState(false)

  async function submitGoal(event: FormEvent) {
    event.preventDefault(); setBusy(true)
    try {
      const response = await fetch(`${api}/goals`, { method: 'POST', headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ category, basePeriodStart: baseStart, basePeriodEnd: baseEnd, baseValueKgCo2e: baseValue, targetPercent: target, dueDate: goalDue }) })
      await readJson<Goal>(response); setBaseStart(''); setBaseEnd(''); setBaseValue(''); setTarget(''); setGoalDue(''); onSuccess('Meta criada.')
    } catch (error) { onError(error instanceof Error ? error.message : 'Não foi possível criar a meta.') }
    finally { setBusy(false) }
  }

  async function submitAction(event: FormEvent) {
    event.preventDefault(); setBusy(true)
    try {
      const response = await fetch(`${api}/actions`, { method: 'POST', headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ goalId, responsibleMemberId: memberId, title, description: description || null, dueDate: actionDue }) })
      await readJson<Action>(response); setTitle(''); setDescription(''); setActionDue(''); onSuccess('Ação criada e atribuída.')
    } catch (error) { onError(error instanceof Error ? error.message : 'Não foi possível criar a ação.') }
    finally { setBusy(false) }
  }

  async function updateState(actionId: string, state: string) {
    try {
      const response = await fetch(`${api}/actions/${actionId}/state`, { method: 'PATCH', headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ state }) })
      await readJson<Action>(response); onSuccess('Estado da ação atualizado.')
    } catch (error) { onError(error instanceof Error ? error.message : 'Não foi possível atualizar a ação.') }
  }

  async function updateGoalState(goalId: string, state: string) {
    try {
      const response = await fetch(`${api}/goals/${goalId}/state`, { method: 'PATCH', headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ state }) })
      await readJson<Goal>(response); onSuccess('Estado da meta atualizado.')
    } catch (error) { onError(error instanceof Error ? error.message : 'Não foi possível atualizar a meta.') }
  }

  return <><div className="page-heading"><div><span className="eyebrow">PLANEJAMENTO DE REDUÇÃO</span><h1>Metas e ações</h1><p>Acompanhe objetivos definidos e o andamento operacional das ações.</p></div><button className="secondary-button compact" onClick={onRefresh}>↻ Atualizar</button></div>
    <div className="info-banner"><span className="info-icon">i</span><p>O progresso operacional das ações não significa redução medida. O progresso ambiental e comparações permanecem indisponíveis sem períodos comparáveis.</p></div>
    <div className="form-layout"><form className="data-form" onSubmit={(event) => void submitGoal(event)}><div className="form-title"><span className="metric-icon">◎</span><div><h2>Nova meta de redução</h2><p>O valor-base pode ser zero; nesse caso não se calcula percentual realizado.</p></div></div><div className="form-grid"><div className="field"><label htmlFor="goal-category">Fonte *</label><select id="goal-category" value={category} onChange={(event) => setCategory(event.target.value)}><option value="ENERGIA_ELETRICA">Energia elétrica</option><option value="DESLOCAMENTO">Deslocamento casa–trabalho</option></select></div><div className="field"><label htmlFor="goal-base-value">Base (kg CO₂e) *</label><input id="goal-base-value" type="number" min="0" step="any" required value={baseValue} onChange={(event) => setBaseValue(event.target.value)}/></div><div className="field"><label htmlFor="goal-base-start">Início do período-base *</label><input id="goal-base-start" type="date" required value={baseStart} onChange={(event) => setBaseStart(event.target.value)} max={baseEnd || undefined}/></div><div className="field"><label htmlFor="goal-base-end">Fim do período-base *</label><input id="goal-base-end" type="date" required value={baseEnd} onChange={(event) => setBaseEnd(event.target.value)} min={baseStart || undefined}/></div><div className="field"><label htmlFor="goal-target">Redução-alvo (%) *</label><input id="goal-target" type="number" min="0.01" max="100" step="any" required value={target} onChange={(event) => setTarget(event.target.value)}/></div><div className="field"><label htmlFor="goal-due">Prazo *</label><input id="goal-due" type="date" required value={goalDue} onChange={(event) => setGoalDue(event.target.value)}/></div></div><div className="form-actions"><span>Organização definida pela sessão.</span><button className="primary-button" disabled={busy}>{busy ? 'Salvando…' : 'Criar meta'}</button></div></form>
      <form className="data-form" onSubmit={(event) => void submitAction(event)}><div className="form-title"><span className="metric-icon">✓</span><div><h2>Nova ação</h2><p>Vincule a uma meta e atribua a um membro.</p></div></div><div className="form-grid"><div className="field"><label htmlFor="action-goal">Meta *</label><select id="action-goal" required value={goalId} onChange={(event) => setGoalId(event.target.value)}><option value="">Selecione</option>{goals.map((goal) => <option key={goal.id} value={goal.id}>{goal.category === 'ENERGIA_ELETRICA' ? 'Energia elétrica' : 'Deslocamento'} · {goal.targetPercent}%</option>)}</select></div><div className="field"><label htmlFor="action-member">Responsável *</label><select id="action-member" required value={memberId} onChange={(event) => setMemberId(event.target.value)}><option value="">Selecione</option>{members.map((member) => <option key={member.id} value={member.id}>{member.name} · {member.role}</option>)}</select></div><div className="field"><label htmlFor="action-title">Título *</label><input id="action-title" required maxLength={160} value={title} onChange={(event) => setTitle(event.target.value)}/></div><div className="field"><label htmlFor="action-due">Prazo *</label><input id="action-due" type="date" required value={actionDue} onChange={(event) => setActionDue(event.target.value)}/></div><div className="field"><label htmlFor="action-description">Descrição</label><input id="action-description" maxLength={4000} value={description} onChange={(event) => setDescription(event.target.value)}/></div></div><div className="form-actions"><span>Somente membro da organização.</span><button className="primary-button" disabled={busy || goals.length === 0 || members.length === 0}>{busy ? 'Salvando…' : 'Criar ação'}</button></div></form></div>
    <div className="section-heading"><div><h2>Metas cadastradas</h2><p>Estados operacionais, sem estimativa de redução realizada.</p></div></div><div className="table-wrap"><table><thead><tr><th>Fonte</th><th>Período-base</th><th>Valor-base</th><th>Alvo</th><th>Prazo</th><th>Estado</th></tr></thead><tbody>{goals.map((goal) => <tr key={goal.id}><td>{goal.category === 'ENERGIA_ELETRICA' ? 'Energia elétrica' : 'Deslocamento'}</td><td>{formatDate(goal.basePeriodStart)} — {formatDate(goal.basePeriodEnd)}</td><td>{Number(goal.baseValueKgCo2e).toLocaleString('pt-BR')} kg CO₂e</td><td>{goal.targetPercent}%</td><td>{formatDate(goal.dueDate)}</td><td><select aria-label={`Estado da meta ${goal.category}`} value={goal.state} onChange={(event) => void updateGoalState(goal.id, event.target.value)}><option value="ATIVA">Ativa</option><option value="CONCLUIDA">Concluída</option><option value="CANCELADA">Cancelada</option></select></td></tr>)}</tbody></table>{goals.length === 0 ? <p className="table-foot">Nenhuma meta cadastrada.</p> : null}</div>
    <div className="section-heading"><div><h2>Ações e andamento operacional</h2><p>Conclusão de ação não altera os cálculos de emissão.</p></div></div><div className="table-wrap"><table><thead><tr><th>Ação</th><th>Meta</th><th>Prazo</th><th>Estado</th><th>Atualizar</th></tr></thead><tbody>{actions.map((action) => <tr key={action.id}><td><b>{action.title}</b><span className="row-note">{action.description}</span></td><td>{goals.find((goal) => goal.id === action.goalId)?.category ?? '—'}</td><td>{formatDate(action.dueDate)}</td><td>{action.state}</td><td><select aria-label={`Estado de ${action.title}`} value={action.state} onChange={(event) => void updateState(action.id, event.target.value)}><option value="PLANEJADA">Planejada</option><option value="EM_ANDAMENTO">Em andamento</option><option value="CONCLUIDA">Concluída</option><option value="CANCELADA">Cancelada</option></select></td></tr>)}</tbody></table>{actions.length === 0 ? <p className="table-foot">Nenhuma ação cadastrada.</p> : null}</div>
  </>
}

export default App
