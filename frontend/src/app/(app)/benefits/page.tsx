'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { ExternalLink, ChevronDown, ChevronUp } from 'lucide-react'
import { analysisStore, fmt, priorityBadge } from '@/lib/utils'
import { welfareApi } from '@/lib/api'
import type { AnalysisResultResponse, WelfareBenefitResponse, ReceivableItem, CrisisType } from '@/lib/types'

const NEEDS_MORE_INPUT_BADGE = { bg: '#FFF7ED', color: '#EA580C', border: '#FFEDD5' }

type Category = '전체' | '보험/환급' | '복지제도'

export default function BenefitsPage() {
  const router = useRouter()
  const [analysis, setAnalysis]   = useState<AnalysisResultResponse | null>(null)
  const [welfare, setWelfare]     = useState<WelfareBenefitResponse[]>([])
  const [filter, setFilter]       = useState<Category>('전체')
  const [expanded, setExpanded]   = useState<string | null>(null)
  const [loadingWelfare, setLoadingWelfare] = useState(false)

  useEffect(() => {
    const data = analysisStore.load()
    if (!data) { router.push('/diagnosis'); return }
    setAnalysis(data)

    // 복지 API 호출
    setLoadingWelfare(true)
    welfareApi.list({ crisisType: data.crisisType as CrisisType, size: 20 })
      .then(res => setWelfare(res.content))
      .catch(() => {/* API 미연결 시 무시 */})
      .finally(() => setLoadingWelfare(false))
  }, [router])

  if (!analysis) return null

  const { receivable, actions, summary, needsMoreInput } = analysis.result

  /* 총 수령 예상액 — rule 엔진 합계(summary) 사용 */
  const totalMin = summary.totalReceivableMin
  const totalMax = summary.totalReceivableMax

  /* 필터링 */
  const filteredReceivable = filter === '복지제도' ? [] : receivable
  const filteredWelfare    = filter === '보험/환급' ? [] : welfare

  const FILTERS: Category[] = ['전체', '보험/환급', '복지제도']

  return (
    <div className="px-4 md:px-8 py-8 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-[#1E293B] mb-2">혜택 매처</h1>
      <p className="text-sm text-[#64748B] mb-8">받을 수 있는 보험금·환급금·복지제도를 한눈에 확인하세요.</p>

      {/* 총 수령액 카드 */}
      <div className="bg-gradient-to-r from-[#ECFDF5] to-[#D1FAE5] rounded-2xl border border-[#A7F3D0] p-6 mb-8">
        <div className="text-sm text-[#059669] font-medium mb-1">예상 총 수령 가능액</div>
        <div className="text-4xl font-bold font-mono text-[#10B981]">
          {fmt(totalMin)} ~ {fmt(totalMax)}
        </div>
        <div className="text-xs text-[#6EE7B7] mt-1">AI 분석 기준 · 실제 수령액은 상이할 수 있습니다</div>
      </div>

      {/* 필터 탭 */}
      <div className="flex gap-1 p-1 bg-[#F1F5F9] rounded-xl mb-6 w-fit">
        {FILTERS.map(f => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors
              ${filter === f ? 'bg-white text-[#1E293B] shadow-sm' : 'text-[#64748B] hover:text-[#1E293B]'}`}>
            {f}
          </button>
        ))}
      </div>

      {/* 보험/환급 항목 (분석 결과) */}
      {filteredReceivable.length > 0 && (
        <div className="mb-8">
          <h2 className="text-sm font-semibold text-[#475569] uppercase tracking-wide mb-3">
            보험금 · 환급금 ({filteredReceivable.length})
          </h2>
          <div className="space-y-3">
            {filteredReceivable.map((item: ReceivableItem, i) => {
              const key = `recv-${i}`
              const isOpen = expanded === key
              // 금액 미산정(null) 또는 NEEDS_MORE_INPUT → 금액 대신 '추가입력 필요' 표시
              const needsInput = item.status === 'NEEDS_MORE_INPUT' || item.estimatedMin == null
              return (
                <div key={key} className="bg-white rounded-2xl border border-[#E2E8F0] shadow-sm overflow-hidden">
                  <button className="w-full p-5 flex items-center gap-4 text-left hover:bg-[#F8FAFC] transition-colors"
                          onClick={() => setExpanded(isOpen ? null : key)}>
                    <div className="w-10 h-10 rounded-xl bg-[#ECFDF5] flex items-center justify-center flex-shrink-0 text-lg">
                      💰
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-semibold text-[#1E293B] mb-0.5">{item.name}</div>
                      <div className="text-xs text-[#64748B]">{item.source} · 기한: {item.deadline}</div>
                    </div>
                    <div className="text-right flex-shrink-0 mr-2">
                      {needsInput ? (
                        <span className="text-xs px-2 py-1 rounded-full border whitespace-nowrap"
                              style={{ background: NEEDS_MORE_INPUT_BADGE.bg, color: NEEDS_MORE_INPUT_BADGE.color, borderColor: NEEDS_MORE_INPUT_BADGE.border }}>
                          추가입력 필요
                        </span>
                      ) : (
                        <>
                          <div className="text-lg font-bold font-mono text-[#10B981]">{fmt(item.estimatedMin as number)}+</div>
                          {item.estimatedMax != null && (
                            <div className="text-xs text-[#94A3B8]">최대 {fmt(item.estimatedMax)}</div>
                          )}
                        </>
                      )}
                    </div>
                    {isOpen ? <ChevronUp size={16} className="text-[#94A3B8]" /> : <ChevronDown size={16} className="text-[#94A3B8]" />}
                  </button>

                  {isOpen && (
                    <div className="px-5 pb-5 border-t border-[#F1F5F9] pt-4 space-y-3">
                      {item.basis && (
                        <div>
                          <div className="text-xs font-medium text-[#64748B] mb-1">산정 근거</div>
                          <p className="text-sm text-[#475569]">{item.basis}</p>
                        </div>
                      )}
                      {item.requiredDocs.length > 0 && (
                        <div>
                          <div className="text-xs font-medium text-[#64748B] mb-2">필요 서류</div>
                          <div className="flex flex-wrap gap-2">
                            {item.requiredDocs.map((doc, j) => (
                              <span key={j} className="text-xs px-2.5 py-1 bg-[#F1F5F9] rounded-full text-[#475569]">
                                {doc}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                      {item.applyUrl && (
                        <a href={item.applyUrl} target="_blank" rel="noopener noreferrer"
                          className="inline-flex items-center gap-1.5 text-sm text-[#2563EB] hover:underline">
                          신청 바로가기 <ExternalLink size={13} />
                        </a>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* 추가 입력 필요 안내 */}
      {filter !== '복지제도' && needsMoreInput && needsMoreInput.length > 0 && (
        <div className="mb-8">
          <h2 className="text-sm font-semibold text-[#475569] uppercase tracking-wide mb-3">
            추가 입력이 필요한 혜택 ({needsMoreInput.length})
          </h2>
          <div className="bg-[#FFF7ED] border border-[#FFEDD5] rounded-2xl p-5 space-y-3">
            <p className="text-xs text-[#9A3412]">
              아래 정보를 입력하면 정확한 자격·예상 수령액을 계산할 수 있어요.
            </p>
            {needsMoreInput.map((item, i) => (
              <div key={i} className="bg-white rounded-xl border border-[#FFEDD5] p-4">
                <div className="font-medium text-[#1E293B] mb-2">{item.benefitName}</div>
                <div className="flex flex-wrap gap-2">
                  {item.missingInputs.map((field, j) => (
                    <span key={j} className="text-xs px-2.5 py-1 bg-[#FFF7ED] rounded-full text-[#EA580C] border border-[#FFEDD5]">
                      {field}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 행동 항목 */}
      {filter !== '복지제도' && actions.length > 0 && (
        <div className="mb-8">
          <h2 className="text-sm font-semibold text-[#475569] uppercase tracking-wide mb-3">
            즉시 행동 필요 ({actions.length})
          </h2>
          <div className="space-y-3">
            {actions.map((action, i) => {
              const badge = priorityBadge(action.priority)
              return (
                <div key={i} className="bg-white rounded-2xl border border-[#E2E8F0] p-5 shadow-sm flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-lg bg-[#F5F3FF]">
                    📋
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap mb-1">
                      <span className="font-semibold text-[#1E293B]">{action.name}</span>
                      <span className="text-xs px-2 py-0.5 rounded-full border"
                            style={{ background: badge.bg, color: badge.color, borderColor: badge.border }}>
                        {badge.label}
                      </span>
                    </div>
                    <div className="text-xs text-[#64748B] mb-2">기한: {action.deadline} · {action.contactInfo}</div>
                    {action.requiredDocs.length > 0 && (
                      <div className="flex flex-wrap gap-1">
                        {action.requiredDocs.map((doc, j) => (
                          <span key={j} className="text-xs px-2 py-0.5 bg-[#F1F5F9] rounded-full text-[#64748B]">{doc}</span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* 복지 제도 (공공 API) */}
      {filteredWelfare.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-[#475569] uppercase tracking-wide mb-3">
            정부 복지 제도 ({filteredWelfare.length})
          </h2>
          <div className="space-y-3">
            {filteredWelfare.map(item => {
              const isOpen = expanded === `wel-${item.id}`
              return (
                <div key={item.id} className="bg-white rounded-2xl border border-[#E2E8F0] shadow-sm overflow-hidden">
                  <button className="w-full p-5 flex items-center gap-4 text-left hover:bg-[#F8FAFC] transition-colors"
                          onClick={() => setExpanded(isOpen ? null : `wel-${item.id}`)}>
                    <div className="w-10 h-10 rounded-xl bg-[#EFF6FF] flex items-center justify-center flex-shrink-0 text-lg">
                      🏛️
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-semibold text-[#1E293B] mb-0.5 truncate">{item.serviceName}</div>
                      <div className="text-xs text-[#64748B]">{item.ministryName}</div>
                    </div>
                    {isOpen ? <ChevronUp size={16} className="text-[#94A3B8]" /> : <ChevronDown size={16} className="text-[#94A3B8]" />}
                  </button>
                  {isOpen && (
                    <div className="px-5 pb-5 border-t border-[#F1F5F9] pt-4 space-y-2">
                      <p className="text-sm text-[#475569]">{item.summary}</p>
                      {item.applyUrl && (
                        <a href={item.applyUrl} target="_blank" rel="noopener noreferrer"
                          className="inline-flex items-center gap-1.5 text-sm text-[#2563EB] hover:underline">
                          신청 바로가기 <ExternalLink size={13} />
                        </a>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

      {loadingWelfare && (
        <div className="text-center py-6 text-sm text-[#94A3B8]">복지 제도 불러오는 중...</div>
      )}

      {!loadingWelfare && filteredReceivable.length === 0 && filteredWelfare.length === 0
        && actions.length === 0 && !(needsMoreInput && needsMoreInput.length > 0) && (
        <div className="text-center py-16 text-[#94A3B8]">분석 결과 혜택 항목이 없습니다.</div>
      )}
    </div>
  )
}
