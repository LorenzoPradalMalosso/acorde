import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { errorMessage, jsonBody, readJson } from './api'
import type { Activity, Request } from './api'
import { Empty, Icon, Info, PageHeading, Status } from './ui'
import { formatDate, formatNumber, sourceName, sources } from './format'
import type { SourceFilter } from './format'

const commuteModes = {
  ONIBUS: 'Ônibus',
  METRO: 'Metrô',
  TREM: 'Trem',
  CARRO: 'Carro',
  MOTO: 'Moto',
  BICICLETA: 'Bicicleta',
  A_PE: 'A pé',
}

export function ActivityEntryPage({ request, kind, onCreated, goRecords }: {
  request: Request
  kind: 'energy' | 'commute'
  onCreated: (message: string) => void
  goRecords: () => void
}) {
  const commute = kind === 'commute'
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const idempotencyKey = useRef<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const fields = new FormData(form)
    setBusy(true)
    setError(null)
    try {
      idempotencyKey.current ??= crypto.randomUUID()
      const activity = {
        idempotencyKey: idempotencyKey.current,
        periodStart: start,
        periodEnd: end,
        region: String(fields.get('region')).trim(),
        ...(commute ? {
          mode: fields.get('mode'),
          roundTripDistanceKm: fields.get('distance'),
          commutingDays: fields.get('days'),
        } : { consumptionKwh: fields.get('consumption') }),
      }
      const path = commute ? '/carbon/activities/commutes' : '/carbon/activities/energy'
      const record = await readJson<Activity>(await request(path, jsonBody('POST', activity)))
      const message = record.emissionsKgCo2e === null
        ? `Registro salvo com cálculo pendente. ${record.justification ?? ''}`.trim()
        : `${commute ? 'Deslocamento' : 'Consumo'} registrado. Estimativa: ${formatNumber(record.emissionsKgCo2e)} kg CO₂e.`
      idempotencyKey.current = null
      form.reset()
      setStart('')
      setEnd('')
      onCreated(message)
    } catch (error) {
      setError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  return <>
    <PageHeading title={commute ? 'Registrar deslocamento' : 'Energia elétrica'} description={commute
      ? 'Informe seus deslocamentos entre casa e trabalho no período.'
      : 'Registre o consumo elétrico da organização para o período informado.'}>
      <button className="secondary-button" onClick={goRecords}>Ver histórico</button>
    </PageHeading>
    <div className="form-layout">
      <form className="panel data-form" onSubmit={(event) => void submit(event)} onChange={() => { idempotencyKey.current = null }}>
        <div className="form-title"><span className="metric-icon"><Icon name={commute ? 'car' : 'energy'}/></span><div><h2>Novo registro</h2><p className="muted">Campos com * são obrigatórios.</p></div></div>
        <fieldset className="form-grid" disabled={busy}>
          {commute ? <>
            <label className="field">Modalidade *
              <select name="mode" required defaultValue="">
                <option value="" disabled>Selecione a modalidade</option>
                {Object.entries(commuteModes).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
            <label className="field">Distância de ida e volta (km) *<input name="distance" type="number" min="0.000001" step="any" required placeholder="Ex.: 20"/></label>
            <label className="field">Dias com deslocamento *<input name="days" type="number" min="0.0001" step="any" required placeholder="Ex.: 5"/></label>
          </> : <label className="field">Consumo (kWh) *<input name="consumption" type="number" min="0.000001" step="any" required placeholder="Ex.: 1250,5"/></label>}
          <label className="field">{commute ? 'Região' : 'Região/rede elétrica'} *<input name="region" required maxLength={160} placeholder="Ex.: BR-SP"/></label>
          <label className="field">Início do período *<input type="date" required value={start} onChange={(event) => setStart(event.target.value)} max={end || undefined}/></label>
          <label className="field">Fim do período *<input type="date" required value={end} onChange={(event) => setEnd(event.target.value)} min={start || undefined}/></label>
        </fieldset>
        {error && <p className="form-error" role="alert">{error}</p>}
        <div className="form-actions"><button className="primary-button" disabled={busy}>{busy ? 'Salvando…' : 'Salvar registro'}</button></div>
      </form>
      <aside className="panel calc-card">
        <span className="metric-icon"><Icon name="info"/></span>
        <h2>Cálculo automático</h2>
        <p>{commute
          ? 'A API multiplica a distância de ida e volta pelos dias informados e usa um fator compatível.'
          : 'A API busca um fator compatível com unidade, região, fonte e período.'}</p>
        <div className="calc-equation">E = Q × FE</div>
        <p>Sem fator compatível, o registro permanece pendente.</p>
      </aside>
    </div>
  </>
}

const historySources = ['all', 'DESLOCAMENTO', 'ENERGIA_ELETRICA'] as const satisfies readonly SourceFilter[]

export function RecordsPage({ request, records, loading, error, reload, onReprocessed, source, onSource, pendingOnly, onPendingOnly, registerCommute }: {
  request: Request
  records: Activity[]
  loading: boolean
  error: string | null
  reload: () => void
  onReprocessed?: () => void
  source: SourceFilter
  onSource: (source: SourceFilter) => void
  pendingOnly: boolean
  onPendingOnly: (value: boolean) => void
  registerCommute?: () => void
}) {
  const [selected, setSelected] = useState<Activity | null>(null)
  const visible = records.filter((item) => (source === 'all' || item.category === source) && (!pendingOnly || item.state === 'PENDENTE'))

  return <>
    <PageHeading title="Histórico" description={registerCommute
      ? 'Consulte seus deslocamentos e as estimativas de emissão.'
      : 'Consulte os registros de mobilidade e energia elétrica da organização e suas estimativas de emissão.'}>
      {registerCommute && <button className="primary-button" onClick={registerCommute}><Icon name="plus"/>Registrar deslocamento</button>}
      <button className="secondary-button" onClick={reload} disabled={loading}><Icon name="refresh"/>Atualizar</button>
    </PageHeading>
    <div className="record-filters">
      {!registerCommute && <div className="switcher" role="group" aria-label="Fonte dos registros">
        {historySources.map((category) => <button key={category} type="button" aria-pressed={source === category} onClick={() => onSource(category)}>
          {category === 'all' ? 'Todos' : sourceName(category)}
        </button>)}
      </div>}
      <label className="record-pending-filter">
        <input type="checkbox" checked={pendingOnly} onChange={(event) => onPendingOnly(event.target.checked)}/>
        Somente pendentes
      </label>
    </div>
    {error ? <p className="form-error" role="alert">{error}</p>
      : loading && records.length === 0 ? <div className="panel loading-state" role="status">Carregando registros…</div>
      : visible.length === 0 ? <div className="panel"><Empty icon="cloud" title={pendingOnly ? 'Nenhum registro pendente' : 'Nenhum registro disponível'}/></div>
      : <div className="panel table-wrap" tabIndex={0} role="region" aria-label="Registros de emissão">
        <table>
          <thead><tr><th scope="col">Fonte</th><th scope="col">Período</th><th scope="col">Quantidade</th><th scope="col">Estado</th><th scope="col">Emissões</th><th scope="col">Cálculo</th></tr></thead>
          <tbody>{visible.map((item) => <tr key={item.id}>
            <td><strong>{sourceName(item.category)}</strong></td>
            <td>{formatDate(item.periodStart)} a {formatDate(item.periodEnd)}</td>
            <td>{formatNumber(item.quantity)} {item.unit}</td>
            <td><Status state={item.state}/></td>
            <td>{item.emissionsKgCo2e === null ? '—' : `${formatNumber(item.emissionsKgCo2e)} kg CO₂e`}</td>
            <td><button className="link-button" onClick={() => setSelected(item)}>Ver revisões</button></td>
          </tr>)}</tbody>
        </table>
      </div>}
    <Info>
      {!registerCommute && source === 'DESLOCAMENTO' && 'Cada colaborador registra o próprio deslocamento pela Web ou pelo app Acorde. '}
      Uma estimativa depende de fator compatível com fonte, unidade, região e período.
    </Info>
    {selected && <CalculationDialog request={request} activity={selected} onClose={() => setSelected(null)} onReprocessed={(updated) => {
      setSelected(updated)
      reload()
      onReprocessed?.()
    }}/>}
  </>
}

type Calculation = {
  revision: number
  factorId: string | null
  factorValue: number | null
  factorSource: string | null
  factorVersion: string | null
  factorScope: string | null
  factorRegion: string | null
  factorAssumptions: string | null
  factorActivityUnit: string | null
  emissionsKgCo2e: number | null
  state: string
  current: boolean
  justification: string | null
}

function CalculationDialog({ request, activity, onClose, onReprocessed }: {
  request: Request; activity: Activity; onClose: () => void; onReprocessed: (activity: Activity) => void
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [revisions, setRevisions] = useState<Calculation[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reprocessError, setReprocessError] = useState<string | null>(null)
  const [reprocessing, setReprocessing] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => { dialog.current?.showModal() }, [])

  useEffect(() => {
    const controller = new AbortController()
    void request(`/carbon/activities/${activity.id}/calculations`, { signal: controller.signal })
      .then(readJson<Calculation[]>)
      .then(
        (items) => { if (!controller.signal.aborted) { setError(null); setRevisions(items) } },
        (reason: unknown) => { if (!controller.signal.aborted) setError(errorMessage(reason)) },
      )
    return () => controller.abort()
  }, [activity.id, activity.revision, request])

  async function reprocess() {
    setReprocessing(true)
    setReprocessError(null)
    setNotice(null)
    try {
      const updated = await readJson<Activity>(await request(`/carbon/activities/${activity.id}/reprocess`, { method: 'POST' }))
      onReprocessed(updated)
      setNotice(updated.emissionsKgCo2e === null
        ? updated.justification ?? 'O cálculo continua pendente.'
        : `Estimativa atualizada: ${formatNumber(updated.emissionsKgCo2e)} kg CO₂e.`)
    } catch (reason) {
      setReprocessError(errorMessage(reason))
    } finally {
      setReprocessing(false)
    }
  }

  return <dialog ref={dialog} className="panel detail-dialog" aria-labelledby="calculation-title" onClose={onClose} onCancel={(event) => {
    if (reprocessing) event.preventDefault()
  }}>
    <form method="dialog" className="dialog-heading">
      <div><h2 id="calculation-title">Revisões do cálculo</h2><p className="muted">{sourceName(activity.category)} · {formatDate(activity.periodStart)} a {formatDate(activity.periodEnd)}</p></div>
      <button className="secondary-button" disabled={reprocessing}>Fechar</button>
    </form>
    {activity.canReprocess && activity.state === 'PENDENTE' && <button className="primary-button" disabled={reprocessing} onClick={() => void reprocess()}>
      {reprocessing ? 'Recalculando…' : 'Recalcular estimativa'}
    </button>}
    {notice && <p role="status">{notice}</p>}
    {reprocessError && <p className="form-error" role="alert">{reprocessError}</p>}
    {error ? <p className="form-error" role="alert">{error}</p> : revisions === null
      ? <p role="status">Carregando revisões…</p>
      : revisions.length === 0 ? <p>{activity.justification ?? 'Nenhuma revisão disponível.'}</p>
      : revisions.map((revision) => <section className="revision-card" key={revision.revision}>
        <div className="card-heading"><h3>Revisão {revision.revision}{revision.current ? ' · vigente' : ''}</h3><Status state={revision.state}/></div>
        {revision.emissionsKgCo2e !== null && <p><strong>{formatNumber(revision.emissionsKgCo2e)} kg CO₂e</strong></p>}
        {revision.factorId && <dl className="detail-list">
          <dt>Fator</dt><dd>{revision.factorValue === null ? '—' : formatNumber(revision.factorValue)} kg CO₂e / {revision.factorActivityUnit}</dd>
          <dt>Fonte e versão</dt><dd>{revision.factorSource} · {revision.factorVersion}</dd>
          <dt>Escopo e região</dt><dd>{revision.factorScope?.replace('_', ' ')} · {revision.factorRegion}</dd>
          {revision.factorAssumptions && <><dt>Premissas</dt><dd>{revision.factorAssumptions}</dd></>}
        </dl>}
        {revision.justification && <p className="muted">{revision.justification}</p>}
      </section>)}
  </dialog>
}

type Factor = {
  id: string
  escopo: string
  valor: number
  unidade_atividade: string
  unidade_resultado: string
  fonte: string
  versao: string
  valido_desde: string
  valido_ate: string | null
  regiao: string
  premissas: string
}

export function FactorsPage({ request }: { request: Request }) {
  const [factors, setFactors] = useState<Factor[] | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    const query = new URLSearchParams({
      category: String(fields.get('category')),
      region: String(fields.get('region')).trim(),
      date: String(fields.get('date')),
    })
    setBusy(true)
    setError(null)
    setFactors(null)
    try {
      setFactors(await readJson<Factor[]>(await request('/factors?' + query)))
    } catch (error) {
      setError(errorMessage(error))
    } finally {
      setBusy(false)
    }
  }

  return <>
    <PageHeading title="Fatores de emissão" description="Consulte o catálogo por fonte, região e data de validade."/>
    <form className="panel data-form" onSubmit={(event) => void search(event)}>
      <fieldset className="form-grid factor-filters" disabled={busy}>
        <div className="field">
          <label htmlFor="factor-category">Fonte</label>
          <select id="factor-category" name="category">{sources.map((item) => <option key={item.category} value={item.category}>{item.name}</option>)}</select>
        </div>
        <label className="field">Região<input name="region" required maxLength={160} placeholder="Ex.: BR-SP"/></label>
        <label className="field">Data<input name="date" type="date" required/></label>
        <button className="primary-button">{busy ? 'Consultando…' : 'Consultar fatores'}</button>
      </fieldset>
    </form>
    {error && <p className="form-error" role="alert">{error}</p>}
    {factors?.length === 0 && <div className="panel"><Empty icon="layers" title="Nenhum fator encontrado">Confira a fonte, a região e a data da consulta.</Empty></div>}
    {factors?.map((factor) => <article className="panel factor-card" key={factor.id}>
      <div className="card-heading"><h2>{factor.fonte}</h2><span className="status">Versão {factor.versao}</span></div>
      <dl className="detail-list">
        <dt>Valor</dt><dd>{formatNumber(factor.valor)} {factor.unidade_resultado} / {factor.unidade_atividade}</dd>
        <dt>Escopo e região</dt><dd>{factor.escopo.replace('_', ' ')} · {factor.regiao === '*' ? 'Todas as regiões' : factor.regiao}</dd>
        <dt>Validade</dt><dd>{formatDate(factor.valido_desde)} a {factor.valido_ate ? formatDate(factor.valido_ate) : 'sem fim definido'}</dd>
        <dt>Premissas</dt><dd>{factor.premissas}</dd>
      </dl>
    </article>)}
  </>
}
