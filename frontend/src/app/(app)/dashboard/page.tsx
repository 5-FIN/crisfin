'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { ArrowRight, TrendingUp, PauseCircle, DollarSign, CheckSquare } from 'lucide-react'
import { analysisStore, fmt, fmtAmount, CRISIS_LABELS, CRISIS_EMOJI, priorityBadge } from '@/lib/utils'
import NoAnalysisEmptyState from '@/components/common/NoAnalysisEmptyState'
import type { AnalysisResultResponse } from '@/lib/types'

export default function DashboardPage() {
  const [analysis, setAnalysis] = useState<AnalysisResultResponse | null>(null)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    setAnalysis(analysisStore.load())
    setLoaded(true)
  }, [])

  if (!loaded) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="animate-pulse text-[#94A3B8]">불러오는 중...</div>
      </div>
    )
  }

  if (!analysis) return <NoAnalysisEmptyState title="대시보드" />

  const { result, crisisType } = analysis
  const { summary, todos, receivable, holdable, actions } = result

  /* Runway: 받을돈 최소 / (holdable 없이 유예 가정 후 지출) — 간단 추산 */
  const runwayDays = Math.min(90, Math.round((summary.totalReceivableMin ?? 0) / 300_000 * 7) + 14)

  const quadrants = [
    {
      href: '/tasks',   icon: CheckSquare, label: '할 일',
      value: `${todos.length}건`,
      sub: `긴급 ${todos.filter(t => t.priority === 'HIGH').length}건`,
      color: '#F59E0B', bg: '#FFFBEB',
    },
    {
      href: '/benefits', icon: DollarSign, label: '받을 돈',
      value: `${fmtAmount(summary.totalReceivableMin, '0원')}~`,
      sub: `최대 ${fmtAmount(summary.totalReceivableMax, '0원')}`,
      color: '#10B981', bg: '#ECFDF5',
    },
    {
      href: '/payments', icon: PauseCircle, label: '미룰 것',
      value: `${holdable.length}건`,
      sub: '유예 가능 항목',
      color: '#2563EB', bg: '#EFF6FF',
    },
    {
      href: '/tasks',   icon: TrendingUp, label: '행동',
      value: `${actions.length}건`,
      sub: `긴급 ${actions.filter(a => a.priority === 'HIGH').length}건`,
      color: '#8B5CF6', bg: '#F5F3FF',
    },
  ]

  return (
    <div className="px-4 md:px-8 py-8 max-w-6xl mx-auto">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xl">{CRISIS_EMOJI[crisisType]}</span>
            <span className="text-sm font-medium text-[#F59E0B] bg-[#FFFBEB] px-2 py-0.5 rounded-full border border-[#FEF3C7]">
              {CRISIS_LABELS[crisisType]} 대응 중
            </span>
          </div>
          <h1 className="text-2xl font-bold text-[#1E293B]">내 금융 위기 대응 현황</h1>
        </div>
        <Link href="/diagnosis"
          className="text-xs text-[#64748B] hover:text-[#2563EB] flex items-center gap-1 border border-[#E2E8F0] rounded-lg px-3 py-1.5 transition-colors">
          재분석 <ArrowRight size={12} />
        </Link>
      </div>

      {/* Runway 바 */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-5 mb-6 shadow-sm">
        <div className="flex items-center justify-between mb-3">
          <div>
            <div className="text-xs text-[#64748B] mb-0.5">재정 생존 가능 기간 (추산)</div>
            <div className="text-3xl font-bold font-mono text-[#1E293B]">{runwayDays}일</div>
          </div>
          <div className="text-right">
            <div className="text-xs text-[#64748B]">긴급 처리 필요</div>
            <div className="text-lg font-bold text-[#F59E0B]">{summary.urgentCount}건</div>
          </div>
        </div>
        <div className="h-3 bg-[#F1F5F9] rounded-full overflow-hidden">
          <div className="h-full rounded-full transition-all"
               style={{
                 width: `${Math.min(100, (runwayDays / 90) * 100)}%`,
                 background: runwayDays > 45 ? '#10B981' : runwayDays > 20 ? '#F59E0B' : '#EF4444',
               }} />
        </div>
        <div className="mt-2 text-xs text-[#94A3B8]">{summary.thirtyDayPlan}</div>
      </div>

      {/* 4분면 카드 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {quadrants.map(({ href, icon: Icon, label, value, sub, color, bg }) => (
          <Link key={label} href={href}
            className="bg-white rounded-2xl border border-[#E2E8F0] p-5 hover:shadow-md hover:-translate-y-0.5 transition-all group">
            <div className="w-10 h-10 rounded-xl flex items-center justify-center mb-4"
                 style={{ background: bg }}>
              <Icon size={20} style={{ color }} />
            </div>
            <div className="text-xs text-[#64748B] mb-1">{label}</div>
            <div className="text-2xl font-bold font-mono" style={{ color }}>{value}</div>
            <div className="text-xs text-[#94A3B8] mt-1">{sub}</div>
            <div className="flex items-center gap-1 mt-3 text-xs font-medium group-hover:gap-2 transition-all"
                 style={{ color }}>
              자세히 보기 <ArrowRight size={12} />
            </div>
          </Link>
        ))}
      </div>

      {/* 긴급 할 일 미리보기 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-[#1E293B]">긴급 할 일</h3>
            <Link href="/tasks" className="text-xs text-[#2563EB] hover:underline flex items-center gap-1">
              전체보기 <ArrowRight size={12} />
            </Link>
          </div>
          <div className="space-y-3">
            {todos.slice(0, 3).map((todo, i) => {
              const badge = priorityBadge(todo.priority)
              return (
                <div key={i} className="flex items-start gap-3 p-3 rounded-xl" style={{ background: '#F8FAFC' }}>
                  <span className="text-xs font-medium px-2 py-1 rounded-full flex-shrink-0"
                        style={{ background: badge.bg, color: badge.color }}>
                    {badge.label}
                  </span>
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-[#1E293B] truncate">{todo.action}</div>
                    <div className="text-xs text-[#94A3B8]">{todo.dayRange} · {todo.deadline}</div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {/* 받을 돈 미리보기 */}
        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-[#1E293B]">받을 수 있는 혜택</h3>
            <Link href="/benefits" className="text-xs text-[#2563EB] hover:underline flex items-center gap-1">
              전체보기 <ArrowRight size={12} />
            </Link>
          </div>
          <div className="space-y-3">
            {receivable.slice(0, 3).map((item, i) => {
              const needsInput = item.status === 'NEEDS_MORE_INPUT' || item.estimatedMin == null
              return (
                <div key={i} className="flex items-center justify-between p-3 rounded-xl bg-[#F8FAFC]">
                  <div className="min-w-0 mr-3">
                    <div className="text-sm font-medium text-[#1E293B] truncate">{item.name}</div>
                    <div className="text-xs text-[#94A3B8]">{item.source}</div>
                  </div>
                  {needsInput ? (
                    <div className="text-xs text-[#EA580C] flex-shrink-0">추가입력 필요</div>
                  ) : (
                    <div className="text-sm font-bold font-mono text-[#10B981] flex-shrink-0">
                      {fmt(item.estimatedMin as number)}+
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      </div>

      {/* 면책 조항 */}
      <p className="mt-6 text-xs text-[#94A3B8] text-center">{result.disclaimer}</p>
    </div>
  )
}
