import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { api, errorMessage, jsonBody, readJson } from './api'
import type { Action, Activity, ApiError, Goal, Member, Membership, Request, Session } from './api'
import { ActivityEntryPage, FactorsPage, RecordsPage } from './CarbonPages'
import { DashboardPage } from './DashboardPage'
import { periodQuery } from './dashboard'
import type { Dashboard, PeriodFilter } from './dashboard'
import type { SourceFilter } from './format'
import { AssignedActionsPage, GoalsActionsPage } from './GoalsActionsPage'
import { Icon } from './ui'
import type { IconName } from './ui'
import './App.css'

const brand = '/branding/acorde-horizontal-principal.svg'
const inverseBrand = '/branding/acorde-horizontal-inverso.svg'
const navigation = [
  { id: 'dashboard', label: 'Dashboard', icon: 'dashboard', role: 'GESTOR' },
  { id: 'mobilidade', label: 'Registrar deslocamento', icon: 'car', role: 'COLABORADOR' },
  { id: 'historico', label: 'Histórico', icon: 'cloud', role: null },
  { id: 'acoes', label: 'Minhas ações', icon: 'actions', role: 'COLABORADOR' },
  { id: 'energia', label: 'Energia elétrica', icon: 'energy', role: 'GESTOR' },
  { id: 'fatores', label: 'Fatores de emissão', icon: 'layers', role: 'GESTOR' },
  { id: 'metas', label: 'Metas e ações', icon: 'goals', role: 'GESTOR' },
] as const satisfies readonly { id: string; label: string; icon: IconName; role: Session['profile']['role'] | null }[]

type Section = typeof navigation[number]['id']
type Notice = { kind: 'success' | 'error'; text: string }
type ThemeProps = { dark: boolean; toggleTheme: () => void }

export default function App() {
  const [session, setSession] = useState<Session | null>(null)
  const [expired, setExpired] = useState(false)
  const [dark, setDark] = useState(() => localStorage.getItem('acorde-theme') === 'dark')

  useEffect(() => {
    document.documentElement.dataset.theme = dark ? 'dark' : 'light'
    localStorage.setItem('acorde-theme', dark ? 'dark' : 'light')
  }, [dark])

  const closeSession = useCallback((expired: boolean) => {
    setExpired(expired)
    setSession(null)
  }, [])
  const toggleTheme = () => setDark((value) => !value)

  return session
    ? <Workspace key={session.accessToken} session={session} closeSession={closeSession} dark={dark} toggleTheme={toggleTheme}/>
    : <Login onLogin={setSession} expired={expired} dark={dark} toggleTheme={toggleTheme}/>
}

