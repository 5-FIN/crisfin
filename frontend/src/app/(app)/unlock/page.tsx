'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Check, Loader2, Sparkles, BookOpen } from 'lucide-react'
import { paymentApi } from '@/lib/api'
import { pendingAnalysisStore, fmt } from '@/lib/utils'
import { resumePendingAnalysis } from '@/lib/resumeAnalysis'
import type { PlanResponse } from '@/lib/types'

/** 무료 플랜은 결제 대상이 아니라 카탈로그에만 표시(길라잡이는 상시 무료) */
const FREE_PLAN = {
  name: '무료',
  tagline: '로그인 없이 지금 바로',
  features: ['위기 유형별 정보 길라잡이', 'AI 프롬프트 복사', '지역 복지 제도 탐색'],
}

/** 추천(하이라이트)할 플랜 코드 */
const RECOMMENDED = 'UNLIMITED_30D'

export default function UnlockPage() {
  const router = useRouter()
  const [plans, setPlans] = useState<PlanResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [busyPlan, setBusyPlan] = useState<string | null>(null)
  const [phase, setPhase] = useState<'checkout' | 'confirm' | 'resume' | null>(null)
  const [error, setError] = useState('')
  const hasPending = typeof window !== 'undefined' && !!pendingAnalysisStore.load()

  useEffect(() => {
    paymentApi.plans()
      .then(setPlans)
      .catch(() => setError('요금제를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  async function handlePay(planCode: string) {
    setError('')
    setBusyPlan(planCode)
    try {
      setPhase('checkout')
      const { orderUid } = await paymentApi.checkout(planCode)
      setPhase('confirm')
      await paymentApi.confirm(orderUid)
      // 결제 후: 보류 분석이 있으면 바로 이어서 실행, 없으면 진단으로
      setPhase('resume')
      const resumed = await resumePendingAnalysis(router.push)
      if (!resumed) router.push('/diagnosis')
    } catch (err) {
      setError(err instanceof Error ? err.message : '결제 처리 중 오류가 발생했습니다.')
      setBusyPlan(null)
      setPhase(null)
    }
  }

  const busyLabel =
    phase === 'checkout' ? '결제 요청 중...' :
    phase === 'confirm'  ? '이용권 활성화 중...' :
    phase === 'resume'   ? '분석 실행 중...' : ''

  return (
    <div className="px-4 md:px-8 py-8 max-w-4xl mx-auto">
      {/* 앱 다른 페이지와 동일한 좌측 정렬 헤더 */}
      <h1 className="text-2xl font-bold text-[#1E293B] mb-2">요금제</h1>
      <p className="text-sm text-[#64748B] mb-8">
        {hasPending
          ? '결제하면 방금 입력한 분석을 바로 이어서 실행합니다.'
          : '무료로 정보를 둘러보거나, AI 맞춤 분석으로 위기를 정리하세요.'}
      </p>

      {error && (
        <div className="mb-6 px-3.5 py-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 size={26} className="text-[#2563EB] animate-spin" /></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-stretch">
          {/* 무료 */}
          <PlanCard
            name={FREE_PLAN.name}
            tagline={FREE_PLAN.tagline}
            price="무료"
            features={FREE_PLAN.features}
            ctaLabel="길라잡이 보기"
            ctaIcon={<BookOpen size={15} />}
            onClick={() => router.push('/guide')}
            variant="free"
            disabled={busyPlan !== null}
          />

          {/* 유료 플랜 (SINGLE, UNLIMITED_30D) */}
          {plans.map(p => {
            const recommended = p.code === RECOMMENDED
            const busy = busyPlan === p.code
            return (
              <PlanCard
                key={p.code}
                name={p.name}
                tagline={p.tagline}
                price={fmt(p.priceKrw)}
                priceSuffix={p.uses == null ? `/ ${p.durationDays}일` : `/ ${p.uses}회`}
                features={p.features}
                ctaLabel={busy ? busyLabel : '결제하고 시작'}
                ctaIcon={busy ? <Loader2 size={15} className="animate-spin" /> : undefined}
                onClick={() => handlePay(p.code)}
                variant={recommended ? 'recommended' : 'paid'}
                badge={recommended ? '추천' : undefined}
                disabled={busyPlan !== null}
              />
            )
          })}
        </div>
      )}

      <div className="mt-6 flex items-center justify-between">
        <span className="text-xs text-[#94A3B8]">테스트 결제(mock) · 결제 즉시 이용권이 활성화됩니다</span>
        <button
          onClick={() => router.push('/dashboard')}
          disabled={busyPlan !== null}
          className="text-xs text-[#64748B] hover:text-[#2563EB] transition-colors disabled:opacity-50"
        >
          나중에 하기
        </button>
      </div>
    </div>
  )
}

/* ── 플랜 카드 (앱 카드 스타일과 일관) ── */
function PlanCard({
  name, tagline, price, priceSuffix, features, ctaLabel, ctaIcon, onClick, variant, badge, disabled,
}: {
  name: string
  tagline: string
  price: string
  priceSuffix?: string
  features: string[]
  ctaLabel: string
  ctaIcon?: React.ReactNode
  onClick: () => void
  variant: 'free' | 'paid' | 'recommended'
  badge?: string
  disabled?: boolean
}) {
  const recommended = variant === 'recommended'
  return (
    <div className={`relative bg-white rounded-2xl border p-5 shadow-sm flex flex-col ${
      recommended ? 'border-[#2563EB]' : 'border-[#E2E8F0]'
    }`}>
      {badge && (
        <span className="absolute top-4 right-4 text-[11px] font-bold text-[#2563EB] bg-[#EFF6FF] border border-[#DBEAFE] px-2 py-0.5 rounded-full flex items-center gap-1">
          <Sparkles size={10} /> {badge}
        </span>
      )}
      <div className="mb-3">
        <div className="font-bold text-[#1E293B]">{name}</div>
        <div className="text-xs text-[#94A3B8] mt-0.5">{tagline}</div>
      </div>
      <div className="flex items-baseline gap-1 mb-4">
        <span className="text-xl font-bold tabular-nums text-[#1E293B]">{price}</span>
        {priceSuffix && <span className="text-xs text-[#94A3B8]">{priceSuffix}</span>}
      </div>
      <ul className="space-y-2 mb-5 flex-1">
        {features.map(f => (
          <li key={f} className="flex items-start gap-2 text-sm text-[#475569]">
            <Check size={15} className="text-[#10B981] flex-shrink-0 mt-0.5" />
            <span>{f}</span>
          </li>
        ))}
      </ul>
      <button
        onClick={onClick}
        disabled={disabled}
        className={`w-full py-2.5 font-semibold rounded-lg transition-colors text-sm flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed ${
          variant === 'free'
            ? 'bg-[#F1F5F9] text-[#475569] hover:bg-[#E2E8F0]'
            : 'bg-[#2563EB] text-white hover:bg-[#1D4ED8]'
        }`}
      >
        {ctaIcon} {ctaLabel}
      </button>
    </div>
  )
}
