import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { analysisStore } from '@/lib/utils'
import type { AnalysisResultResponse } from '@/lib/types'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import DashboardPage from '@/app/(app)/dashboard/page'

const ANALYSIS: AnalysisResultResponse = {
  id: 1,
  crisisType: 'HOSPITALIZATION',
  situationDescription: '입원',
  llmProvider: 'mock',
  createdAt: '2026-06-30T00:00:00Z',
  result: {
    todos: [
      { dayRange: 'D+1~3', action: '진단서 발급', deadline: '3일 내', priority: 'HIGH', reason: 'r' },
      { dayRange: 'D+4~7', action: '실손 청구', deadline: '7일 내', priority: 'MED', reason: 'r' },
    ],
    receivable: [
      { name: '실손의료비', estimatedMin: 1_000_000, estimatedMax: 2_000_000, status: 'ELIGIBLE', basis: 'b', source: 'A보험', applyUrl: '', deadline: 'd', requiredDocs: [] },
    ],
    holdable: [
      { name: '카드대금', deferPeriod: '3개월', riskLevel: 'LOW', howTo: 'h', caution: 'c' },
    ],
    actions: [
      { name: '서류 제출', requiredDocs: [], deadline: 'd', contactInfo: 'c', priority: 'HIGH' },
    ],
    summary: {
      totalReceivableMin: 1_000_000,
      totalReceivableMax: 2_000_000,
      urgentCount: 3,
      thirtyDayPlan: '30일 안에 보험금을 청구하세요',
    },
    disclaimer: '참고용 정보입니다',
  },
}

beforeEach(() => {
  localStorage.clear()
  mockPush.mockReset()
})

describe('DashboardPage', () => {
  it('분석 결과가 없으면 /diagnosis로 리다이렉트한다', async () => {
    render(<DashboardPage />)
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/diagnosis')
    })
  })

  it('분석 결과 시드 시 핵심 항목(받을돈/할일/요약)을 렌더한다', async () => {
    analysisStore.save(ANALYSIS)
    render(<DashboardPage />)

    // 헤더 + 위기 라벨
    await waitFor(() => {
      expect(screen.getByText('내 금융 위기 대응 현황')).toBeInTheDocument()
    })
    expect(screen.getByText('입원/수술 대응 중')).toBeInTheDocument()

    // 30일 플랜 요약
    expect(screen.getByText('30일 안에 보험금을 청구하세요')).toBeInTheDocument()

    // 4분면: 할 일 건수, 받을 돈
    expect(screen.getByText('할 일')).toBeInTheDocument()
    expect(screen.getByText('받을 돈')).toBeInTheDocument()

    // 긴급 할 일 미리보기 + 받을 돈 미리보기 항목
    expect(screen.getByText('진단서 발급')).toBeInTheDocument()
    expect(screen.getByText('실손의료비')).toBeInTheDocument()

    // 면책 조항
    expect(screen.getByText('참고용 정보입니다')).toBeInTheDocument()

    expect(mockPush).not.toHaveBeenCalled()
  })
})
