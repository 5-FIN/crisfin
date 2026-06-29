'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Lock, Check, Loader2, ShieldCheck, ArrowRight } from 'lucide-react'
import { paymentApi } from '@/lib/api'
import { pendingAnalysisStore, fmt } from '@/lib/utils'
import { resumePendingAnalysis } from '@/lib/resumeAnalysis'

const PLAN_FEATURES = [
  'AI 위기 금융 분석 무제한',
  '받을 돈·미룰 것·할 일 맞춤 플랜',
  '자격·예상 수령액 자동 계산 (rule 엔진)',
  '30일 긴급도 타임라인',
]

export default function UnlockPage() {
  const router = useRouter()
  const [amount, setAmount] = useState<number | null>(null)
  const [phase, setPhase] = useState<'idle' | 'checkout' | 'confirm' | 'resume'>('idle')
  const [error, setError] = useState('')
  const hasPending = typeof window !== 'undefined' && !!pendingAnalysisStore.load()

  // 결제 금액 미리보기 (mock checkout)
  useEffect(() => {
    paymentApi.checkout()
      .then(res => setAmount(res.amount))
      .catch(() => {/* 금액 미리보기는 실패해도 무시 */})
  }, [])

  async function handlePay() {
    setError('')
    try {
      // 1) 결제 주문 생성 (mock)
      setPhase('checkout')
      const { orderUid } = await paymentApi.checkout()

      // 2) 결제 확인 → 이용권 즉시 활성화
      setPhase('confirm')
      await paymentApi.confirm(orderUid)

      // 3) 결제 후 즉시 분석 재실행 (보류 요청이 있을 때)
      setPhase('resume')
      const resumed = await resumePendingAnalysis(router.push)
      if (!resumed) router.push('/diagnosis')
    } catch (err) {
      setError(err instanceof Error ? err.message : '결제 처리 중 오류가 발생했습니다.')
      setPhase('idle')
    }
  }

  const busy = phase !== 'idle'
  const busyLabel =
    phase === 'checkout' ? '결제 요청 중...' :
    phase === 'confirm'  ? '이용권 활성화 중...' :
    phase === 'resume'   ? '분석 재실행 중...' : ''

  return (
    <div className="max-w-md mx-auto px-4 py-12">
      <div className="bg-white rounded-2xl border border-[#E2E8F0] shadow-sm overflow-hidden">
        {/* 헤더 */}
        <div className="px-6 py-8 text-center border-b border-[#F1F5F9]"
             style={{ background: 'linear-gradient(135deg, #EFF6FF 0%, #F5F3FF 100%)' }}>
          <div className="w-14 h-14 rounded-2xl bg-[#2563EB] flex items-center justify-center mx-auto mb-4">
            <Lock size={26} className="text-white" />
          </div>
          <h1 className="text-xl font-bold text-[#1E293B] mb-1.5">분석 이용권이 필요해요</h1>
          <p className="text-sm text-[#64748B]">
            {hasPending
              ? '결제하면 방금 입력한 분석을 바로 이어서 실행합니다.'
              : '이용권을 활성화하고 AI 위기 금융 분석을 시작하세요.'}
          </p>
        </div>

        {/* 플랜 카드 */}
        <div className="px-6 py-6">
          <div className="flex items-baseline gap-1.5 mb-5">
            <span className="text-3xl font-bold font-mono text-[#1E293B]">
              {amount != null ? fmt(amount) : '—'}
            </span>
            <span className="text-sm text-[#94A3B8]">/ 1회 분석 이용권</span>
          </div>

          <ul className="space-y-2.5 mb-6">
            {PLAN_FEATURES.map(f => (
              <li key={f} className="flex items-start gap-2.5 text-sm text-[#475569]">
                <Check size={16} className="text-[#10B981] flex-shrink-0 mt-0.5" />
                <span>{f}</span>
              </li>
            ))}
          </ul>

          {error && (
            <div className="mb-4 px-3.5 py-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600">
              {error}
            </div>
          )}

          <button
            onClick={handlePay}
            disabled={busy}
            className="w-full py-3 bg-[#2563EB] text-white font-semibold rounded-xl hover:bg-[#1D4ED8] disabled:opacity-60 disabled:cursor-not-allowed transition-colors text-sm flex items-center justify-center gap-2"
          >
            {busy
              ? <><Loader2 size={16} className="animate-spin" /> {busyLabel}</>
              : <>결제하고 바로 분석하기 <ArrowRight size={16} /></>}
          </button>

          <div className="mt-4 flex items-center justify-center gap-1.5 text-xs text-[#94A3B8]">
            <ShieldCheck size={13} />
            테스트 결제 (mock) · 결제 즉시 이용권이 활성화됩니다
          </div>
        </div>
      </div>

      <button
        onClick={() => router.push('/dashboard')}
        disabled={busy}
        className="w-full mt-4 text-center text-xs text-[#64748B] hover:text-[#2563EB] transition-colors disabled:opacity-50"
      >
        나중에 하기
      </button>
    </div>
  )
}
