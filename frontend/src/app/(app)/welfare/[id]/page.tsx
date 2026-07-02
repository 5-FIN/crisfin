'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft, ExternalLink, Loader2, Building2, MapPin, Star } from 'lucide-react'
import { welfareApi } from '@/lib/api'
import { CRISIS_LABELS } from '@/lib/utils'
import type { WelfareBenefitResponse } from '@/lib/types'

/** "[라벨] 내용" 줄들을 라벨/내용 섹션으로 파싱 */
function parseDetail(detail: string): { label: string; content: string }[] {
  return detail
    .split('\n')
    .map(line => {
      const m = line.match(/^\s*\[(.+?)\]\s*(.*)$/)
      return m ? { label: m[1], content: m[2] } : { label: '', content: line }
    })
    .filter(s => s.content.trim() !== '')
}

export default function WelfareDetailPage() {
  const params = useParams()
  const router = useRouter()
  const id = Number(params?.id)

  const [item, setItem] = useState<WelfareBenefitResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [isFav, setIsFav] = useState(false)

  useEffect(() => {
    if (!id) return
    welfareApi.get(id)
      .then(setItem)
      .catch(() => setError('복지 제도를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
    // 로그인 상태면 즐겨찾기 여부 확인(비로그인/실패는 조용히 무시)
    welfareApi.favorites()
      .then(list => setIsFav(list.some(f => f.id === id)))
      .catch(() => {})
  }, [id])

  async function toggleFav() {
    const next = !isFav
    setIsFav(next) // 낙관적 토글
    try {
      if (next) await welfareApi.addFavorite(id)
      else await welfareApi.removeFavorite(id)
    } catch {
      setIsFav(!next) // 실패 시 롤백
    }
  }

  if (loading) {
    return <div className="flex justify-center py-24"><Loader2 size={28} className="text-[#2563EB] animate-spin" /></div>
  }

  if (error || !item) {
    return (
      <div className="px-4 md:px-8 py-8 max-w-2xl mx-auto">
        <button onClick={() => router.back()} className="text-sm text-[#64748B] hover:text-[#2563EB] flex items-center gap-1 mb-6">
          <ArrowLeft size={15} /> 뒤로
        </button>
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-600">
          {error || '복지 제도를 찾을 수 없습니다.'}
        </div>
      </div>
    )
  }

  const region = [item.ctpvNm, item.sggNm].filter(Boolean).join(' ')
  const sections = item.detailContent ? parseDetail(item.detailContent) : []

  return (
    <div className="px-4 md:px-8 py-8 max-w-2xl mx-auto">
      <button onClick={() => router.back()} className="text-sm text-[#64748B] hover:text-[#2563EB] flex items-center gap-1 mb-6">
        <ArrowLeft size={15} /> 뒤로
      </button>

      {/* 헤더 */}
      <div className="flex items-start gap-3 mb-3">
        <div className="w-11 h-11 rounded-xl bg-[#EFF6FF] flex items-center justify-center flex-shrink-0 text-xl">🏛️</div>
        <div className="min-w-0 flex-1">
          <h1 className="text-xl font-bold text-[#1E293B] leading-snug">{item.serviceName}</h1>
          <div className="flex items-center gap-2 flex-wrap mt-1.5 text-xs text-[#64748B]">
            {item.ministryName && (
              <span className="flex items-center gap-1"><Building2 size={12} />{item.ministryName}</span>
            )}
            {region && (
              <span className="flex items-center gap-1 px-1.5 py-0.5 rounded-full bg-[#EFF6FF] text-[#2563EB] border border-[#DBEAFE]">
                <MapPin size={11} />{region}
              </span>
            )}
          </div>
        </div>
        <button onClick={toggleFav} title={isFav ? '즐겨찾기 해제' : '즐겨찾기'}
          className={`flex-shrink-0 p-2 rounded-lg transition-colors ${isFav ? 'text-[#F59E0B]' : 'text-[#CBD5E1] hover:text-[#F59E0B]'}`}>
          <Star size={20} fill={isFav ? 'currentColor' : 'none'} />
        </button>
      </div>

      {/* 위기유형 태그 */}
      {item.crisisTags?.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-5">
          {item.crisisTags.map(t => (
            <span key={t} className="text-xs px-2 py-0.5 rounded-full bg-[#F5F3FF] text-[#7C3AED] border border-[#EDE9FE]">
              {CRISIS_LABELS[t] ?? t}
            </span>
          ))}
        </div>
      )}

      {/* 요약 */}
      {item.summary && (
        <p className="text-sm text-[#475569] bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl p-4 mb-5 leading-relaxed">
          {item.summary}
        </p>
      )}

      {/* 상세 본문 (인제스트한 개요/지원대상/선정기준/지원내용/신청방법) */}
      {sections.length > 0 ? (
        <div className="space-y-4 mb-6">
          {sections.map((s, i) => (
            <div key={i}>
              {s.label && <div className="text-xs font-semibold text-[#2563EB] mb-1">{s.label}</div>}
              <p className="text-sm text-[#1E293B] leading-relaxed whitespace-pre-wrap">{s.content}</p>
            </div>
          ))}
        </div>
      ) : (
        // 상세 본문이 없으면 목록 필드로 대체
        <div className="space-y-4 mb-6">
          {item.targetDescription && <Field label="지원 대상/지역" value={item.targetDescription} />}
          {item.selectionCriteria && <Field label="관심 주제" value={item.selectionCriteria} />}
          {item.applyMethod && <Field label="신청 방법" value={item.applyMethod} />}
        </div>
      )}

      {/* 연락처 */}
      {item.contact && <Field label="문의" value={item.contact} />}

      {/* 신청 링크 */}
      {item.applyUrl && (
        <a href={item.applyUrl} target="_blank" rel="noopener noreferrer"
          className="mt-6 inline-flex items-center justify-center gap-1.5 w-full py-2.5 bg-[#2563EB] text-white font-semibold rounded-lg hover:bg-[#1D4ED8] transition-colors text-sm">
          신청 페이지로 이동 <ExternalLink size={14} />
        </a>
      )}
    </div>
  )
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs font-semibold text-[#64748B] mb-1">{label}</div>
      <p className="text-sm text-[#1E293B] leading-relaxed whitespace-pre-wrap">{value}</p>
    </div>
  )
}