function Workspace({ session, closeSession, dark, toggleTheme }: ThemeProps & {
  session: Session
  closeSession: (expired: boolean) => void
}) {
  const profile = session.profile
  const isManager = profile.role === 'GESTOR'
  const [section, setSection] = useState<Section>(isManager ? 'dashboard' : 'mobilidade')
  const [pendingOnly, setPendingOnly] = useState(false)
  const [recordSource, setRecordSource] = useState<SourceFilter>('all')
  const [period, setPeriod] = useState<PeriodFilter>('all')
  const [notice, setNotice] = useState<Notice | null>(null)
  const content = useRef<HTMLDivElement>(null)
  const focusHeading = useRef(false)
  const endingSession = useRef(false)

  const request: Request = useCallback(async (path, options) => {
    const headers = new Headers(options?.headers)
    headers.set('Authorization', `Bearer ${session.accessToken}`)
    const response = await fetch(api + path, { ...options, headers })
    if (response.status === 401 && !endingSession.current) {
      endingSession.current = true
      closeSession(true)
    }
    return response
  }, [closeSession, session.accessToken])

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      endingSession.current = true
      closeSession(true)
    }, Math.max(0, Date.parse(session.expiresAt) - Date.now()))
    return () => window.clearTimeout(timeout)
  }, [closeSession, session.expiresAt])

  useEffect(() => {
    if (!focusHeading.current) return
    content.current?.querySelector('h1')?.focus()
    window.scrollTo({ top: 0 })
    focusHeading.current = false
  }, [section])

  const fetchDashboard = useCallback(async (signal: AbortSignal) => {
    return readJson<Dashboard>(await request('/dashboard' + periodQuery(period), { signal }))
  }, [period, request])
  const fetchRecords = useCallback(async (signal: AbortSignal) => {
    return readJson<Activity[]>(await request('/carbon/activities', { signal }))
  }, [request])
  const fetchPlans = useCallback(async (signal: AbortSignal) => {
    const [goals, actions, members] = await Promise.all([
      request('/goals', { signal }).then(readJson<Goal[]>),
      request('/actions', { signal }).then(readJson<Action[]>),
      request(`/organizations/${session.profile.organizationId}/members`, { signal }).then(readJson<Member[]>),
    ])
    return { goals, actions, members }
  }, [request, session.profile.organizationId])
  const fetchAssignedActions = useCallback(async (signal: AbortSignal) => {
    return readJson<Action[]>(await request('/actions', { signal }))
  }, [request])

  const dashboard = useResource(isManager ? fetchDashboard : null)
  const records = useResource(fetchRecords)
  const plans = useResource(isManager ? fetchPlans : null)
  const assignedActions = useResource(!isManager && section === 'acoes' ? fetchAssignedActions : null)

  function navigate(next: Section, pending = false, source: SourceFilter = 'all') {
    if (next !== section) focusHeading.current = true
    if (next === 'historico') records.reload()
    if (next === 'metas') plans.reload()
    if (next === 'acoes') assignedActions.reload()
    setSection(next)
    setPendingOnly(pending)
    setRecordSource(source)
    setNotice(null)
  }

  function logout() {
    endingSession.current = true
    closeSession(false)
    void request('/auth/logout', { method: 'POST' }).catch(() => {})
  }

  const initials = profile.name.trim().split(/\s+/).slice(0, 2).map((word) => word[0]).join('').toUpperCase()
  const organization = <><Icon name="building"/><span>{profile.organizationName}</span></>

  return <div className="app-shell">
    <a className="skip-link" href="#workspace">Ir para o conteúdo</a>
    <aside className="sidebar">
      <img className="brand-logo" src={inverseBrand} alt="Acorde"/>
      <nav aria-label="Navegação principal">
        {navigation.filter(({ role }) => role === null || role === profile.role).map(({ id, label, icon }) => <button key={id} className="nav-item" aria-current={section === id ? 'page' : undefined} onClick={() => navigate(id)}>
          <Icon name={icon}/><span>{label}</span>
        </button>)}
      </nav>
      <div className="sidebar-footer">
        <button type="button" className="nav-item" onClick={logout}><Icon name="exit"/><span>Sair da conta</span></button>
        <div className="org-badge">{organization}</div>
      </div>
    </aside>
    <main className="workspace" id="workspace" tabIndex={-1}>
      <header className="topbar">
        <div className="compact-org">{organization}</div>
        <div className="top-actions">
          <ThemeButton dark={dark} toggleTheme={toggleTheme}/>
          <details className="account">
            <summary className="avatar" aria-label="Opções da conta">{initials}</summary>
            <div className="account-panel panel">
              <strong>{profile.name}</strong><span>{isManager ? 'Gestor' : 'Colaborador'} · {profile.organizationName}</span>
              <button className="secondary-button" onClick={logout}><Icon name="exit"/>Sair</button>
            </div>
          </details>
        </div>
      </header>
      <div className="content" ref={content}>
        {notice && <div className={`notice ${notice.kind}`} role={notice.kind === 'error' ? 'alert' : 'status'}>
          <Icon name={notice.kind === 'success' ? 'check' : 'info'}/>{notice.text}
          <button className="icon-button" aria-label="Fechar aviso" onClick={() => setNotice(null)}>×</button>
        </div>}
        {isManager && section === 'dashboard' && <DashboardPage dashboard={dashboard.data} loading={dashboard.loading} error={dashboard.error} period={period} onPeriod={setPeriod} onRetry={dashboard.reload} goRecords={(pending) => navigate('historico', pending)} goEnergy={() => navigate('energia')}/>}
        {isManager && section === 'energia' && <ActivityEntryPage kind="energy" request={request} onCreated={(text) => {
          setNotice({ kind: 'success', text })
          records.reload()
          dashboard.reload()
        }} goRecords={() => navigate('historico', false, 'ENERGIA_ELETRICA')}/>}
        {!isManager && section === 'mobilidade' && <ActivityEntryPage kind="commute" request={request} onCreated={(text) => {
          navigate('historico')
          setNotice({ kind: 'success', text })
        }} goRecords={() => navigate('historico')}/>}
        {section === 'historico' && <RecordsPage
          request={request}
          records={records.data ?? []}
          loading={records.loading}
          error={records.error}
          reload={records.reload}
          onReprocessed={isManager ? dashboard.reload : undefined}
          source={isManager ? recordSource : 'DESLOCAMENTO'}
          onSource={setRecordSource}
          pendingOnly={pendingOnly}
          onPendingOnly={setPendingOnly}
          registerCommute={isManager ? undefined : () => navigate('mobilidade')}
        />}
        {!isManager && section === 'acoes' && <AssignedActionsPage
          request={request}
          actions={assignedActions.data}
          loading={assignedActions.loading}
          error={assignedActions.error}
          onRefresh={assignedActions.reload}
        />}
        {isManager && section === 'fatores' && <FactorsPage request={request}/>}
        {isManager && section === 'metas' && <GoalsActionsPage request={request} data={plans.data} loading={plans.loading} error={plans.error} onRefresh={plans.reload} onSuccess={(text) => {
          setNotice({ kind: 'success', text })
          plans.reload()
        }}/>}
      </div>
    </main>
  </div>
}

