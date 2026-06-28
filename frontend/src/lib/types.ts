/* ────────────────────────────────────────────────
   공통 API 래퍼
──────────────────────────────────────────────── */
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  timestamp: string
  error?: { code: string; message: string; detail?: string }
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

/* ────────────────────────────────────────────────
   인증
──────────────────────────────────────────────── */
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface SignupRequest {
  email: string
  password: string
  nickname: string
}

export interface LoginRequest {
  email: string
  password: string
}

/* ────────────────────────────────────────────────
   위기 유형
──────────────────────────────────────────────── */
export type CrisisType =
  | 'HOSPITALIZATION'
  | 'ACCIDENT'
  | 'UNEMPLOYMENT'
  | 'CAREGIVING'
  | 'BEREAVEMENT'

export interface CrisisTypeResponse {
  type: CrisisType
  label: string
  emoji: string
  isRecurring: boolean
  description: string
}

/* ────────────────────────────────────────────────
   길라잡이 (Guide)
──────────────────────────────────────────────── */
export interface GuideResponse {
  crisisType: CrisisType
  title: string
  coachingPrompt: string
  keyRules: string[]
  sourceLaws: string[]
  disclaimer: string
}

/* ────────────────────────────────────────────────
   MyData
──────────────────────────────────────────────── */
export type PersonaType =
  | 'OFFICE_WORKER'
  | 'SELF_EMPLOYED'
  | 'FREELANCER'
  | 'LAID_OFF'
  | 'PUBLIC_SERVANT'

/* ────────────────────────────────────────────────
   분석 (Analysis)
──────────────────────────────────────────────── */
export interface AnalysisRequest {
  crisisType: CrisisType
  situationDescription: string
  filteredMyData?: Record<string, unknown>
}

export interface TodoItem {
  dayRange: string
  action: string
  deadline: string
  priority: 'HIGH' | 'MED' | 'LOW'
  reason: string
}

export interface ReceivableItem {
  name: string
  estimatedMin: number
  estimatedMax: number
  source: string
  applyUrl: string
  deadline: string
  requiredDocs: string[]
}

export interface HoldableItem {
  name: string
  deferPeriod: string
  riskLevel: 'LOW' | 'MED' | 'HIGH'
  howTo: string
  caution: string
}

export interface ActionItem {
  name: string
  requiredDocs: string[]
  deadline: string
  contactInfo: string
  priority: 'HIGH' | 'MED' | 'LOW'
}

export interface AnalysisSummary {
  totalReceivableMin: number
  totalReceivableMax: number
  urgentCount: number
  thirtyDayPlan: string
}

export interface AnalysisResult {
  todos: TodoItem[]
  receivable: ReceivableItem[]
  holdable: HoldableItem[]
  actions: ActionItem[]
  summary: AnalysisSummary
  disclaimer: string
}

export interface AnalysisResultResponse {
  id: number
  crisisType: CrisisType
  situationDescription: string
  result: AnalysisResult
  llmProvider: string
  createdAt: string
}

/* ────────────────────────────────────────────────
   복지 (Welfare)
──────────────────────────────────────────────── */
export interface WelfareBenefitResponse {
  id: number
  externalServiceId: string
  serviceName: string
  summary: string
  targetDescription: string
  selectionCriteria: string
  applyMethod: string
  applyUrl: string
  ministryName: string
  contact: string | null
  crisisTags: CrisisType[]
  isActive: boolean
  lastSyncedAt: string
}
