import { describe, it, expect } from 'vitest'
import { summarizeMyData, fmt } from '@/lib/utils'

describe('summarizeMyData', () => {
  it('빈 입력(null/undefined/{})은 모두 0을 반환한다', () => {
    const expected = {
      fixedExpense: 0,
      upcomingPayment: 0,
      debtOutstanding: 0,
      debtMonthlyPayment: 0,
      insurancePremium: 0,
    }
    expect(summarizeMyData(null)).toEqual(expected)
    expect(summarizeMyData(undefined)).toEqual(expected)
    expect(summarizeMyData({})).toEqual(expected)
  })

  it('정상 데이터를 항목별로 합산한다', () => {
    const data = {
      autoTransfers: [
        { description: '월세', amount: 500_000 },
        { description: '통신비', amount: 80_000 },
      ],
      cards: [
        { issuer: 'A카드', monthlyUsage: 300_000 },
        { issuer: 'B카드', monthlyUsage: 150_000 },
      ],
      loans: [
        { lender: '은행', outstandingBalance: 10_000_000, monthlyPayment: 400_000 },
        { lender: '캐피탈', outstandingBalance: 5_000_000, monthlyPayment: 200_000 },
      ],
      insurance: [
        { productName: '실손', monthlyPremium: 50_000 },
        { productName: '종신', monthlyPremium: 120_000 },
      ],
    }
    expect(summarizeMyData(data)).toEqual({
      fixedExpense: 580_000,
      upcomingPayment: 450_000,
      debtOutstanding: 15_000_000,
      debtMonthlyPayment: 600_000,
      insurancePremium: 170_000,
    })
  })

  it('일부 항목 누락 시 해당 지표만 0으로 안전 처리한다', () => {
    const data = {
      autoTransfers: [{ description: '월세', amount: 500_000 }],
      // cards, loans, insurance 누락
    }
    expect(summarizeMyData(data)).toEqual({
      fixedExpense: 500_000,
      upcomingPayment: 0,
      debtOutstanding: 0,
      debtMonthlyPayment: 0,
      insurancePremium: 0,
    })
  })

  it('배열이 아닌 값이나 숫자가 아닌 필드는 무시한다', () => {
    const data = {
      autoTransfers: 'not-an-array',
      cards: [
        { issuer: 'A카드', monthlyUsage: 100_000 },
        { issuer: 'B카드', monthlyUsage: '200000' }, // 문자열 → 무시
        { issuer: 'C카드' }, // 필드 없음 → 무시
      ],
    }
    expect(summarizeMyData(data)).toEqual({
      fixedExpense: 0,
      upcomingPayment: 100_000,
      debtOutstanding: 0,
      debtMonthlyPayment: 0,
      insurancePremium: 0,
    })
  })
})

describe('fmt', () => {
  it('1만원 미만은 원 단위로 표시한다', () => {
    expect(fmt(0)).toBe('0원')
    expect(fmt(500)).toBe('500원')
    expect(fmt(9_999)).toBe('9,999원')
  })

  it('1만원 이상 1억원 미만은 만원 단위로 반올림 표시한다', () => {
    expect(fmt(10_000)).toBe('1만원')
    expect(fmt(15_000)).toBe('2만원') // 1.5 → 반올림 2
    expect(fmt(1_000_000)).toBe('100만원')
    expect(fmt(99_990_000)).toBe('9,999만원')
  })

  it('1억원 이상은 억 단위로 소수1자리 표시한다', () => {
    expect(fmt(100_000_000)).toBe('1.0억')
    expect(fmt(150_000_000)).toBe('1.5억')
    expect(fmt(1_234_500_000)).toBe('12.3억')
  })
})