function useResource<T>(load: ((signal: AbortSignal) => Promise<T>) | null) {
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState<{ load: NonNullable<typeof load>; revision: number; data: T | null; error: string | null } | null>(null)

  useEffect(() => {
    if (!load) return
    const controller = new AbortController()
    const complete = (data: T | null, error: string | null) => {
      if (!controller.signal.aborted) setResult({ load, revision, data, error })
    }
    void load(controller.signal).then((data) => complete(data, null), (error: unknown) => complete(null, errorMessage(error)))
    return () => controller.abort()
  }, [load, revision])

  const current = result?.load === load ? result : null
  return {
    data: current?.data ?? null,
    error: current?.error ?? null,
    loading: load !== null && current?.revision !== revision,
    reload: () => setRevision((value) => value + 1),
  }
}

function ThemeButton({ dark, toggleTheme }: ThemeProps) {
  return <button className="icon-button theme-button" onClick={toggleTheme} type="button" aria-label={dark ? 'Ativar modo claro' : 'Ativar modo escuro'}>
    <Icon name={dark ? 'sun' : 'moon'}/>
  </button>
}

function Login({ onLogin, expired, dark, toggleTheme }: ThemeProps & { onLogin: (session: Session) => void; expired: boolean }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [organization, setOrganization] = useState('')
  const [memberships, setMemberships] = useState<Membership[]>([])
  const [error, setError] = useState(expired ? 'Sua sessão expirou. Entre novamente.' : '')
  const [busy, setBusy] = useState(false)
  const organizationInput = useRef<HTMLSelectElement>(null)

  useEffect(() => {
    if (memberships.length) organizationInput.current?.focus()
  }, [memberships])

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const response = await fetch(api + '/auth/login', jsonBody('POST', {
        email: email.trim(),
        password,
        ...(organization ? { organizationId: organization } : {}),
      }))
      if (response.status === 409) {
        const data = await response.json() as ApiError
        setMemberships(data.memberships ?? [])
        setError(data.message ?? 'Selecione uma organização.')
        return
      }
      onLogin(await readJson<Session>(response))
    } catch (reason) {
      setError(errorMessage(reason))
    } finally {
      setBusy(false)
    }
  }

  return <main className="login-shell">
    <section className="login-visual" aria-label="Acorde">
      <div className="login-brand">
        <img src={inverseBrand} alt="Acorde"/>
        <p>Monitoramento e redução da pegada de carbono operacional.</p>
      </div>
    </section>
    <section className="login-form-side">
      <div className="login-theme"><ThemeButton dark={dark} toggleTheme={toggleTheme}/></div>
      <form className="panel login-form" onSubmit={(event) => void submit(event)}>
        <img className="compact-brand" src={dark ? inverseBrand : brand} alt="Acorde"/>
        <div><h1>Acesse sua conta</h1><p>Entre para acessar o Acorde pela sua organização.</p></div>
        <fieldset disabled={busy}>
          <label htmlFor="login-email">E-mail</label>
          <input id="login-email" autoComplete="username" type="email" required value={email} onChange={(event) => {
            setEmail(event.target.value)
            setMemberships([])
            setOrganization('')
          }} placeholder="seu.email@organizacao.com"/>
          <label htmlFor="login-password">Senha</label>
          <div className="password-field">
            <input id="login-password" autoComplete="current-password" type={showPassword ? 'text' : 'password'} required value={password} onChange={(event) => setPassword(event.target.value)}/>
            <button type="button" className="icon-button" aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'} onClick={() => setShowPassword(!showPassword)}>
              <Icon name={showPassword ? 'eyeOff' : 'eye'}/>
            </button>
          </div>
          {memberships.length > 0 && <>
            <label htmlFor="login-org">Organização</label>
            <select id="login-org" ref={organizationInput} value={organization} onChange={(event) => setOrganization(event.target.value)} required>
              <option value="">Selecione uma organização</option>
              {memberships.map((item) => <option value={item.organizationId} key={item.organizationId}>{item.organizationName} · {item.role}</option>)}
            </select>
          </>}
        </fieldset>
        {error && <div className="form-error" role="alert">{error}</div>}
        <button className="primary-button" disabled={busy}>{busy ? 'Entrando…' : 'Entrar'}</button>
      </form>
    </section>
  </main>
}
