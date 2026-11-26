import { statusLabel } from './format'
import type { ReactNode } from 'react'

const symbols = {
  dashboard: <><path d="m3 10 9-7 9 7v10H3z"/><path d="M9 20v-6h6v6"/></>,
  cloud: <path d="M6 18a4 4 0 0 1-.6-8A6.5 6.5 0 0 1 18 8.7a4.7 4.7 0 0 1 0 9.3Z"/>,
  energy: <path d="m13 2-9 12h7l-1 8 10-12h-7z"/>,
  car: <><path d="m5 11 2-5h10l2 5m-13 8v2m12-2v2"/><rect x="3" y="11" width="18" height="8" rx="2"/><path d="M7 15h.01M17 15h.01"/></>,
  layers: <><path d="m12 3 9 5-9 5-9-5z"/><path d="m3 12 9 5 9-5M3 16l9 5 9-5"/></>,
  goals: <><circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="4"/><path d="m12 12 8-8m-3 0h3v3"/></>,
  building: <><path d="M3 21h18M5 21V9h6v12m0-12V4h8v17M8 12h.01M8 16h.01M14 8h2m-2 4h2m-2 4h2"/></>,
  sun: <><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19m0-14-1.5 1.5m-11 11L5 19"/></>,
  moon: <path d="M20.9 14.3A9 9 0 0 1 9.7 3.1 9 9 0 1 0 20.9 14.3Z"/>,
  eye: <><path d="M2 12s3.7-6 10-6 10 6 10 6-3.7 6-10 6-10-6-10-6Z"/><circle cx="12" cy="12" r="2.5"/></>,
  eyeOff: <><path d="M4 4l16 16M10 6.2a10 10 0 0 1 2-.2c6.3 0 10 6 10 6a16 16 0 0 1-3.2 3.4M6.4 7.8A16 16 0 0 0 2 12s3.7 6 10 6a11 11 0 0 0 4.2-.9"/></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 10h18"/></>,
  plus: <path d="M12 5v14M5 12h14"/>,
  check: <path d="m5 12 5 5L19 7"/>,
  actions: <><rect x="4" y="4" width="16" height="16" rx="2"/><path d="m8 12 3 3 5-6"/></>,
  info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v5m0-8h.01"/></>,
  refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M6 7a7 7 0 0 1 11-1l3 6M4 12l3 6a7 7 0 0 0 11-1"/></>,
  filter: <path d="M4 6h16M7 12h10m-7 6h4"/>,
  exit: <><path d="M10 4H4v16h6m-1-8h12m-4-4 4 4-4 4"/></>,
}

export type IconName = keyof typeof symbols

export function Icon({ name }: { name: IconName }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="icon">{symbols[name]}</svg>
}

export function PageHeading({ title, description, children }: { title: string; description: string; children?: ReactNode }) {
  return <header className="page-heading">
    <div><h1 tabIndex={-1}>{title}</h1><p>{description}</p></div>
    {children && <div className="page-actions">{children}</div>}
  </header>
}

export function Info({ children }: { children: ReactNode }) {
  return <aside className="info-banner"><Icon name="info"/><p>{children}</p></aside>
}

export function Empty({ icon, title, children }: { icon: IconName; title: string; children?: ReactNode }) {
  return <div className="empty-state"><span className="metric-icon"><Icon name={icon}/></span><strong>{title}</strong>{children && <p>{children}</p>}</div>
}

export function Status({ state }: { state: string }) {
  return <span className={`status status-${state.toLowerCase()}`}>{statusLabel(state)}</span>
}
