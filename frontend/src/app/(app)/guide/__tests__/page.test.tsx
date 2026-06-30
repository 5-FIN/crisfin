import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { GuideResponse } from '@/lib/types'

// guideApi.get 모킹
const mockGet = vi.fn()
vi.mock('@/lib/api', () => ({
  guideApi: {
    get: (crisisType: string) => mockGet(crisisType),
  },
}))

import GuidePage from '@/app/(app)/guide/page'

const GUIDE: GuideResponse = {
  crisisType: 'HOSPITALIZATION',
  title: '입원·수술 대응 가이드',
  coachingPrompt: '나는 갑작스러운 입원으로...',
  keyRules: ['실손보험 청구 서류를 챙기세요', '진단서를 발급받으세요'],
  sourceLaws: ['국민건강보험법'],
  disclaimer: '본 정보는 참고용이며 법적 효력이 없습니다.',
}

beforeEach(() => {
  mockGet.mockReset()
  mockGet.mockResolvedValue(GUIDE)
})

describe('GuidePage', () => {
  it('위기 유형 선택 클릭 시 프롬프트/수칙/면책을 표시한다', async () => {
    const user = userEvent.setup()
    render(<GuidePage />)

    // 위기 선택 화면
    expect(screen.getByText('어떤 위기 상황인가요?')).toBeInTheDocument()

    // '입원/수술' 카드 클릭
    await user.click(screen.getByText('입원/수술'))

    expect(mockGet).toHaveBeenCalledWith('HOSPITALIZATION')

    // 가이드 결과: 제목/프롬프트/수칙/면책
    await waitFor(() => {
      expect(screen.getByText('입원·수술 대응 가이드')).toBeInTheDocument()
    })
    expect(screen.getByText('나는 갑작스러운 입원으로...')).toBeInTheDocument()
    expect(screen.getByText('실손보험 청구 서류를 챙기세요')).toBeInTheDocument()
    expect(screen.getByText('국민건강보험법')).toBeInTheDocument()
    expect(screen.getByText('본 정보는 참고용이며 법적 효력이 없습니다.')).toBeInTheDocument()
  })

  it('복사 버튼 클릭 시 navigator.clipboard.writeText가 coachingPrompt로 호출된다', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    // userEvent.setup()이 자체 clipboard 스텁을 설치하므로, 그 이후에 덮어쓴다.
    const user = userEvent.setup()
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
    })
    render(<GuidePage />)

    await user.click(screen.getByText('입원/수술'))
    await waitFor(() => {
      expect(screen.getByText('복사')).toBeInTheDocument()
    })

    await user.click(screen.getByText('복사'))

    expect(writeText).toHaveBeenCalledWith(GUIDE.coachingPrompt)
    // 복사 후 '복사됨' 상태 전환
    await waitFor(() => {
      expect(screen.getByText('복사됨 ✓')).toBeInTheDocument()
    })
  })

  it('API 실패 시 에러 메시지를 표시한다', async () => {
    mockGet.mockRejectedValue(new Error('서버 오류'))
    const user = userEvent.setup()
    render(<GuidePage />)

    await user.click(screen.getByText('실직/소득단절'))

    await waitFor(() => {
      expect(screen.getByText('서버 오류')).toBeInTheDocument()
    })
  })
})
