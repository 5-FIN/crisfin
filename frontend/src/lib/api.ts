import { tokenStore } from '@/lib/utils'
import type {
  ApiResponse, PageResponse,
  AuthResponse, SignupRequest, LoginRequest,
  CrisisTypeResponse, GuideResponse,
  PersonaType, CrisisType,
  AnalysisRequest, AnalysisResultResponse,
  WelfareBenefitResponse,
} from '@/lib/types'

/* ── 기본 fetch 래퍼 ── */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = tokenStore.getAccess()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(init?.headers as Record<string, string> | undefined),
  }

  const res = await fetch(path, { ...init, headers })
  const body: ApiResponse<T> = await res.json()

  if (!body.success) {
    throw new Error(body.error?.message ?? '요청 중 오류가 발생했습니다.')
  }
  return body.data
}

/* ── Auth ── */
export const authApi = {
  signup: (data: SignupRequest) =>
    request<AuthResponse>('/api/v1/auth/signup', {
      method: 'POST', body: JSON.stringify(data),
    }),
  login: (data: LoginRequest) =>
    request<AuthResponse>('/api/v1/auth/login', {
      method: 'POST', body: JSON.stringify(data),
    }),
  refresh: (refreshToken: string) =>
    request<AuthResponse>('/api/v1/auth/refresh', {
      method: 'POST', body: JSON.stringify({ refreshToken }),
    }),
  logout: (refreshToken: string) =>
    request<void>('/api/v1/auth/logout', {
      method: 'POST', body: JSON.stringify({ refreshToken }),
    }),
}

/* ── Crisis ── */
export const crisisApi = {
  types: () => request<CrisisTypeResponse[]>('/api/v1/crisis/types'),
}

/* ── Guide ── */
export const guideApi = {
  get: (crisisType: CrisisType) =>
    request<GuideResponse>(`/api/v1/guide/${crisisType}`),
}

/* ── MyData ── */
export const myDataApi = {
  mock: (persona: PersonaType) =>
    request<Record<string, unknown>>(`/api/v1/mydata/mock?persona=${persona}`),
  filter: (persona: PersonaType, selectedFields: string[]) =>
    request<Record<string, unknown>>('/api/v1/mydata/filter', {
      method: 'POST', body: JSON.stringify({ persona, selectedFields }),
    }),
}

/* ── Analysis ── */
export const analysisApi = {
  recommend: (data: AnalysisRequest) =>
    request<AnalysisResultResponse>('/api/v1/analysis/recommend', {
      method: 'POST', body: JSON.stringify(data),
    }),
  getResult: (id: number) =>
    request<AnalysisResultResponse>(`/api/v1/analysis/results/${id}`),
}

/* ── Welfare ── */
export const welfareApi = {
  list: (params?: { crisisType?: CrisisType; page?: number; size?: number }) => {
    const qs = new URLSearchParams()
    if (params?.crisisType) qs.set('crisisType', params.crisisType)
    if (params?.page != null) qs.set('page', String(params.page))
    if (params?.size != null) qs.set('size', String(params.size))
    return request<PageResponse<WelfareBenefitResponse>>(
      `/api/v1/welfare/benefits?${qs.toString()}`,
    )
  },
  get: (id: number) =>
    request<WelfareBenefitResponse>(`/api/v1/welfare/benefits/${id}`),
}
