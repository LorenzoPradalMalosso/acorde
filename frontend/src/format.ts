export const sources = [
  { category: 'ENERGIA_ELETRICA', name: 'Energia elétrica', icon: 'energy' },
  { category: 'DESLOCAMENTO', name: 'Mobilidade', icon: 'car' },
] as const

export type SourceFilter = 'all' | typeof sources[number]['category']

export function sourceName(category: string) {
  return sources.find((source) => source.category === category)?.name ?? category
}

export const formatNumber = (value: number | string) => Number(value).toLocaleString('pt-BR', { maximumFractionDigits: 8 })
export const formatDate = (date: string) => new Date(`${date}T12:00:00`).toLocaleDateString('pt-BR')

export function statusLabel(state: string) {
  switch (state) {
    case 'VALIDA': return 'Calculado'
    case 'PENDENTE': return 'Pendente'
    case 'SUBSTITUIDA': return 'Substituído'
    case 'ATIVA': return 'Ativa'
    case 'PLANEJADA': return 'Planejada'
    case 'EM_ANDAMENTO': return 'Em andamento'
    case 'CONCLUIDA': return 'Concluída'
    case 'CANCELADA': return 'Cancelada'
    default: return state
  }
}
