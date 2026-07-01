'use client'

import { useEffect, useMemo, useState } from 'react'
import {
  Wallet, CreditCard, Landmark, ShieldCheck, Repeat, Banknote,
  Loader2, type LucideIcon,
} from 'lucide-react'
import { myDataApi } from '@/lib/api'
import { fmt, summarizeMyData, myDataSelectionStore } from '@/lib/utils'
import type { PersonaType } from '@/lib/types'

/* ── 토글 가능한 마이데이터 항목 ── */
export const MYDATA_FIELDS = [
  { key: 'bankAccounts',  label: '계좌',     icon: Wallet },
  { key: 'cards',         label: '카드',     icon: CreditCard },
  { key: 'loans',         label: '대출',     icon: Landmark },
  { key: 'insurance',     label: '보험',     icon: ShieldCheck },
  { key: 'autoTransfers', label: '자동이체', icon: Repeat },
  { key: 'income',        label: '소득',     icon: Banknote },
] as const

type FieldKey = (typeof MYDATA_FIELDS)[number]['key']

/** '소득'은 monthlyIncome/incomeType 두 키로 매핑됨 */
const FIELD_TO_DATA_KEYS: Record<FieldKey, string[]> = {
  bankAccounts:  ['bankAccounts'],
  cards:         ['cards'],
  loans:         ['loans'],
  insurance:     ['insurance'],
  autoTransfers: ['autoTransfers'],
  income:        ['monthlyIncome', 'incomeType'],
}

type Obj = Record<string, unknown>

interface Props {
  persona: PersonaType
  /** 최종 페이로드(켜진 항목 + 수정값)를 부모에 올려보냄 */
  onChange: (filteredMyData: Obj) => void
}

