import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** 금액 → 억/만원/원 포맷 */
export function fmt(n: number): string {
  if (n >= 100_000_000) return `${(n / 100_000_000).toFixed(1)}억`
  if (n >= 10_000) return `${Math.round(n / 10_000).toLocaleString()}만원`
  return `${n.toLocaleString()}원`
}

/** D-Day 색상 */
export function dDayColor(days: number): string {
  if (days <= 5) return '#F59E0B'
  if (days <= 14) return '#F97316'
  return '#64748B'
}

/** 우선순위 배지 */
export function priorityBadge(p: 'HIGH' | 'MED' | 'LOW') {
  const map = {
    HIGH: { label: '긴급', bg: '#FFFBEB', color: '#D97706', border: '#FEF3C7' },
    MED:  { label: '주의', bg: '#FFF7ED', color: '#EA580C', border: '#FFEDD5' },
    LOW:  { label: '안전', bg: '#ECFDF5', color: '#059669', border: '#D1FAE5' },
  }
  return map[p]
}

/** riskLevel 배지 */
export function riskBadge(r: 'HIGH' | 'MED' | 'LOW') {
  return priorityBadge(r)
}

/** 위기유형 한글 레이블 */
export const CRISIS_LABELS: Record<string, string> = {
  HOSPITALIZATION: '입원/수술',
  ACCIDENT:        '사고/재해',
  UNEMPLOYMENT:    '실직/소득단절',
  CAREGIVING:      '간병',
  BEREAVEMENT:     '가족 사망',
}

export const CRISIS_EMOJI: Record<string, string> = {
  HOSPITALIZATION: '🏥',
  ACCIDENT:        '🚑',
  UNEMPLOYMENT:    '💼',
  CAREGIVING:      '🩺',
  BEREAVEMENT:     '🕊️',
}

/** 프로토타입 위기유형 키 → 백엔드 enum 매핑 */
export const CRISIS_KEY_MAP: Record<string, string> = {
  'hospitalization': 'HOSPITALIZATION',
  'accident':        'ACCIDENT',
  'job-loss':        'UNEMPLOYMENT',
  'caregiving':      'CAREGIVING',
  'bereavement':     'BEREAVEMENT',
}

/** 직업 유형 → 페르소나 매핑 */
export function jobToPersona(job: string): string {
  const map: Record<string, string> = {
    'employed':      'OFFICE_WORKER',
    'self_employed': 'SELF_EMPLOYED',
    'freelancer':    'FREELANCER',
    'laid_off':      'LAID_OFF',
    'public':        'PUBLIC_SERVANT',
  }
  return map[job] ?? 'OFFICE_WORKER'
}

/** 폼 데이터 → situationDescription 템플릿 변환 */
export function buildSituationDescription(params: {
  crisisType: string
  job: string
  income: string
  household: string
  detail: string
}): string {
  const label = CRISIS_LABELS[params.crisisType] ?? params.crisisType
  return `[위기 유형] ${label}\n[직업] ${params.job}\n[월 소득] ${params.income}만원\n[가구원 수] ${params.household}인\n[상세 상황] ${params.detail}`
}

/** localStorage 토큰 헬퍼 */
export const tokenStore = {
  getAccess: () => (typeof window !== 'undefined' ? localStorage.getItem('cf_access') : null),
  getRefresh: () => (typeof window !== 'undefined' ? localStorage.getItem('cf_refresh') : null),
  set: (access: string, refresh: string) => {
    localStorage.setItem('cf_access', access)
    localStorage.setItem('cf_refresh', refresh)
  },
  clear: () => {
    localStorage.removeItem('cf_access')
    localStorage.removeItem('cf_refresh')
  },
}

/** 분석 결과 localStorage 헬퍼 */
export const analysisStore = {
  save: (result: unknown) => localStorage.setItem('cf_analysis', JSON.stringify(result)),
  load: () => {
    try {
      const raw = localStorage.getItem('cf_analysis')
      return raw ? JSON.parse(raw) : null
    } catch { return null }
  },
  clear: () => localStorage.removeItem('cf_analysis'),
}

/** 태스크 완료 상태 localStorage 헬퍼 */
export const taskStore = {
  key: (analysisId: number) => `cf_tasks_${analysisId}`,
  load: (analysisId: number): Record<string, boolean> => {
    try {
      const raw = localStorage.getItem(taskStore.key(analysisId))
      return raw ? JSON.parse(raw) : {}
    } catch { return {} }
  },
  toggle: (analysisId: number, taskKey: string) => {
    const current = taskStore.load(analysisId)
    current[taskKey] = !current[taskKey]
    localStorage.setItem(taskStore.key(analysisId), JSON.stringify(current))
    return current
  },
}
