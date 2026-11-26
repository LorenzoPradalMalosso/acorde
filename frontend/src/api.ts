export type Membership = { organizationId: string; organizationName: string; memberId: string; role: string }
export type Profile = { userId: string; name: string; email: string; organizationId: string; organizationName: string; memberId: string; role: 'GESTOR' | 'COLABORADOR' }
export type Session = { accessToken: string; expiresAt: string; profile: Profile }
export type Activity = { id: string; category: string; periodStart: string; periodEnd: string; quantity: number; unit: string; state: string; justification: string | null; revision: number | null; emissionsKgCo2e: number | null; current: boolean; canReprocess: boolean }
export type Goal = { id: string; category: string; basePeriodStart: string; basePeriodEnd: string; baseValueKgCo2e: number; targetPercent: number; dueDate: string; state: string }
export type Action = { id: string; goalId: string; responsibleMemberId: string; title: string; description: string | null; dueDate: string; state: string }
export type Member = { id: string; name: string; email: string; role: string }
export type ApiError = { message?: string; memberships?: Membership[] }
export type Request = (path: string, options?: RequestInit) => Promise<Response>

export const api = import.meta.env.VITE_API_BASE_URL ?? '/api'

export async function readJson<T>(response: Response): Promise<T> {
  const body = await response.json().catch(() => null) as (T & ApiError) | null
  if (!response.ok || body === null) throw new Error(body?.message ?? `Falha na API (${response.status}).`)
  return body
}

export function jsonBody(method: 'POST' | 'PATCH', body: unknown): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
}

export const errorMessage = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível concluir a operação.'

