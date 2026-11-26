import { useEffect, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { errorMessage, jsonBody, readJson } from './api'
import type { Action, Goal, Member, Request } from './api'
import { Empty, Icon, Info, PageHeading, Status } from './ui'
import { formatDate, formatNumber, sourceName, sources, statusLabel } from './format'

const goalStates = ['ATIVA', 'CONCLUIDA', 'CANCELADA']
const actionStates = ['PLANEJADA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA']
type View = 'goals' | 'actions'
type Plans = { goals: Goal[]; actions: Action[]; members: Member[] }

export function GoalsActionsPage({ request, data, loading, error, onRefresh, onSuccess }: {
  request: Request
  data: Plans | null
  loading: boolean
  error: string | null
  onRefresh: () => void
  onSuccess: (message: string) => void
}) {
  const [view, setView] = useState<View>('goals')
  const [form, setForm] = useState<{ type: View; goal?: string } | null>(null)
  const [updating, setUpdating] = useState(false)
  const [updateError, setUpdateError] = useState<string | null>(null)
  const [collapsedGoals, setCollapsedGoals] = useState<Set<string>>(() => new Set())
  const goals = data?.goals ?? []
  const actions = data?.actions ?? []
  const members = data?.members ?? []
  const canAssign = goals.length > 0 && members.length > 0

  async function updateState(type: View, id: string, state: string) {
    setUpdating(true)
    setUpdateError(null)
    try {
      await readJson(await request(`/${type}/${id}/state`, jsonBody('PATCH', { state })))
      onSuccess(type === 'goals' ? 'Estado da meta atualizado.' : 'Estado da ação atualizado.')
    } catch (reason) {
      setUpdateError(errorMessage(reason))
    } finally {
      setUpdating(false)
    }
  }

  function actionRow(action: Action, showGoal: boolean) {
    const goal = goals.find((item) => item.id === action.goalId)
    const member = members.find((item) => item.id === action.responsibleMemberId)
    const subtitle = [showGoal && goal && sourceName(goal.category), member?.name].filter(Boolean).join(' · ')
    return <ActionRow action={action} key={action.id} subtitle={subtitle}>
      <StateSelect label={`Estado de ${action.title}`} options={actionStates} value={action.state} disabled={updating || loading} update={(value) => void updateState('actions', action.id, value)}/>
    </ActionRow>
  }

  function toggleGoal(id: string) {
    setCollapsedGoals((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return <>
    <PageHeading title="Metas e ações" description="Defina metas de redução e acompanhe ações da organização.">
      <button className="secondary-button" onClick={onRefresh} disabled={loading} aria-label="Atualizar metas e ações"><Icon name="refresh"/></button>
      <button className="primary-button" onClick={() => setForm({ type: view })} disabled={view === 'actions' && !canAssign}>
        <Icon name="plus"/>{view === 'goals' ? 'Nova meta' : 'Nova ação'}
      </button>
    </PageHeading>
    <div className="section-tabs" role="group" aria-label="Metas e ações">
      <button aria-pressed={view === 'goals'} onClick={() => setView('goals')}>Metas</button>
      <button aria-pressed={view === 'actions'} onClick={() => setView('actions')}>Ações</button>
    </div>
    {(error || updateError) && <p className="form-error" role="alert">{error || updateError}</p>}
    {loading && !data ? <div className="panel loading-state" role="status">Carregando metas e ações…</div> : !error && <>
      {view === 'goals' && <div className="goal-list">
        {goals.length === 0 && <div className="panel"><Empty icon="goals" title="Nenhuma meta cadastrada">Crie uma meta para acompanhar a redução por fonte.</Empty></div>}
        {goals.map((goal) => {
          const linked = actions.filter((action) => action.goalId === goal.id)
          const name = sourceName(goal.category)
          const collapsed = collapsedGoals.has(goal.id)
          const detailsId = `goal-details-${goal.id}`
          const dueDate = formatDate(goal.dueDate)
          return <article className="panel goal-card" key={goal.id}>
            <header className="goal-header">
              <span className="metric-icon"><Icon name="goals"/></span>
              <h2>Reduzir emissões de {name.toLocaleLowerCase('pt-BR')}</h2>
              <div className="goal-target"><Status state={goal.state}/><p><strong>−{formatNumber(goal.targetPercent)}%</strong> até {dueDate}</p></div>
              <button
                type="button"
                className="secondary-button goal-toggle"
                aria-expanded={!collapsed}
                aria-controls={detailsId}
                aria-label={`${collapsed ? 'Expandir' : 'Recolher'} meta de ${name}, prazo ${dueDate}`}
                onClick={() => toggleGoal(goal.id)}
              >{collapsed ? 'Expandir' : 'Recolher'}</button>
            </header>
            <div id={detailsId} hidden={collapsed}>
              <div className="goal-details-heading">
                <p>Base: {formatNumber(goal.baseValueKgCo2e)} kg CO₂e · {formatDate(goal.basePeriodStart)} a {formatDate(goal.basePeriodEnd)}</p>
                <StateSelect label={`Estado da meta de ${name}, prazo ${dueDate}`} options={goalStates} value={goal.state} disabled={updating || loading} update={(value) => void updateState('goals', goal.id, value)}/>
              </div>
              <div className="goal-progress">
                <div><strong>Progresso ambiental</strong><span>Indisponível</span></div>
                <div className="progress-track" aria-hidden="true"/>
                <p>{Number(goal.baseValueKgCo2e) === 0 ? 'O valor-base é zero.' : 'Aguardando períodos com cobertura e metodologia comparáveis.'}</p>
              </div>
              <div className="linked-actions">
                <div className="card-heading">
                  <h3>Ações vinculadas ({linked.length})</h3>
                  {canAssign && <button className="link-button" onClick={() => setForm({ type: 'actions', goal: goal.id })}><Icon name="plus"/>Nova ação</button>}
                </div>
                {linked.length > 0 ? <ul className="action-list">{linked.map((action) => actionRow(action, false))}</ul> : <p className="muted">Nenhuma ação vinculada.</p>}
              </div>
            </div>
          </article>
        })}
      </div>}
      {view === 'actions' && <div className="panel">
        {actions.length > 0 ? <ul className="action-list standalone-actions">{actions.map((action) => actionRow(action, true))}</ul>
          : <Empty icon="actions" title="Nenhuma ação cadastrada">{canAssign ? 'Crie uma ação vinculada a uma meta.' : 'Cadastre uma meta e selecione um membro ativo para atribuir uma ação.'}</Empty>}
      </div>}
    </>}
    <Info>A conclusão das ações indica andamento operacional. A redução de emissões depende dos cálculos e da comparação entre períodos.</Info>
    {form && <PlanDialog type={form.type} selectedGoal={form.goal} goals={goals} members={members} request={request} onClose={() => setForm(null)} onSuccess={(message) => {
      setView(form.type)
      setForm(null)
      onSuccess(message)
    }}/>}
  </>
}

const assignedFilters = [
  { id: 'open', label: 'Abertas', empty: 'Nenhuma ação aberta' },
  { id: 'CONCLUIDA', label: 'Concluídas', empty: 'Nenhuma ação concluída' },
  { id: 'CANCELADA', label: 'Canceladas', empty: 'Nenhuma ação cancelada' },
] as const

function nextActionStep(state: string) {
  if (state === 'PLANEJADA') return { state: 'EM_ANDAMENTO', label: 'Iniciar' }
  if (state === 'EM_ANDAMENTO') return { state: 'CONCLUIDA', label: 'Concluir' }
  return null
}

export function AssignedActionsPage({ request, actions, loading, error, onRefresh }: {
  request: Request
  actions: Action[] | null
  loading: boolean
  error: string | null
  onRefresh: () => void
}) {
  const [filter, setFilter] = useState<(typeof assignedFilters)[number]>(assignedFilters[0])
  const [updatingId, setUpdatingId] = useState<string | null>(null)
  const [updateError, setUpdateError] = useState<string | null>(null)
  const [notice, setNotice] = useState('')
  const busy = loading || updatingId !== null
  const visible = (actions ?? []).filter((action) => filter.id === 'open'
    ? nextActionStep(action.state) !== null
    : action.state === filter.id)

  async function advance(action: Action, state: string) {
    setUpdatingId(action.id)
    setUpdateError(null)
    setNotice('')
    try {
      await readJson<Action>(await request(`/actions/${action.id}/state`, jsonBody('PATCH', { state })))
      setNotice(state === 'CONCLUIDA' ? 'Ação concluída. Consulte em Concluídas.' : 'Ação iniciada.')
    } catch (reason) {
      setUpdateError(errorMessage(reason))
    } finally {
      onRefresh()
      setUpdatingId(null)
    }
  }

  return <>
    <PageHeading title="Minhas ações" description="Acompanhe os prazos e atualize o andamento das ações atribuídas a você.">
      <button type="button" className="secondary-button" disabled={busy} onClick={() => {
        setUpdateError(null)
        setNotice('')
        onRefresh()
      }}><Icon name="refresh"/>Atualizar</button>
    </PageHeading>
    <div className="switcher" role="group" aria-label="Estado das ações">
      {assignedFilters.map((option) => <button type="button" key={option.id} aria-pressed={filter.id === option.id} onClick={() => setFilter(option)}>{option.label}</button>)}
    </div>
    {notice && <p className="notice success" role="status">{notice}</p>}
    {updateError && <p className="form-error" role="alert">{updateError}</p>}
    {error ? <p className="form-error" role="alert">{error}</p>
      : loading && !actions ? <div className="panel loading-state" role="status">Carregando ações…</div>
      : <div className="panel" aria-busy={busy}>
        {visible.length === 0 ? <Empty icon="actions" title={filter.empty}/>
          : <ul className="action-list standalone-actions">
            {visible.map((action) => {
              const next = nextActionStep(action.state)
              return <ActionRow action={action} key={action.id}>
                {next && <button type="button" className="primary-button action-step" aria-label={`${next.label} ação ${action.title}`} disabled={busy} onClick={() => void advance(action, next.state)}>
                  {updatingId === action.id ? 'Salvando…' : next.label}
                </button>}
              </ActionRow>
            })}
          </ul>}
      </div>}
  </>
}

function ActionRow({ action, subtitle, children }: { action: Action; subtitle?: string; children: ReactNode }) {
  return <li className="action-row">
    <span className="metric-icon"><Icon name={action.state === 'CONCLUIDA' ? 'check' : 'actions'}/></span>
    <div>
      <h3>{action.title}</h3>
      {action.description && <p>{action.description}</p>}
      <small>{subtitle && `${subtitle} · `}Prazo {formatDate(action.dueDate)}</small>
    </div>
    <Status state={action.state}/>
    {children}
  </li>
}

function StateSelect({ label, value, options, disabled, update }: { label: string; value: string; options: string[]; disabled: boolean; update: (value: string) => void }) {
  return <select className="state-select" aria-label={label} value="" disabled={disabled} onChange={(event) => update(event.target.value)}>
    <option value="" disabled>Alterar estado</option>
    {options.filter((option) => option !== value).map((option) => <option key={option} value={option}>{statusLabel(option)}</option>)}
  </select>
}

function PlanDialog({ type, selectedGoal, goals, members, request, onClose, onSuccess }: {
  type: View
  selectedGoal?: string
  goals: Goal[]
  members: Member[]
  request: Request
  onClose: () => void
  onSuccess: (message: string) => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const isGoal = type === 'goals'

  useEffect(() => { dialog.current?.showModal() }, [])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    const fields = new FormData(event.currentTarget)
    const body = isGoal ? {
      category: fields.get('category'),
      basePeriodStart: start,
      basePeriodEnd: end,
      baseValueKgCo2e: fields.get('baseValue'),
      targetPercent: fields.get('target'),
      dueDate: fields.get('dueDate'),
    } : {
      goalId: fields.get('goal'),
      responsibleMemberId: fields.get('member'),
      title: String(fields.get('title')).trim(),
      description: String(fields.get('description')).trim() || null,
      dueDate: fields.get('dueDate'),
    }

    try {
      await readJson(await request('/' + type, jsonBody('POST', body)))
      onSuccess(isGoal ? 'Meta criada.' : 'Ação criada e atribuída.')
    } catch (reason) {
      setError(errorMessage(reason))
    } finally {
      setBusy(false)
    }
  }

  return <dialog className="panel detail-dialog" ref={dialog} aria-labelledby="plan-title" onClose={onClose} onCancel={(event) => { if (busy) event.preventDefault() }}>
    <form method="dialog" className="dialog-heading">
      <h2 id="plan-title">{isGoal ? 'Nova meta de redução' : 'Nova ação'}</h2><button className="secondary-button" disabled={busy}>Fechar</button>
    </form>
    <form onSubmit={(event) => void submit(event)}>
      <fieldset className="form-grid" disabled={busy}>
        {isGoal ? <>
          <div className="field">
            <label htmlFor="goal-category">Fonte</label>
            <select id="goal-category" name="category">{sources.map((source) => <option key={source.category} value={source.category}>{source.name}</option>)}</select>
          </div>
          <label className="field">Valor-base (kg CO₂e)<input name="baseValue" type="number" min="0" step="any" required/></label>
          <label className="field">Início do período-base<input type="date" required value={start} onChange={(event) => setStart(event.target.value)} max={end || undefined}/></label>
          <label className="field">Fim do período-base<input type="date" required value={end} onChange={(event) => setEnd(event.target.value)} min={start || undefined}/></label>
          <label className="field">Redução-alvo (%)<input name="target" type="number" min="0.01" max="100" step="any" required/></label>
          <label className="field">Prazo<input name="dueDate" type="date" required/></label>
        </> : <>
          <div className="field">
            <label htmlFor="action-goal">Meta</label>
            <select id="action-goal" name="goal" required defaultValue={selectedGoal ?? ''}>
              <option value="">Selecione uma meta</option>
              {goals.map((goal) => <option key={goal.id} value={goal.id}>{sourceName(goal.category)} · {formatNumber(goal.targetPercent)}% · {formatDate(goal.dueDate)}</option>)}
            </select>
          </div>
          <div className="field">
            <label htmlFor="action-member">Responsável</label>
            <select id="action-member" name="member" required>
              <option value="">Selecione um membro</option>
              {members.map((member) => <option key={member.id} value={member.id}>{member.name}</option>)}
            </select>
          </div>
          <label className="field">Título<input name="title" maxLength={160} required/></label>
          <label className="field">Prazo<input name="dueDate" type="date" required/></label>
          <label className="field wide-field">Descrição<textarea name="description" maxLength={4000} rows={3}/></label>
        </>}
      </fieldset>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="form-actions"><button className="primary-button" disabled={busy}>{busy ? 'Salvando…' : isGoal ? 'Criar meta' : 'Criar ação'}</button></div>
    </form>
  </dialog>
}
