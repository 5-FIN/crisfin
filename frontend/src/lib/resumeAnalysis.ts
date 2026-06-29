import { analysisApi } from '@/lib/api'
import { analysisStore, pendingAnalysisStore } from '@/lib/utils'
import type { AnalysisRequest } from '@/lib/types'

/**
 * 보류된 분석 요청(pendingAnalysisStore)을 다시 실행한다.
 * - 성공: 결과 저장 + pending 제거 → /dashboard 이동
 * - 402(미결제): pending 유지 → /unlock 이동
 * - 401(미로그인): pending 유지 → /login 이동
 * 보류 요청이 없으면 아무것도 하지 않고 false 반환(호출부가 기본 라우팅).
 */
export async function resumePendingAnalysis(
  push: (path: string) => void,
): Promise<boolean> {
  const pending = pendingAnalysisStore.load() as AnalysisRequest | null
  if (!pending) return false

  try {
    const result = await analysisApi.recommend(pending)
    analysisStore.save(result)
    pendingAnalysisStore.clear()
    push('/dashboard')
    return true
  } catch (err) {
    const status = (err as { status?: number }).status
    if (status === 402) { push('/unlock'); return true }
    if (status === 401) { push('/login'); return true }
    // 그 외 오류: 보류 요청을 유지한 채 진단 화면으로
    return false
  }
}
