import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { analysisStore } from '@/lib/utils'
import type { AnalysisResultResponse } from '@/lib/types'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import PaymentsPage from '@/app/(app)/payments/page'

const ANALYSIS: AnalysisResultResponse = {
  id: 9,
  crisisType: 'ACCIDENT',
  situationDescription: '사고',
  llmProvider: 'mock',
  createdAt: '2026-06-30T00:00:00Z',
  result: {
    todos: [
      { dayRange: 'D+1~3', action: '병원비 정산', deadline: '3일 내', priority: 'HIGH', reason: '연체 방지' },
      { dayRange: 'D+5~7', action: '경미 처리', deadline: '7일 내', priority: 'LOW', reason: 'r' },
    ],
    receivable: [],
    holdable: [
      { name: '카드 할부금', deferPeriod: '3개월', riskLevel: 'LOW', howTo: '콜센터 문의', caution: '이자 발생' },
    ],
    actions: [],
    summary: { totalReceivableMin: 0, totalReceivableMax: 0, urgentCount: 1, thirtyDayPlan: '' },
    disclaimer: 'd',
  },
}

beforeEach(() => {
  localStorage.clear()
  mockPush.mockReset()
})

describe('PaymentsPage', () => {
  it('분석 결과가 없으면 /diagnosis로 리다이렉트한다', async () => {
    render(<PaymentsPage />)
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/diagnosis')
    })
  })

  it('시드 시 긴급 처리 탭(기본)에 긴급 todo를 렌더한다', async () => {
    analysisStore.save(ANALYSIS)
    render(<PaymentsPage />)

    await waitFor(() => {
      expect(screen.getByText('납부 관리')).toBeInTheDocument()
    })

    // 기본 탭: 긴급 처리 (HIGH todo만)
    expect(screen.getByText('병원비 정산')).toBeInTheDocument()
    expect(screen.getByText('연체 방지')).toBeInTheDocument()
    // LOW todo는 긴급 탭에 표시되지 않음
    expect(screen.queryByText('경미 처리')).not.toBeInTheDocument()
  })

  it('유예 가능 탭 클릭 시 holdable 항목을 렌더한다', async () => {
    analysisStore.save(ANALYSIS)
    const user = userEvent.setup()
    render(<PaymentsPage />)

    await waitFor(() => {
      expect(screen.getByText('미룰 수 있는 것')).toBeInTheDocument()
    })

    await user.click(screen.getByText('미룰 수 있는 것'))

    await waitFor(() => {
      expect(screen.getByText('카드 할부금')).toBeInTheDocument()
    })
    expect(screen.getByText('유예 기간: 3개월')).toBeInTheDocument()
  })
})
