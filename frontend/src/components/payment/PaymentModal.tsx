'use client'

import { useEffect, useState } from 'react'
import { X, Check, Loader2 } from 'lucide-react'
import { paymentApi } from '@/lib/api'
import { fmt } from '@/lib/utils'
import type { PlanResponse } from '@/lib/types'

const RECOMMENDED = 'UNLIMITED_30D'

/**
 * 결제 모달 — 유료 게이팅(402) 시 페이지 이동 없이 현재 화면 위에 떠서 결제한다.
 * 결제 성공 시 onPaid()로 호출부(예: 진단 화면)가 방금 입력한 분석을 제자리에서
 * 이어 실행한다. "요금제 창에서 결제하고 원래 자리로 돌아가는" UX.
 */
export default function PaymentModal({
  onPaid,
  onClose,
}: {
  onPaid: () => void
  onClose: () => void
}) {
  const [plans, setPlans] = useState<PlanResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [busyPlan, setBusyPlan] = useState<string | null>(null)
  const [phase, setPhase] = useState<'checkout' | 'confirm' | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    paymentApi.plans()
      .then(setPlans)
      .catch(() => setError('요금제를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  async function pay(code: string) {
    setError('')
    setBusyPlan(code)
    try {
      setPhase('checkout')
      const { orderUid } = await paymentApi.checkout(code)
      setPhase('confirm')
      await paymentApi.confirm(orderUid)
      onPaid()
    } catch (err) {
      setError(err instanceof Error ? err.message : '결제 처리 중 오류가 발생했습니다.')
      setBusyPlan(null)
      setPhase(null)
    }
  }

  const busyLabel = phase === 'checkout' ? '결제 요청 중...' : phase === 'confirm' ? '이용권 활성화 중...' : ''

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40" onClick={busyPlan ? undefined : onClose} />
      <div className="relative bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        {/* 헤더 */}
        <div className="sticky top-0 bg-white border-b border-[#E2E8F0] px-6 py-4 flex items-start justify-between">
          <div>
            <div className="font-bold text-[#1E293B]">AI 맞춤 분석 시작하기</div>
            <div className="text-xs text-[#64748B] mt-0.5">결제하면 방금 입력한 내용으로 바로 이어서 분석합니다.</div>
          </div>
          <button onClick={onClose} disabled={!!busyPlan}
            className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#94A3B8] disabled:opacity-50 flex-shrink-0">
            <X size={18} />
          </button>
        </div>

        <div className="p-6 space-y-3">
          {error && (
            <div className="px-3.5 py-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600">{error}</div>
          )}

          {loading ? (
            <div className="flex justify-center py-10"><Loader2 size={24} className="animate-spin text-[#2563EB]" /></div>
          ) : (
            plans.map(p => {
              const rec = p.code === RECOMMENDED
              const busy = busyPlan === p.code
              return (
                <div key={p.code} className={`rounded-xl border p-4 ${rec ? 'border-[#2563EB]' : 'border-[#E2E8F0]'}`}>
                  <div className="flex items-center justify-between mb-1 gap-2">
                    <span className="font-bold text-[#1E293B]">
                      {p.name}
                      {rec && <span className="ml-2 text-[11px] text-[#2563EB] bg-[#EFF6FF] px-2 py-0.5 rounded-full">추천</span>}
                    </span>
                    <span className="text-lg font-bold tabular-nums text-[#1E293B] whitespace-nowrap">
                      {fmt(p.priceKrw)}
                      <span className="text-xs text-[#94A3B8] font-normal"> / {p.uses == null ? `${p.durationDays}일` : `${p.uses}회`}</span>
                    </span>
                  </div>
                  <div className="text-xs text-[#94A3B8] mb-3">{p.tagline}</div>
                  <ul className="space-y-1.5 mb-3">
                    {p.features.map(f => (
                      <li key={f} className="flex items-start gap-2 text-sm text-[#475569]">
                        <Check size={14} className="text-[#10B981] mt-0.5 flex-shrink-0" />{f}
                      </li>
                    ))}
                  </ul>
                  <button onClick={() => pay(p.code)} disabled={!!busyPlan}
                    className="w-full py-2.5 bg-[#2563EB] text-white font-semibold rounded-lg hover:bg-[#1D4ED8] disabled:opacity-60 disabled:cursor-not-allowed text-sm flex items-center justify-center gap-2">
                    {busy ? <><Loader2 size={15} className="animate-spin" />{busyLabel}</> : '결제하고 이어서 분석'}
                  </button>
                </div>
              )
            })
          )}

          <div className="text-center text-[11px] text-[#94A3B8] pt-1">
            테스트 결제(mock) · 결제 즉시 이용권이 활성화됩니다
          </div>
        </div>
      </div>
    </div>
  )
}
