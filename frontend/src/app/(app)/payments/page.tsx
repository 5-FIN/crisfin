'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { PauseCircle, AlertTriangle, Info } from 'lucide-react'
import { analysisStore, fmt, riskBadge } from '@/lib/utils'
import type { AnalysisResultResponse, HoldableItem } from '@/lib/types'

type Tab = 'holdable' | 'urgent'

export default function PaymentsPage() {
  const router = useRouter()
  const [analysis, setAnalysis] = useState<AnalysisResultResponse | null>(null)
  const [tab, setTab] = useState<Tab>('urgent')
  const [expanded, setExpanded] = useState<number | null>(null)

  useEffect(() => {
    const data = analysisStore.load()
    if (!data) { router.push('/diagnosis'); return }
    setAnalysis(data)
  }, [router])

  if (!analysis) return null

  const { holdable, todos } = analysis.result
  const urgentTodos = todos.filter(t => t.priority === 'HIGH')

  /* holdable 위험도별 통계 */
  const highRisk = holdable.filter(h => h.riskLevel === 'LOW').length  // 신용영향 낮은 것 = 유예 권장
  const totalHoldable = holdable.length

  const tabs: { key: Tab; label: string; count: number }[] = [
    { key: 'urgent',   label: '긴급 처리 항목', count: urgentTodos.length },
    { key: 'holdable', label: '미룰 수 있는 것', count: totalHoldable },
  ]

  return (
    <div className="px-4 md:px-8 py-8 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-[#1E293B] mb-2">납부 관리</h1>
      <p className="text-sm text-[#64748B] mb-8">AI가 분석한 납부 우선순위와 유예 가능 항목입니다.</p>

      {/* 요약 카드 */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        {[
          { label: '긴급 처리', value: `${urgentTodos.length}건`, sub: '즉시 행동 필요', color: '#F59E0B', bg: '#FFFBEB', icon: AlertTriangle },
          { label: '유예 가능', value: `${highRisk}건`,          sub: '신용 영향 낮음',  color: '#2563EB', bg: '#EFF6FF', icon: PauseCircle },
          { label: '총 유예 항목', value: `${totalHoldable}건`, sub: 'AI 분석 결과',    color: '#8B5CF6', bg: '#F5F3FF', icon: Info },
        ].map(({ label, value, sub, color, bg, icon: Icon }) => (
          <div key={label} className="bg-white rounded-2xl border border-[#E2E8F0] p-5 shadow-sm">
            <div className="flex items-center gap-2 mb-3">
              <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: bg }}>
                <Icon size={16} style={{ color }} />
              </div>
              <span className="text-xs text-[#64748B]">{label}</span>
            </div>
            <div className="text-2xl font-bold font-mono" style={{ color }}>{value}</div>
            <div className="text-xs text-[#94A3B8] mt-1">{sub}</div>
          </div>
        ))}
      </div>

      {/* 탭 */}
      <div className="flex gap-1 p-1 bg-[#F1F5F9] rounded-xl mb-6 w-fit">
        {tabs.map(({ key, label, count }) => (
          <button key={key} onClick={() => setTab(key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2
              ${tab === key ? 'bg-white text-[#1E293B] shadow-sm' : 'text-[#64748B] hover:text-[#1E293B]'}`}>
            {label}
            <span className={`text-xs px-1.5 py-0.5 rounded-full ${tab === key ? 'bg-[#EFF6FF] text-[#2563EB]' : 'bg-[#E2E8F0] text-[#64748B]'}`}>
              {count}
            </span>
          </button>
        ))}
      </div>

      {/* 긴급 처리 탭 */}
      {tab === 'urgent' && (
        <div className="space-y-3">
          {urgentTodos.length === 0 ? (
            <div className="text-center py-12 text-[#94A3B8]">긴급 처리 항목이 없습니다.</div>
          ) : (
            urgentTodos.map((todo, i) => (
              <div key={i} className="bg-white rounded-2xl border border-[#FEF3C7] p-5 shadow-sm">
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-[#FFFBEB] flex items-center justify-center flex-shrink-0">
                    <AlertTriangle size={18} className="text-[#F59E0B]" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap mb-1">
                      <span className="font-semibold text-[#1E293B]">{todo.action}</span>
                      <span className="text-xs px-2 py-0.5 rounded-full bg-[#FFFBEB] text-[#D97706] border border-[#FEF3C7]">
                        긴급
                      </span>
                    </div>
                    <div className="text-sm text-[#64748B] mb-2">{todo.reason}</div>
                    <div className="flex items-center gap-4 text-xs text-[#94A3B8]">
                      <span>기간: {todo.dayRange}</span>
                      <span>기한: {todo.deadline}</span>
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* 유예 가능 탭 */}
      {tab === 'holdable' && (
        <div className="space-y-3">
          {holdable.length === 0 ? (
            <div className="text-center py-12 text-[#94A3B8]">유예 가능 항목이 없습니다.</div>
          ) : (
            holdable.map((item: HoldableItem, i) => {
              const badge = riskBadge(item.riskLevel)
              const isOpen = expanded === i
              return (
                <div key={i} className="bg-white rounded-2xl border border-[#E2E8F0] shadow-sm overflow-hidden">
                  <button className="w-full p-5 flex items-center gap-4 text-left hover:bg-[#F8FAFC] transition-colors"
                          onClick={() => setExpanded(isOpen ? null : i)}>
                    <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                         style={{ background: '#EFF6FF' }}>
                      <PauseCircle size={18} className="text-[#2563EB]" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-[#1E293B]">{item.name}</span>
                        <span className="text-xs px-2 py-0.5 rounded-full border"
                              style={{ background: badge.bg, color: badge.color, borderColor: badge.border }}>
                          신용영향 {badge.label}
                        </span>
                      </div>
                      <div className="text-sm text-[#64748B] mt-0.5">유예 기간: {item.deferPeriod}</div>
                    </div>
                    <span className="text-[#94A3B8] text-lg flex-shrink-0">{isOpen ? '▲' : '▼'}</span>
                  </button>

                  {isOpen && (
                    <div className="px-5 pb-5 border-t border-[#F1F5F9]">
                      <div className="pt-4 grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div className="p-3 rounded-xl bg-[#F8FAFC]">
                          <div className="text-xs text-[#64748B] mb-1">신청 방법</div>
                          <div className="text-sm text-[#1E293B]">{item.howTo}</div>
                        </div>
                        <div className="p-3 rounded-xl bg-[#FFFBEB]">
                          <div className="text-xs text-[#64748B] mb-1">주의사항</div>
                          <div className="text-sm text-[#D97706]">{item.caution}</div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>
      )}
    </div>
  )
}
