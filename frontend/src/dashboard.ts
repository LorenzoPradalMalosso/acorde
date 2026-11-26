export type Dashboard = {
  from: string | null
  to: string | null
  activityCount: number
  pendingCount: number
  emissionsKgCo2e: number | null
  sources: { category: string; activityCount: number; validCount: number; pendingCount: number; emissionsKgCo2e: number | null }[]
  periods: { periodStart: string; periodEnd: string; activityCount: number; validCount: number; pendingCount: number; emissionsKgCo2e: number | null }[]
  coverageNote: string
}

export const periodOptions = [
  { value: 'all', label: 'Todo o período' },
  { value: 'year', label: 'Últimos 12 meses' },
  { value: 'month', label: 'Últimos 30 dias' },
] as const

export type PeriodFilter = typeof periodOptions[number]['value']

export function periodQuery(period: PeriodFilter) {
  if (period === 'all') return ''
  const to = new Date()
  const from = new Date(to)
  if (period === 'month') from.setDate(to.getDate() - 29)
  else {
    from.setDate(1)
    from.setMonth(to.getMonth() - 12)
    const daysInMonth = new Date(from.getFullYear(), from.getMonth() + 1, 0).getDate()
    from.setDate(Math.min(to.getDate(), daysInMonth))
  }
  const localDate = (date: Date) => [date.getFullYear(), date.getMonth() + 1, date.getDate()].map((part) => String(part).padStart(2, '0')).join('-')
  return '?' + new URLSearchParams({ from: localDate(from), to: localDate(to) })
}
