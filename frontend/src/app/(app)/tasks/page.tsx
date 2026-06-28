'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { CheckCircle, Circle } from 'lucide-react'
import { analysisStore, taskStore, priorityBadge } from '@/lib/utils'
import type { AnalysisResultResponse, TodoItem } from '@/lib/types'

interface TaskGroup {
  period: string
  color: string
  bg: string
  items: { key: string; action: string; reason: string; dayRange: string; deadline: string; priority: 'HIGH' | 'MED' | 'LOW' }[]
}

function buildTaskGroups(todos: TodoItem[]): TaskGroup[] {
  const today: TodoItem[]  = todos.filter(t => t.dayRange.startsWith('D+0') || t.dayRange.includes('0~'))
  const week: TodoItem[]   = todos.filter(t => t.dayRange.includes('D+3') || t.dayRange.includes('7') || t.dayRange.includes('주'))
  const month: TodoItem[]  = todos.filter(t => !today.includes(t) && !week.includes(t))

  const toItem = (t: TodoItem, i: number, prefix: string) => ({
    key: `${prefix}-${i}`, action: t.action, reason: t.reason,
    dayRange: t.dayRange, deadline: t.deadline, priority: t.priority,
  })

  return [
    { period: '오늘',    color: '#F59E0B', bg: '#FFFBEB', items: today.map((t, i) => toItem(t, i, 'today')) },
    { period: '이번 주', color: '#2563EB', bg: '#EFF6FF', items: week.map((t, i) => toItem(t, i, 'week')) },
    { period: '30일 내', color: '#8B5CF6', bg: '#F5F3FF', items: month.map((t, i) => toItem(t, i, 'month')) },
  ].filter(g => g.items.length > 0)
}

export default function TasksPage() {
  const router = useRouter()
  const [analysis, setAnalysis]   = useState<AnalysisResultResponse | null>(null)
  const [done, setDone]           = useState<Record<string, boolean>>({})

  useEffect(() => {
    const data = analysisStore.load()
    if (!data) { router.push('/diagnosis'); return }
    setAnalysis(data)
    setDone(taskStore.load(data.id))
  }, [router])

  if (!analysis) return null

  const groups   = buildTaskGroups(analysis.result.todos)
  const allItems = groups.flatMap(g => g.items)
  const doneCount   = allItems.filter(t => done[t.key]).length
  const totalCount  = allItems.length
  const percentage  = totalCount > 0 ? Math.round((doneCount / totalCount) * 100) : 0

  function toggle(key: string) {
    const next = taskStore.toggle(analysis!.id, key)
    setDone({ ...next })
  }

  return (
    <div className="px-4 md:px-8 py-8 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-[#1E293B] mb-2">액션 체크리스트</h1>
      <p className="text-sm text-[#64748B] mb-8">타임라인 순서대로 처리해주세요.</p>

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
        {percentage === 100 && (
          <div className="mt-3 text-sm text-[#10B981] font-medium flex items-center gap-1.5">
            <CheckCircle size={16} /> 모든 항목 완료! 수고하셨습니다.
          </div>
        )}
      </div>

      {/* 타임라인 그룹 */}
      {groups.map(group => (
        <div key={group.period} className="mb-8">
          {/* 섹션 헤더 */}
          <div className="flex items-center gap-3 mb-4">
            <div className="w-3 h-3 rounded-full" style={{ background: group.color }} />
            <span className="text-sm font-semibold" style={{ color: group.color }}>{group.period}</span>
            <div className="flex-1 h-px bg-[#E2E8F0]" />
            <span className="text-xs text-[#94A3B8]">
              {group.items.filter(t => done[t.key]).length}/{group.items.length}
            </span>
          </div>

          {/* 체크리스트 */}
          <div className="space-y-3">
            {group.items.map(item => {
              const isDone  = !!done[item.key]
              const badge   = priorityBadge(item.priority)
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
                          {item.action}
                        </span>
                        {!isDone && (
                          <span className="text-xs px-2 py-0.5 rounded-full border flex-shrink-0"
                                style={{ background: badge.bg, color: badge.color, borderColor: badge.border }}>
                            {badge.label}
                          </span>
                        )}
                      </div>
                      <div className={`text-sm ${isDone ? 'text-[#CBD5E1]' : 'text-[#64748B]'}`}>
                        {item.reason}
                      </div>
                      <div className="flex items-center gap-3 mt-1.5 text-xs text-[#94A3B8]">
                        <span>{item.dayRange}</span>
                        <span>·</span>
                        <span>기한: {item.deadline}</span>
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
        <div className="text-center py-16 text-[#94A3B8]">할 일 목록이 없습니다.</div>
      )}
    </div>
  )
}
