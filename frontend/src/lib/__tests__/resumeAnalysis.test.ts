import { describe, it, expect, vi, beforeEach } from 'vitest'
import { analysisStore, pendingAnalysisStore } from '@/lib/utils'

// analysisApi.recommend 모킹
const mockRecommend = vi.fn()
vi.mock('@/lib/api', () => ({
  analysisApi: {
    recommend: (...args: unknown[]) => mockRecommend(...args),
  },
}))

import { resumePendingAnalysis } from '@/lib/resumeAnalysis'

const PENDING = {
  crisisType: 'HOSPITALIZATION',
  situationDescription: '입원으로 소득 단절',
}

const RESULT = {
  id: 99,
  crisisType: 'HOSPITALIZATION',
  result: { summary: { totalReceivableMin: 1 } },
}

beforeEach(() => {
  localStorage.clear()
  mockRecommend.mockReset()
})

describe('resumePendingAnalysis', () => {
  it('보류 요청이 없으면 false를 반환하고 push를 호출하지 않는다', async () => {
    const push = vi.fn()
    const result = await resumePendingAnalysis(push)
    expect(result).toBe(false)
    expect(push).not.toHaveBeenCalled()
    expect(mockRecommend).not.toHaveBeenCalled()
  })

  it('성공 시 결과를 저장하고 pending을 비운 뒤 /dashboard로 이동, true 반환', async () => {
    pendingAnalysisStore.save(PENDING)
    mockRecommend.mockResolvedValue(RESULT)
    const push = vi.fn()

    const result = await resumePendingAnalysis(push)

    expect(mockRecommend).toHaveBeenCalledWith(PENDING)
    expect(analysisStore.load()).toEqual(RESULT)
    expect(pendingAnalysisStore.load()).toBeNull()
    expect(push).toHaveBeenCalledWith('/dashboard')
    expect(result).toBe(true)
  })

  it('402(미결제) 에러 시 /unlock으로 이동, true 반환, pending 유지', async () => {
    pendingAnalysisStore.save(PENDING)
    mockRecommend.mockRejectedValue(Object.assign(new Error('payment'), { status: 402 }))
    const push = vi.fn()

    const result = await resumePendingAnalysis(push)

    expect(push).toHaveBeenCalledWith('/unlock')
    expect(result).toBe(true)
    expect(pendingAnalysisStore.load()).toEqual(PENDING) // 유지
  })

  it('401(미로그인) 에러 시 /login으로 이동, true 반환', async () => {
    pendingAnalysisStore.save(PENDING)
    mockRecommend.mockRejectedValue(Object.assign(new Error('auth'), { status: 401 }))
    const push = vi.fn()

    const result = await resumePendingAnalysis(push)

    expect(push).toHaveBeenCalledWith('/login')
    expect(result).toBe(true)
    expect(pendingAnalysisStore.load()).toEqual(PENDING) // 유지
  })

  it('기타 에러 시 false를 반환하고 push를 호출하지 않으며 pending을 유지한다', async () => {
    pendingAnalysisStore.save(PENDING)
    mockRecommend.mockRejectedValue(Object.assign(new Error('server'), { status: 500 }))
    const push = vi.fn()

    const result = await resumePendingAnalysis(push)

    expect(result).toBe(false)
    expect(push).not.toHaveBeenCalled()
    expect(pendingAnalysisStore.load()).toEqual(PENDING)
  })

  it('status가 없는 에러도 기타 에러로 처리한다', async () => {
    pendingAnalysisStore.save(PENDING)
    mockRecommend.mockRejectedValue(new Error('network'))
    const push = vi.fn()

    const result = await resumePendingAnalysis(push)

    expect(result).toBe(false)
    expect(push).not.toHaveBeenCalled()
  })
})
