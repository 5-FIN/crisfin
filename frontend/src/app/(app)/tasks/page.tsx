'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { CheckCircle, Circle } from 'lucide-react'
import { analysisStore, taskStore, priorityBadge } from '@/lib/utils'
import type { AnalysisResultResponse, TimelinePhase } from '@/lib/types'

/* 구간 색상 팔레트 (버킷 순서대로 순환) */
const PHASE_COLORS = ['#F59E0B', '#2563EB', '#8B5CF6', '#10B981']

interface ChecklistItem {
  key: string
  title: string
  priority: 'HIGH' | 'MED' | 'LOW'
  dayRange: string
  category: string
}

interface ChecklistGroup {
  range: string
  color: string
  items: ChecklistItem[]
}

/** result.timeline(백엔드 긴급도 정렬) → 체크리스트 그룹 */
function buildGroups(timeline: TimelinePhase[]): ChecklistGroup[] {
  return timeline
    .map((phase, pi) => ({
      range: phase.range,
      color: PHASE_COLORS[pi % PHASE_COLORS.length],
      items: phase.items.map((t, i) => ({
        key: `p${pi}-${i}`,
        title: t.title,
        priority: t.priority,
        dayRange: t.dayRange,
        category: t.category,
      })),
    }))
    .filter(g => g.items.length > 0)
}

export default function TasksPage() {
  const router = useRouter()
  const [analysis, setAnalysis] = useState<AnalysisResultResponse | null>(null)
  const [done, setDone]         = useState<Record<string, boolean>>({})

  useEffect(() => {
    const data = analysisStore.load()
    if (!data) { router.push('/diagnosis'); return }
    setAnalysis(data)
    setDone(taskStore.load(data.id))
  }, [router])

  if (!analysis) return null

  const timeline = analysis.result.timeline ?? []
  const groups   = buildGroups(timeline)
  const allItems = groups.flatMap(g => g.items)
  const doneCount  = allItems.filter(t => done[t.key]).length
  const totalCount = allItems.length
  const percentage = totalCount > 0 ? Math.round((doneCount / totalCount) * 100) : 0

  function toggle(key: string) {
    const next = taskStore.toggle(analysis!.id, key)
    setDone({ ...next })
  }

  return (
    <div className="px-4 md:px-8 py-8 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-[#1E293B] mb-2">액션 체크리스트</h1>
      <p className="text-sm text-[#64748B] mb-8">긴급도 순으로 정렬된 30일 타임라인입니다.</p>

      {/* 전체 진행률 */}
      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-6 mb-8 shadow-sm">
        <div className="flex items-center justify-between mb-3">
          <div>
            <div className="text-xs text-[#64748B] mb-0.5">전체 진행률</div>
            <div className="text-3xl font-bold font-mono text-[#1E293B]">{percentage}%</div>
          </div>
          <div className="text-right">
            <div className="text-2xl font-bold text-[#10B981] font-mono">{doneCount}</div>
            <div className="text-xs text-[#64748B]">/ {totalCount} 완료</div>
          </div>
        </div>
        <div className="h-3 bg-[#F1F5F9] rounded-full overflow-hidden">
          <div className="h-full rounded-full bg-[#10B981] transition-all duration-500"
               style={{ width: `${percentage}%` }} />
        </div>
        {percentage === 100 && totalCount > 0 && (
          <div className="mt-3 text-sm text-[#10B981] font-medium flex items-center gap-1.5">
            <CheckCircle size={16} /> 모든 항목 완료! 수고하셨습니다.
          </div>
        )}
      </div>

      {/* 30일 타임라인 그룹 */}
      {groups.map(group => (
        <div key={group.range} className="mb-8">
          {/* 섹션 헤더 */}
          <div className="flex items-center gap-3 mb-4">
            <div className="w-3 h-3 rounded-full" style={{ background: group.color }} />
            <span className="text-sm font-semibold" style={{ color: group.color }}>{group.range}</span>
            <div className="flex-1 h-px bg-[#E2E8F0]" />
            <span className="text-xs text-[#94A3B8]">
              {group.items.filter(t => done[t.key]).length}/{group.items.length}
            </span>
          </div>

          {/* 체크리스트 */}
          <div className="space-y-3">
            {group.items.map(item => {
              const isDone = !!done[item.key]
              const badge  = priorityBadge(item.priority)
              return (
                <button
                  key={item.key}
                  onClick={() => toggle(item.key)}
                  className={`w-full text-left p-4 rounded-2xl border transition-all hover:shadow-sm
                    ${isDone ? 'border-[#D1FAE5] bg-[#F0FDF4]' : 'border-[#E2E8F0] bg-white hover:border-[#DBEAFE]'}`}
                >
                  <div className="flex items-start gap-3">
                    {isDone
                      ? <CheckCircle size={22} className="text-[#10B981] flex-shrink-0 mt-0.5" />
                      : <Circle     size={22} className="text-[#CBD5E1] flex-shrink-0 mt-0.5" />
                    }
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap mb-1">
                        <span className={`font-medium ${isDone ? 'line-through text-[#94A3B8]' : 'text-[#1E293B]'}`}>
                          {item.title}
                        </span>
                        {!isDone && (
                          <span className="text-xs px-2 py-0.5 rounded-full border flex-shrink-0"
                                style={{ background: badge.bg, color: badge.color, borderColor: badge.border }}>
                            {badge.label}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-3 text-xs text-[#94A3B8]">
                        <span>{item.dayRange}</span>
                        {item.category && (
                          <>
                            <span>·</span>
                            <span>{item.category}</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                </button>
              )
            })}
          </div>
        </div>
      ))}

      {groups.length === 0 && (
        <div className="text-center py-16 text-[#94A3B8]">타임라인 항목이 없습니다.</div>
      )}
    </div>
  )
}