/* ── 작은 스위치 ── */
function Switch({ on, onToggle }: { on: boolean; onToggle: () => void }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-pressed={on}
      className={`relative w-11 h-6 rounded-full transition-colors flex-shrink-0
        ${on ? 'bg-[#2563EB]' : 'bg-[#E2E8F0]'}`}
    >
      <span
        className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform
          ${on ? 'translate-x-5' : 'translate-x-0'}`}
      />
    </button>
  )
}

/* ── 수정 가능한 숫자 입력 ── */
function NumInput({
  label, value, onChange,
}: { label: string; value: number; onChange: (n: number) => void }) {
  return (
    <label className="flex items-center justify-between gap-3 text-sm">
      <span className="text-[#64748B]">{label}</span>
      <input
        type="number"
        value={Number.isFinite(value) ? value : 0}
        onChange={e => onChange(Number(e.target.value) || 0)}
        className="w-32 px-2.5 py-1.5 rounded-lg border border-[#E2E8F0] text-right text-[#1E293B]
          focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition"
      />
    </label>
  )
}

function SummaryCard({ label, value, icon: Icon }: { label: string; value: string; icon: LucideIcon }) {
  return (
    <div className="rounded-xl border border-[#E2E8F0] bg-[#F8FAFC] p-4">
      <div className="flex items-center gap-2 text-[#64748B] mb-1.5">
        <Icon size={15} />
        <span className="text-xs font-medium">{label}</span>
      </div>
      <div className="text-lg font-bold text-[#1E293B]">{value}</div>
    </div>
  )
}

export default function MyDataSelector({ persona, onChange }: Props) {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [data, setData] = useState<Obj>({})
  // 기본값(전부 ON)에 저장된 선택 상태를 덮어써서 복원. 새 필드는 기본 ON 유지.
  const [enabled, setEnabled] = useState<Record<FieldKey, boolean>>(() => {
    const defaults = Object.fromEntries(
      MYDATA_FIELDS.map(f => [f.key, true]),
    ) as Record<FieldKey, boolean>
    const saved = myDataSelectionStore.load()
    return saved ? { ...defaults, ...saved } : defaults
  })

  /* 페르소나 변경 시 실제 목 데이터 로드 */
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    myDataApi.mock(persona)
      .then(res => { if (!cancelled) setData(res ?? {}) })
      .catch(() => { if (!cancelled) setError('재정 데이터를 불러오지 못했습니다.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [persona])

  const toggle = (k: FieldKey) => setEnabled(p => {
    const next = { ...p, [k]: !p[k] }
    myDataSelectionStore.save(next)   // 선택 상태 영구 저장
    return next
  })

  /* ── 수정 가능한 핵심 수치들을 배열 단위로 로컬 보관 ── */
  const editValue = (arrKey: string, idx: number, field: string, n: number) => {
    setData(prev => {
      const arr = Array.isArray(prev[arrKey]) ? [...(prev[arrKey] as Obj[])] : []
      if (!arr[idx]) return prev
      arr[idx] = { ...arr[idx], [field]: n }
      return { ...prev, [arrKey]: arr }
    })
  }
  const editScalar = (key: string, n: number) =>
    setData(prev => ({ ...prev, [key]: n }))

  /* ── 켜진 항목 + 수정값만 모은 최종 페이로드 ── */
  const payload = useMemo<Obj>(() => {
    const out: Obj = {}
    for (const f of MYDATA_FIELDS) {
      if (!enabled[f.key]) continue
      for (const dk of FIELD_TO_DATA_KEYS[f.key]) {
        if (data[dk] !== undefined) out[dk] = data[dk]
      }
    }
    // 공공 마이데이터(위기별 필수값)는 토글 대상이 아니라 항상 포함 —
    // 진단 폼이 손입력 대신 이 값으로 applicantProfile을 채운다.
    if (data.publicData !== undefined) out.publicData = data.publicData
    return out
  }, [enabled, data])

  useEffect(() => { onChange(payload) }, [payload, onChange])

  const summary = useMemo(() => summarizeMyData(payload), [payload])

  const arr = (k: string): Obj[] => (Array.isArray(data[k]) ? (data[k] as Obj[]) : [])
  const str = (v: unknown) => (typeof v === 'string' ? v : '')
  const num = (v: unknown) => (typeof v === 'number' ? v : 0)

  if (loading) {
    return (
      <div className="flex items-center justify-center gap-2 py-12 text-[#64748B]">
        <Loader2 size={18} className="animate-spin text-[#2563EB]" />
        <span className="text-sm">재정 데이터를 불러오는 중...</span>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {error && (
        <div className="px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-600">{error}</div>
      )}

      {/* 상황 요약 */}
      <div>
        <h3 className="text-sm font-semibold text-[#1E293B] mb-3">상황 요약</h3>
        <div className="grid grid-cols-2 gap-3">
          <SummaryCard label="고정지출(월)" value={fmt(summary.fixedExpense)} icon={Repeat} />
          <SummaryCard label="임박 결제" value={fmt(summary.upcomingPayment)} icon={CreditCard} />
          <SummaryCard
            label="부채 (잔금 / 월상환)"
            value={`${fmt(summary.debtOutstanding)} / ${fmt(summary.debtMonthlyPayment)}`}
            icon={Landmark}
          />
          <SummaryCard label="보험료(월)" value={fmt(summary.insurancePremium)} icon={ShieldCheck} />
        </div>
      </div>

      {/* 항목 토글 + 미리보기/수정 */}
      <div>
        <h3 className="text-sm font-semibold text-[#1E293B] mb-1">분석에 포함할 항목</h3>
        <p className="text-xs text-[#64748B] mb-3">켜진 항목의 핵심 수치는 직접 수정할 수 있습니다.</p>
        <div className="space-y-3">
          {MYDATA_FIELDS.map(({ key, label, icon: Icon }) => {
            const on = enabled[key]
            return (
              <div
                key={key}
                className={`rounded-xl border transition-colors
                  ${on ? 'border-[#DBEAFE] bg-[#EFF6FF]' : 'border-[#E2E8F0] bg-white'}`}
              >
                <div className="flex items-center justify-between p-4">
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-lg flex items-center justify-center
                      ${on ? 'bg-[#2563EB] text-white' : 'bg-[#F8FAFC] text-[#94A3B8]'}`}>
                      <Icon size={18} />
                    </div>
                    <span className={`text-sm font-medium ${on ? 'text-[#1E293B]' : 'text-[#94A3B8]'}`}>{label}</span>
                  </div>
                  <Switch on={on} onToggle={() => toggle(key)} />
                </div>

                {/* 미리보기 / 수정 영역 */}
                {on && (
                  <div className="px-4 pb-4 space-y-2.5 border-t border-[#DBEAFE] pt-3">
                    {key === 'bankAccounts' && (
                      arr('bankAccounts').length === 0
                        ? <p className="text-xs text-[#94A3B8]">데이터 없음</p>
                        : arr('bankAccounts').map((a, i) => (
                            <div key={i} className="flex items-center justify-between text-sm text-[#475569]">
                              <span>{str(a.bank)} · {str(a.accountType)}</span>
                              <span className="font-medium text-[#1E293B]">{fmt(num(a.balance))}</span>
                            </div>
                          ))
                    )}

                    {key === 'cards' && (
                      arr('cards').length === 0
                        ? <p className="text-xs text-[#94A3B8]">데이터 없음</p>
                        : arr('cards').map((c, i) => (
                            <NumInput
                              key={i}
                              label={`${str(c.issuer)} (이번달 결제예정)`}
                              value={num(c.monthlyUsage)}
                              onChange={n => editValue('cards', i, 'monthlyUsage', n)}
                            />
                          ))
                    )}

                    {key === 'loans' && (
                      arr('loans').length === 0
                        ? <p className="text-xs text-[#94A3B8]">데이터 없음</p>
                        : arr('loans').map((l, i) => (
                            <div key={i} className="space-y-2">
                              <div className="text-xs font-medium text-[#64748B]">{str(l.lender)} · {str(l.loanType)}</div>
                              <NumInput
                                label="잔금"
                                value={num(l.outstandingBalance)}
                                onChange={n => editValue('loans', i, 'outstandingBalance', n)}
                              />
                              <NumInput
                                label="월 상환액"
                                value={num(l.monthlyPayment)}
                                onChange={n => editValue('loans', i, 'monthlyPayment', n)}
                              />
                            </div>
                          ))
                    )}

                    {key === 'insurance' && (
                      arr('insurance').length === 0
                        ? <p className="text-xs text-[#94A3B8]">데이터 없음</p>
                        : arr('insurance').map((ins, i) => (
                            <NumInput
                              key={i}
                              label={`${str(ins.productName)} (월보험료)`}
                              value={num(ins.monthlyPremium)}
                              onChange={n => editValue('insurance', i, 'monthlyPremium', n)}
                            />
                          ))
                    )}

                    {key === 'autoTransfers' && (
                      arr('autoTransfers').length === 0
                        ? <p className="text-xs text-[#94A3B8]">데이터 없음</p>
                        : arr('autoTransfers').map((t, i) => (
                            <NumInput
                              key={i}
                              label={str(t.description)}
                              value={num(t.amount)}
                              onChange={n => editValue('autoTransfers', i, 'amount', n)}
                            />
                          ))
                    )}

                    {key === 'income' && (
                      <NumInput
                        label="월 소득"
                        value={num(data.monthlyIncome)}
                        onChange={n => editScalar('monthlyIncome', n)}
                      />
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
