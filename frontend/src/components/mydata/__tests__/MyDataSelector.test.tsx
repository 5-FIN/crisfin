import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

// myDataApi.mock 모킹 — 샘플 persona 데이터 반환
const mockMock = vi.fn()
vi.mock('@/lib/api', () => ({
  myDataApi: {
    mock: (persona: string) => mockMock(persona),
  },
}))

import MyDataSelector, { MYDATA_FIELDS } from '@/components/mydata/MyDataSelector'
import { myDataSelectionStore } from '@/lib/utils'

const SAMPLE = {
  bankAccounts: [{ bank: '국민', accountType: '입출금', balance: 3_000_000 }],
  cards: [{ issuer: '신한카드', monthlyUsage: 400_000 }],
  loans: [{ lender: '우리은행', loanType: '신용', outstandingBalance: 10_000_000, monthlyPayment: 300_000 }],
  insurance: [{ productName: '실손보험', monthlyPremium: 50_000 }],
  autoTransfers: [{ description: '월세', amount: 600_000 }],
  monthlyIncome: 3_500_000,
  incomeType: 'SALARY',
}

beforeEach(() => {
  localStorage.clear()   // 선택 상태가 localStorage에 저장되므로 테스트 간 격리
  mockMock.mockReset()
  mockMock.mockResolvedValue(structuredClone(SAMPLE))
})

describe('MYDATA_FIELDS export', () => {
  it('6개 토글 항목을 노출한다', () => {
    expect(MYDATA_FIELDS).toHaveLength(6)
    expect(MYDATA_FIELDS.map(f => f.key)).toEqual([
      'bankAccounts', 'cards', 'loans', 'insurance', 'autoTransfers', 'income',
    ])
  })
})

describe('MyDataSelector', () => {
  it('로딩 후 페르소나 데이터를 불러와 항목과 상황요약을 렌더한다', async () => {
    render(<MyDataSelector persona="OFFICE_WORKER" onChange={() => {}} />)

    // 로딩 표시
    expect(screen.getByText('재정 데이터를 불러오는 중...')).toBeInTheDocument()

    // 로딩 종료 후 요약 + 항목 라벨 표시
    await waitFor(() => {
      expect(screen.getByText('상황 요약')).toBeInTheDocument()
    })
    expect(mockMock).toHaveBeenCalledWith('OFFICE_WORKER')
    expect(screen.getByText('고정지출(월)')).toBeInTheDocument()
    expect(screen.getByText('계좌')).toBeInTheDocument()
    expect(screen.getByText('카드')).toBeInTheDocument()

    // 고정지출 = autoTransfers 합(60만) → '60만원'
    expect(screen.getByText('60만원')).toBeInTheDocument()
  })

  it('초기 onChange 페이로드에 켜진 모든 항목의 데이터 키가 포함된다', async () => {
    const onChange = vi.fn()
    render(<MyDataSelector persona="OFFICE_WORKER" onChange={onChange} />)

    // 데이터 로드 후 payload 갱신을 기다린다(초기 빈 페이로드 호출 이후)
    await waitFor(() => {
      const last = onChange.mock.calls.at(-1)![0]
      expect(last).toHaveProperty('cards')
    })

    const last = onChange.mock.calls.at(-1)![0]
    expect(last).toHaveProperty('cards')
    expect(last).toHaveProperty('loans')
    expect(last).toHaveProperty('autoTransfers')
    expect(last).toHaveProperty('monthlyIncome', 3_500_000)
  })

  it('항목 토글 off 시 onChange 페이로드에서 해당 데이터 키가 빠진다', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<MyDataSelector persona="OFFICE_WORKER" onChange={onChange} />)

    await waitFor(() => {
      const last = onChange.mock.calls.at(-1)![0]
      expect(last).toHaveProperty('cards')
    })

    // '카드' 행의 스위치(aria-pressed)를 끈다
    const switches = screen.getAllByRole('button', { name: '' }).filter(
      b => b.getAttribute('aria-pressed') !== null,
    )
    // MYDATA_FIELDS 순서: 0=계좌 1=카드 ...
    await user.click(switches[1])

    await waitFor(() => {
      const last = onChange.mock.calls.at(-1)![0]
      expect(last).not.toHaveProperty('cards')
      // 다른 항목은 유지
      expect(last).toHaveProperty('loans')
    })
  })

  it('숫자 수정 시 상황요약이 갱신되고 onChange에 반영된다', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<MyDataSelector persona="OFFICE_WORKER" onChange={onChange} />)

    await waitFor(() => {
      expect(screen.getByText('상황 요약')).toBeInTheDocument()
    })

    // autoTransfers '월세' 입력(value=600,000)을 999999로 변경
    const wolse = screen.getByDisplayValue('600,000') as HTMLInputElement
    await user.clear(wolse)
    await user.type(wolse, '999999')

    await waitFor(() => {
      const last = onChange.mock.calls.at(-1)![0]
      const autoTransfers = last.autoTransfers as Array<{ amount: number }>
      expect(autoTransfers[0].amount).toBe(999999)
    })

    // 요약 고정지출이 100만원으로 갱신(999,999 → 반올림 100만)
    await waitFor(() => {
      expect(screen.getByText('100만원')).toBeInTheDocument()
    })
  })

  it('토글 off 상태가 localStorage에 저장되어 재마운트 시 복원된다', async () => {
    const user = userEvent.setup()
    const { unmount } = render(<MyDataSelector persona="OFFICE_WORKER" onChange={() => {}} />)

    await waitFor(() => {
      expect(screen.getByText('상황 요약')).toBeInTheDocument()
    })

    // 카드(인덱스 1) 토글 off → localStorage에 저장
    const switches = screen.getAllByRole('button', { name: '' }).filter(
      b => b.getAttribute('aria-pressed') !== null,
    )
    await user.click(switches[1])

    await waitFor(() => {
      expect(myDataSelectionStore.load()).toMatchObject({ cards: false })
    })

    // 재마운트 → 저장된 선택(카드 off)이 복원되어 payload에서 cards가 빠진다
    unmount()
    const onChange = vi.fn()
    render(<MyDataSelector persona="OFFICE_WORKER" onChange={onChange} />)

    await waitFor(() => {
      const last = onChange.mock.calls.at(-1)![0]
      expect(last).toHaveProperty('loans')   // 데이터 로드 완료 신호
    })
    const last = onChange.mock.calls.at(-1)![0]
    expect(last).not.toHaveProperty('cards')
  })
})
