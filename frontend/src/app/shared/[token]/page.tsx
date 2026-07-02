'use client'

import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import { Loader2, CheckCircle, ArrowRight } from 'lucide-react'
import { analysisApi } from '@/lib/api'
import { CRISIS_LABELS, CRISIS_EMOJI, fmt } from '@/lib/utils'
import type { AnalysisResultResponse } from '@/lib/types'

/**
 * 공개 공유 페이지 — 토큰으로 분석 결과를 읽기 전용 조회한다.
 * (app) 레이아웃 밖이라 사이드바/인증이 없다. 비로그인 방문자도 볼 수 있다.
 */
export default function SharedResultPage() {
  const params = useParams()
  const token = String(params?.token ?? '')

  const [data, setData] = useState<AnalysisResultResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) return
    analysisApi.getShared(token)
      .then(setData)
      .catch(() => setError('공유된 분석 결과를 찾을 수 없거나 만료되었습니다.'))
      .finally(() => setLoading(false))
  }, [token])

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center"><Loader2 size={28} className="text-[#2563EB] animate-spin" /></div>
  }

  if (error || !data) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4 px-4">
        <p className="text-sm text-[#64748B]">{error || '결과를 불러올 수 없습니다.'}</p>
        <Link href="/" className="text-sm text-[#2563EB] hover:underline">CrisFin 홈으로</Link>
      </div>
    )
  }

  const { result, crisisType } = data
  const { summary, receivable, todos, actions } = result

  return (
    <div className="min-h-screen bg-[#F9FAFB]">
      {/* 상단바 */}
      <header className="h-14 bg-white border-b border-[#E2E8F0] flex items-center px-4 md:px-8">
        <Link href="/" className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-[#2563EB] flex items-center justify-center text-white text-sm font-bold">C</div>
          <span className="font-bold text-[#1E293B]">CrisFin</span>
        </Link>
        <span className="ml-3 text-xs text-[#94A3B8]">공유된 분석 결과</span>
      </header>

      <div className="max-w-2xl mx-auto px-4 py-8">
        {/* 위기유형 */}
        <div className="flex items-center gap-2 mb-4">
          <span className="text-xl">{CRISIS_EMOJI[crisisType]}</span>
          <span className="text-sm font-medium text-[#D97706] bg-[#FFFBEB] px-2 py-0.5 rounded-full border border-[#FEF3C7]">
            {CRISIS_LABELS[crisisType] ?? crisisType}
          </span>
        </div>

        {/* 총 수령 예상액 */}
        <div className="bg-gradient-to-r from-[#ECFDF5] to-[#D1FAE5] rounded-2xl border border-[#A7F3D0] p-6 mb-6">
          <div className="text-sm text-[#059669] font-medium mb-1">예상 총 수령 가능액</div>
          <div className="text-3xl font-bold font-mono text-[#10B981]">
            {fmt(summary.totalReceivableMin)} ~ {fmt(summary.totalReceivableMax)}
          </div>
          <div className="text-xs text-[#6EE7B7] mt-1">긴급 처리 {summary.urgentCount}건 · {summary.thirtyDayPlan}</div>
        </div>

        {/* 받을 돈 */}
        {receivable?.length > 0 && (
          <Section title="받을 수 있는 혜택">
            {receivable.map((item, i) => (
              <Row key={i} name={item.name} sub={item.source}
                right={item.estimatedMin != null ? `${fmt(item.estimatedMin)}+` : '추가입력 필요'} />
            ))}
          </Section>
        )}

        {/* 할 일 */}
        {todos?.length > 0 && (
          <Section title="할 일">
            {todos.map((t, i) => (
              <div key={i} className="flex items-start gap-2.5 p-3">
                <CheckCircle size={16} className="text-[#2563EB] mt-0.5 flex-shrink-0" />
                <div>
                  <div className="text-sm text-[#1E293B]">{t.action}</div>
                  <div className="text-xs text-[#94A3B8]">{t.dayRange} · {t.deadline}</div>
                </div>
              </div>
            ))}
          </Section>
        )}

        {/* 즉시 행동 */}
        {actions?.length > 0 && (
          <Section title="즉시 행동">
            {actions.map((a, i) => (
              <Row key={i} name={a.name} sub={`기한: ${a.deadline} · ${a.contactInfo}`} />
            ))}
          </Section>
        )}

        <p className="text-xs text-[#94A3B8] text-center my-6">{result.disclaimer}</p>

        {/* CTA */}
        <Link href="/"
          className="flex items-center justify-center gap-1.5 w-full py-3 bg-[#2563EB] text-white font-semibold rounded-xl hover:bg-[#1D4ED8] transition-colors text-sm">
          나도 위기 금융 분석 받기 <ArrowRight size={16} />
        </Link>
      </div>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-6">
      <h2 className="text-sm font-semibold text-[#475569] uppercase tracking-wide mb-2">{title}</h2>
      <div className="bg-white rounded-2xl border border-[#E2E8F0] divide-y divide-[#F1F5F9]">{children}</div>
    </div>
  )
}

function Row({ name, sub, right }: { name: string; sub: string; right?: string }) {
  return (
    <div className="flex items-center justify-between p-4">
      <div className="min-w-0 mr-3">
        <div className="text-sm font-medium text-[#1E293B] truncate">{name}</div>
        <div className="text-xs text-[#94A3B8] truncate">{sub}</div>
      </div>
      {right && <div className="text-sm font-bold font-mono text-[#10B981] flex-shrink-0">{right}</div>}
    </div>
  )
}
