import type { CSSProperties, ReactNode } from 'react'
import { periodOptions } from './dashboard'
import type { Dashboard, PeriodFilter } from './dashboard'
import { formatDate, formatNumber, sources } from './format'
import { Empty, Icon, Info, PageHeading } from './ui'
import type { IconName } from './ui'

const chartNumber = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2, notation: 'compact' })
const percent = new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 1 })

export function DashboardPage({ dashboard, loading, error, period, onPeriod, onRetry, goRecords, goEnergy }: {
  dashboard: Dashboard | null
  loading: boolean
  error: string | null
  period: PeriodFilter
  onPeriod: (value: PeriodFilter) => void
  onRetry: () => void
  goRecords: (pendingOnly?: boolean) => void
  goEnergy: () => void
}) {
  const sourceValues = sources.map((source) => ({
    ...source,
    stats: dashboard?.sources.find((item) => item.category === source.category),
  }))
  const total = dashboard?.emissionsKgCo2e ?? null
  const pending = dashboard?.pendingCount
  const unavailable = loading ? 'Carregando…' : error ? 'Indisponível' : 'Sem cálculo válido'

  return <>
    <PageHeading title="Dashboard" description="Visão geral das emissões monitoradas da organização.">
      <button className="secondary-button" onClick={onRetry} disabled={loading}><Icon name="refresh"/>Atualizar</button>
      <div className="period-control">
        <Icon name="calendar"/>
        <select aria-label="Período do dashboard" value={period} onChange={(event) => onPeriod(event.target.value as PeriodFilter)}>
          {periodOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
      </div>
    </PageHeading>
    {error && <div className="notice error" role="alert">{error}<button className="secondary-button compact" onClick={onRetry}>Tentar novamente</button></div>}
    <div className="metric-grid" aria-busy={loading}>
      <Metric label="Emissões monitoradas" value={total} icon="cloud" fallback={unavailable}>
        {dashboard && `${formatNumber(dashboard.activityCount)} atividades no período`}
      </Metric>
      {sourceValues.map(({ category, name, icon, stats }, index) => <Metric key={category} label={name} value={stats?.emissionsKgCo2e ?? null} icon={icon} fallback={unavailable} tone={index === 0 ? 'energy' : 'mobility'}>
        {total && stats?.emissionsKgCo2e != null ? `${percent.format(stats.emissionsKgCo2e / total)} do total monitorado` : dashboard ? `${stats?.validCount ?? 0} registros calculados` : null}
      </Metric>)}
      <Metric label="Registros pendentes" value={pending ?? null} icon="layers" fallback={loading ? 'Carregando…' : 'Indisponível'} tone="pending">
        <button className="link-button" onClick={() => goRecords(true)}>Ver registros</button>
      </Metric>
    </div>
    <div className="charts-grid">
      <section className="panel chart-panel">
        <h2>Evolução das emissões</h2>
        <p className="muted">Emissões em kg CO₂e por intervalo registrado</p>
        <EmissionTrend periods={dashboard?.periods ?? []} availability={unavailable}/>
      </section>
      <section className="panel chart-panel">
        <h2>Composição por fonte</h2>
        <p className="muted">Participação no total monitorado</p>
        <div className="composition">
          <div className={`donut ${total && total > 0 ? 'has-data' : ''}`} style={{ '--energy-share': `${total ? (sourceValues[0].stats?.emissionsKgCo2e ?? 0) / total * 100 : 0}%` } as CSSProperties} aria-hidden="true">
            <div><strong>{total === null ? '—' : chartNumber.format(total)}</strong><span>kg CO₂e</span></div>
          </div>
          <ul className="chart-legend">
            {sourceValues.map(({ category, name, stats }, index) => <li key={category}>
              <span className={`legend-swatch source-${index}`} aria-hidden="true"/>
              <div><strong>{name}</strong><span>{total && stats?.emissionsKgCo2e != null ? `${percent.format(stats.emissionsKgCo2e / total)} · ${formatNumber(stats.emissionsKgCo2e)} kg CO₂e` : unavailable}</span></div>
            </li>)}
          </ul>
        </div>
      </section>
    </div>
    <Info>{dashboard?.coverageNote ?? 'O painel cobre as fontes e os períodos registrados.'} A comparação entre períodos depende de cobertura e metodologia compatíveis.</Info>
    <div className="dashboard-footer">
      <button className="secondary-button" onClick={() => goRecords()}>Ver histórico</button>
      <button className="primary-button" onClick={goEnergy}><Icon name="plus"/>Registrar energia</button>
    </div>
  </>
}

function Metric({ label, value, fallback, icon, tone, children }: {
  label: string
  value: number | null
  fallback: string
  icon: IconName
  tone?: string
  children?: ReactNode
}) {
  return <article className={`panel metric-card ${tone ?? ''}`}>
    <span className="metric-icon"><Icon name={icon}/></span>
    <h2>{label}</h2>
    <strong className={`metric-value ${value === null ? 'metric-unavailable' : ''}`}>{value === null ? fallback : formatNumber(value)}{value !== null && tone !== 'pending' && <small> kg CO₂e</small>}</strong>
    <div className="metric-detail">{children}</div>
  </article>
}

function EmissionTrend({ periods, availability }: { periods: Dashboard['periods']; availability: string }) {
  const ordered = [...periods].sort((a, b) => a.periodStart.localeCompare(b.periodStart) || a.periodEnd.localeCompare(b.periodEnd))
  const recent = ordered.slice(-12)
  const maximum = Math.max(0, ...recent.map((item) => item.emissionsKgCo2e ?? 0))
  const lines = [0, 1, 2, 3, 4]
  const points = recent.map((item, index) => ({
    ...item,
    x: recent.length < 2 ? 250 : index * 494 / (recent.length - 1) + 3,
    y: item.emissionsKgCo2e === null ? null : 135 - 125 * (item.emissionsKgCo2e / (maximum || 1)),
  }))

  return <>
    {maximum > 0 ? <figure className="trend">
      <div className="chart-axis" aria-hidden="true">{lines.map((line) => <span key={line}>{chartNumber.format(maximum * (1 - line / 4))}</span>)}</div>
      <svg className="trend-svg" viewBox="0 0 500 142" preserveAspectRatio="none" role="img" aria-label="Evolução das emissões calculadas. Os valores por intervalo estão na tabela.">
        <defs><linearGradient id="trend-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="var(--acorde-accent)" stopOpacity=".45"/><stop offset="100%" stopColor="var(--acorde-accent)" stopOpacity=".10"/></linearGradient></defs>
        {lines.map((line) => <line key={line} x1="0" x2="500" y1={10 + 31.25 * line} y2={10 + 31.25 * line} className="grid-line"/>)}
        {points.map((point, index) => {
          const next = points[index + 1]
          if (point.y === null || !next || next.y === null) return null
          return <g key={index}><path d={`M${point.x} 135V${point.y}L${next.x} ${next.y}V135Z`} fill="url(#trend-fill)"/>
            <line className="trend-line" x1={point.x} x2={next.x} y1={point.y} y2={next.y}/></g>
        })}
        {points.map((point, index) => point.y === null ? null : <circle className="trend-dot" key={index} cx={point.x} cy={point.y} r="3.3"/>)}
      </svg>
      <figcaption>
        <span>{formatDate(recent[0].periodStart)}</span>
        {recent.length > 1 && <span>{formatDate(recent[recent.length - 1].periodEnd)}</span>}
      </figcaption>
    </figure> : <div className="chart-empty"><Empty icon="cloud" title={availability}>{periods.length > 0 ? 'Os registros deste período ainda estão pendentes.' : 'A evolução aparece quando há registros calculados.'}</Empty></div>}
    {periods.length > 0 && <details className="intervals">
      <summary>Ver dados por intervalo</summary>
      <div className="table-wrap" tabIndex={0} role="region" aria-label="Dados por intervalo">
        <table><thead><tr><th scope="col">Intervalo</th><th scope="col">Válidos</th><th scope="col">Pendentes</th><th scope="col">Emissões</th></tr></thead>
          <tbody>{ordered.map((item) => <tr key={`${item.periodStart}-${item.periodEnd}`}><td>{formatDate(item.periodStart)} a {formatDate(item.periodEnd)}</td><td>{item.validCount}</td><td>{item.pendingCount}</td><td>{item.emissionsKgCo2e === null ? 'Sem cálculo válido' : `${formatNumber(item.emissionsKgCo2e)} kg CO₂e`}</td></tr>)}</tbody>
        </table>
      </div>
    </details>}
  </>
}
