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
  regionCtpv?: string
  regionSgg?: string
}

export interface UserResponse {
  email: string
  nickname: string
  role: string
  regionCtpv?: string | null
  regionSgg?: string | null
}

export interface UpdateProfileRequest {
  nickname: string
  regionCtpv?: string
  regionSgg?: string
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
  recurring: boolean
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
/** rule 엔진 입력용 신청자 프로필 (모두 선택) */
export interface ApplicantProfile {
  householdSize?: number
  monthlyIncome?: number
  age?: number
  liquidFinancialAssets?: number
  employmentInsuranceMonths?: number
  involuntarySeparation?: boolean
  annualOutOfPocketMedical?: number
  careGrade?: number
}

export interface AnalysisRequest {
  crisisType: CrisisType
  situationDescription: string
  filteredMyData?: Record<string, unknown>
  applicantProfile?: ApplicantProfile
}

/** 개인화 재추론 요청 (생략 시 부모 분석에서 상속) */
export interface ReinferRequest {
  filteredMyData?: Record<string, unknown>
  applicantProfile?: ApplicantProfile
  situationDescription?: string
}

export interface TodoItem {
  dayRange: string
  action: string
  deadline: string
  priority: 'HIGH' | 'MED' | 'LOW'
  reason: string
}

export type ReceivableStatus = 'ELIGIBLE' | 'NEEDS_MORE_INPUT'

export interface ReceivableItem {
  name: string
  /** rule 엔진이 금액을 산정하지 못하면 null */
  estimatedMin: number | null
  estimatedMax: number | null
  status: ReceivableStatus
  /** 자격/금액 산정 근거 */
  basis: string
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

/** 30일 타임라인 항목 (백엔드가 긴급도순 정렬해 제공) */
export interface TimelineTask {
  title: string
  priority: 'HIGH' | 'MED' | 'LOW'
  dayRange: string
  urgencyScore: number
  category: string
}

/** 30일 버킷 단위 타임라인 구간 */
export interface TimelinePhase {
  range: string
  fromDay: number
  toDay: number
  items: TimelineTask[]
}

/** 추가 입력이 필요한 혜택 안내 */
export interface NeedsMoreInputItem {
  benefitName: string
  missingInputs: string[]
}

export interface AnalysisResult {
  todos: TodoItem[]
  receivable: ReceivableItem[]
  holdable: HoldableItem[]
  actions: ActionItem[]
  summary: AnalysisSummary
  /** 백엔드가 긴급도 정렬해 제공하는 30일 타임라인 */
  timeline?: TimelinePhase[]
  /** '추가입력 필요' 안내 목록 */
  needsMoreInput?: NeedsMoreInputItem[]
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
  active: boolean
  lastSyncedAt: string | null
  ctpvNm?: string | null
  sggNm?: string | null
}

/* ────────────────────────────────────────────────
   결제 / 이용권 (Payments)
──────────────────────────────────────────────── */
/** 요금제 카탈로그 항목 (GET /payments/plans) */
export interface PlanResponse {
  code: string
  name: string
  priceKrw: number
  durationDays: number
  /** null = 무제한 */
  uses: number | null
  tagline: string
  features: string[]
}

export interface CheckoutRequest {
  plan: string
}

export interface CheckoutResponse {
  orderUid: string
  amount: number
  plan?: string
}

export interface EntitlementResponse {
  active: boolean
  plan: string | null
  expiresAt?: string | null
  remainingUses?: number | null
}
