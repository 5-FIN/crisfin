import { tokenStore } from '@/lib/utils'
import type {
  ApiResponse, PageResponse,
  AuthResponse, SignupRequest, LoginRequest,
  CrisisTypeResponse, GuideResponse,
  PersonaType, CrisisType,
  AnalysisRequest, AnalysisResultResponse,
  WelfareBenefitResponse,
} from '@/lib/types'

/* ── 저수준 fetch (토큰 갱신 없음) ── */
async function rawFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = tokenStore.getAccess()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(init?.headers as Record<string, string> | undefined),
  }

  const res = await fetch(path, { ...init, headers })
  const body: ApiResponse<T> = await res.json()

  if (!body.success) {
    const err = Object.assign(
      new Error(body.error?.message ?? '요청 중 오류가 발생했습니다.'),
      { status: res.status },
    )
    throw err
  }
  return body.data
}

/* ── 토큰 자동 갱신 포함 fetch ── */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  try {
    return await rawFetch<T>(path, init)
  } catch (err: unknown) {
    if (err instanceof Error && (err as Error & { status?: number }).status === 401) {
      const refreshToken = tokenStore.getRefresh()
      if (!refreshToken) {
        tokenStore.clear()
        if (typeof window !== 'undefined') window.location.href = '/'
        throw new Error('로그인이 필요합니다.')
      }
      try {
        const newAuth = await rawFetch<AuthResponse>('/api/v1/auth/refresh', {
          method: 'POST',
          body: JSON.stringify({ refreshToken }),
        })
        tokenStore.set(newAuth.accessToken, newAuth.refreshToken)
        return await rawFetch<T>(path, init)
      } catch {
        tokenStore.clear()
        if (typeof window !== 'undefined') window.location.href = '/'
        throw new Error('세션이 만료되었습니다. 다시 로그인해주세요.')
      }
    }
    throw err
  }
}

/* ── Auth ── */
export const authApi = {
  signup: (data: SignupRequest) =>
    rawFetch<AuthResponse>('/api/v1/auth/signup', {
      method: 'POST', body: JSON.stringify(data),
    }),
  login: (data: LoginRequest) =>
    rawFetch<AuthResponse>('/api/v1/auth/login', {
      method: 'POST', body: JSON.stringify(data),
    }),
  refresh: (refreshToken: string) =>
    rawFetch<AuthResponse>('/api/v1/auth/refresh', {
      method: 'POST', body: JSON.stringify({ refreshToken }),
    }),
  logout: (refreshToken: string) =>
    rawFetch<void>('/api/v1/auth/logout', {
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
  history: (page = 0, size = 20) =>
    request<PageResponse<AnalysisResultResponse>>(
      `/api/v1/analysis/history?page=${page}&size=${size}`,
    ),
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
