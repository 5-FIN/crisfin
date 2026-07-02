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

/** null 가능 금액 포맷 (rule 엔진 미산정 시 fallback) */
export function fmtAmount(n: number | null | undefined, fallback = '—'): string {
  return n == null ? fallback : fmt(n)
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

/* ────────────────────────────────────────────────
   마이데이터 집계
──────────────────────────────────────────────── */

/** 마이데이터 상황요약 4개 지표 */
export interface MyDataSummary {
  /** 고정지출(월): autoTransfers 금액 합 */
  fixedExpense: number
  /** 임박결제: cards 이번달 결제예정액(monthlyUsage) 합 */
  upcomingPayment: number
  /** 부채 잔금 합 (loans outstandingBalance) */
  debtOutstanding: number
  /** 부채 월상환액 합 (loans monthlyPayment) */
  debtMonthlyPayment: number
  /** 보험 월보험료 합 (insurance monthlyPremium) */
  insurancePremium: number
}

function sumBy(arr: unknown, key: string): number {
  if (!Array.isArray(arr)) return 0
  return arr.reduce((acc, item) => {
    const v = item && typeof item === 'object' ? (item as Record<string, unknown>)[key] : undefined
    return acc + (typeof v === 'number' ? v : 0)
  }, 0)
}

/**
 * 켜진/수정된 마이데이터로 상황요약 지표를 계산하는 순수함수.
 * 백엔드 V3 시드 키 기준: autoTransfers[].amount, cards[].monthlyUsage,
 * loans[].outstandingBalance/monthlyPayment, insurance[].monthlyPremium.
 */
export function summarizeMyData(data: Record<string, unknown> | undefined | null): MyDataSummary {
  const d = data ?? {}
  return {
    fixedExpense:       sumBy(d.autoTransfers, 'amount'),
    upcomingPayment:    sumBy(d.cards, 'monthlyUsage'),
    debtOutstanding:    sumBy(d.loans, 'outstandingBalance'),
    debtMonthlyPayment: sumBy(d.loans, 'monthlyPayment'),
    insurancePremium:   sumBy(d.insurance, 'monthlyPremium'),
  }
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

/** 결제/로그인 후 재실행할 보류 분석 요청 localStorage 헬퍼 */
export const pendingAnalysisStore = {
  save: (req: unknown) => localStorage.setItem('cf_pending_analysis', JSON.stringify(req)),
  load: () => {
    try {
      const raw = localStorage.getItem('cf_pending_analysis')
      return raw ? JSON.parse(raw) : null
    } catch { return null }
  },
  clear: () => localStorage.removeItem('cf_pending_analysis'),
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

/** 납부 관리의 긴급 항목 처리완료 상태 localStorage 헬퍼(항목키→완료여부) */
export const paymentStore = {
  key: (analysisId: number) => `cf_payments_${analysisId}`,
  load: (analysisId: number): Record<string, boolean> => {
    try {
      const raw = localStorage.getItem(paymentStore.key(analysisId))
      return raw ? JSON.parse(raw) : {}
    } catch { return {} }
  },
  toggle: (analysisId: number, itemKey: string) => {
    const current = paymentStore.load(analysisId)
    current[itemKey] = !current[itemKey]
    localStorage.setItem(paymentStore.key(analysisId), JSON.stringify(current))
    return current
  },
  /** 완료되지 않은 긴급 항목 수 = 사이드바 배지 값 */
  pendingCount: (analysisId: number, urgentKeys: string[]): number => {
    const done = paymentStore.load(analysisId)
    return urgentKeys.filter(k => !done[k]).length
  },
}

/** 마이데이터 항목 on/off 선택 상태 localStorage 헬퍼 (필드키→포함여부) */
export const myDataSelectionStore = {
  load: (): Record<string, boolean> | null => {
    try {
      const raw = localStorage.getItem('cf_mydata_selection')
      return raw ? JSON.parse(raw) : null
    } catch { return null }
  },
  save: (selection: Record<string, boolean>) =>
    localStorage.setItem('cf_mydata_selection', JSON.stringify(selection)),
}
