'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { analysisApi } from '@/lib/api'
import { analysisStore, CRISIS_LABELS, CRISIS_EMOJI, fmt } from '@/lib/utils'
import type { AnalysisResultResponse, PageResponse } from '@/lib/types'

export default function HistoryPage() {
  const router = useRouter()
  const [data, setData] = useState<PageResponse<AnalysisResultResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    analysisApi.history()
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  function handleSelect(item: AnalysisResultResponse) {
    analysisStore.save(item)
    router.push('/dashboard')
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-2 border-[#2563EB] border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="p-6 max-w-2xl mx-auto">
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-red-700 text-sm">
          {error}
        </div>
      </div>
    )
  }

  if (!data?.content.length) {
    return (
      <div className="p-6 max-w-2xl mx-auto">
        <h1 className="text-xl font-bold text-[#1E293B] mb-6">분석 히스토리</h1>
        <div className="bg-white rounded-xl border border-[#E2E8F0] p-12 text-center">
          <p className="text-[#64748B] text-sm">아직 분석 이력이 없습니다.</p>
          <button
            onClick={() => router.push('/diagnosis')}
            className="mt-4 px-4 py-2 bg-[#2563EB] text-white text-sm rounded-lg hover:bg-[#1D4ED8] transition-colors"
          >
            첫 분석 시작하기
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-xl font-bold text-[#1E293B] mb-6">분석 히스토리</h1>
      <div className="space-y-3">
        {data.content.map((item) => {
          const summary = (item.result as { summary?: { totalReceivableMax?: number } })?.summary
          return (
            <button
              key={item.id}
              onClick={() => handleSelect(item)}
              className="w-full text-left bg-white rounded-xl border border-[#E2E8F0] p-4 hover:border-[#2563EB] hover:shadow-sm transition-all"
            >
              <div className="flex items-center gap-3">
                <span className="text-2xl">{CRISIS_EMOJI[item.crisisType] ?? '📋'}</span>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-[#1E293B]">
                    {CRISIS_LABELS[item.crisisType] ?? item.crisisType}
                  </p>
                  <p className="text-xs text-[#94A3B8] mt-0.5 truncate">
                    {item.situationDescription.slice(0, 60)}
                    {item.situationDescription.length > 60 ? '…' : ''}
                  </p>
                  <p className="text-xs text-[#CBD5E1] mt-1">
                    {new Date(item.createdAt).toLocaleDateString('ko-KR', {
                      year: 'numeric', month: 'long', day: 'numeric',
                    })}
                  </p>
                </div>
                {summary?.totalReceivableMax != null && (
                  <div className="text-right flex-shrink-0">
                    <p className="text-sm font-semibold text-[#2563EB]">
                      최대 {fmt(summary.totalReceivableMax)}
                    </p>
                    <p className="text-xs text-[#94A3B8]">수령 가능</p>
                  </div>
                )}
              </div>
            </button>
          )
        })}
      </div>

      {data.totalPages > 1 && (
        <p className="text-center text-xs text-[#94A3B8] mt-4">
          {data.number + 1} / {data.totalPages} 페이지
        </p>
      )}
    </div>
  )
}
