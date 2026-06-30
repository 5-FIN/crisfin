import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { analysisStore, taskStore } from '@/lib/utils'
import type { AnalysisResultResponse } from '@/lib/types'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import TasksPage from '@/app/(app)/tasks/page'

const ANALYSIS: AnalysisResultResponse = {
  id: 5,
  crisisType: 'UNEMPLOYMENT',
  situationDescription: '실직',
  llmProvider: 'mock',
  createdAt: '2026-06-30T00:00:00Z',
  result: {
    todos: [],
    receivable: [],
    holdable: [],
    actions: [],
    summary: { totalReceivableMin: 0, totalReceivableMax: 0, urgentCount: 0, thirtyDayPlan: '' },
    timeline: [
      {
        range: 'D+1~7',
        fromDay: 1,
        toDay: 7,
        items: [
          { title: '실업급여 신청', priority: 'HIGH', dayRange: 'D+1~3', urgencyScore: 90, category: '고용' },
        ],
      },
      {
        range: 'D+8~14',
        fromDay: 8,
        toDay: 14,
        items: [
          { title: '국민연금 납부예외 신청', priority: 'MED', dayRange: 'D+8~10', urgencyScore: 60, category: '연금' },
        ],
      },
    ],
    disclaimer: 'd',
  },
}

beforeEach(() => {
  localStorage.clear()
  mockPush.mockReset()
})

describe('TasksPage', () => {
  it('분석 결과가 없으면 /diagnosis로 리다이렉트한다', async () => {
    render(<TasksPage />)
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/diagnosis')
    })
  })

  it('타임라인 시드 시 그룹/항목과 진행률을 렌더한다', async () => {
    analysisStore.save(ANALYSIS)
    render(<TasksPage />)

    await waitFor(() => {
      expect(screen.getByText('액션 체크리스트')).toBeInTheDocument()
    })

    // 타임라인 항목
    expect(screen.getByText('실업급여 신청')).toBeInTheDocument()
    expect(screen.getByText('국민연금 납부예외 신청')).toBeInTheDocument()

    // 구간 헤더
    expect(screen.getByText('D+1~7')).toBeInTheDocument()
    expect(screen.getByText('D+8~14')).toBeInTheDocument()

    // 초기 진행률 0%
    expect(screen.getByText('0%')).toBeInTheDocument()
  })

  it('항목 클릭 시 완료 토글되어 taskStore에 반영된다', async () => {
    analysisStore.save(ANALYSIS)
    const user = userEvent.setup()
    render(<TasksPage />)

    await waitFor(() => {
      expect(screen.getByText('실업급여 신청')).toBeInTheDocument()
    })

    await user.click(screen.getByText('실업급여 신청'))

    // 첫 항목 완료 → 2개 중 1개 = 50%
    await waitFor(() => {
      expect(screen.getByText('50%')).toBeInTheDocument()
    })
    // taskStore 영속화 확인 (key는 p0-0)
    expect(taskStore.load(5)['p0-0']).toBe(true)
  })

  it('타임라인이 비면 빈 상태 안내를 표시한다', async () => {
    analysisStore.save({ ...ANALYSIS, result: { ...ANALYSIS.result, timeline: [] } })
    render(<TasksPage />)

    await waitFor(() => {
      expect(screen.getByText('타임라인 항목이 없습니다.')).toBeInTheDocument()
    })
  })
})
