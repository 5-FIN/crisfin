'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Search, Loader2, Star } from 'lucide-react'
import { welfareApi } from '@/lib/api'
import { CRISIS_LABELS } from '@/lib/utils'
import { SIDO_LIST } from '@/lib/regions'
import type { WelfareBenefitResponse, PageResponse } from '@/lib/types'

const SORTS = [
  { value: 'recent', label: '최신순' },
  { value: 'name', label: '가나다순' },
]

export default function WelfareBrowsePage() {
  const [input, setInput] = useState('')       // 검색창 입력값
  const [query, setQuery] = useState('')        // 실제 적용된 검색어(제출 시)
  const [region, setRegion] = useState('')
  const [sort, setSort] = useState('recent')
  const [page, setPage] = useState(0)

  const [data, setData] = useState<PageResponse<WelfareBenefitResponse> | null>(null)
  const [loading, setLoading] = useState(true)
  const [favSet, setFavSet] = useState<Set<number>>(new Set())

  // 목록 조회 (검색어/지역/정렬/페이지 변경 시)
  useEffect(() => {
    setLoading(true)
    welfareApi.list({ keyword: query || undefined, ctpvNm: region || undefined, sort, page, size: 20 })
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [query, region, sort, page])

  // 즐겨찾기 id 집합(로그인 상태에서만)
  useEffect(() => {
    welfareApi.favorites().then(list => setFavSet(new Set(list.map(f => f.id)))).catch(() => {})
  }, [])

  async function toggleFav(id: number) {
    const has = favSet.has(id)
    setFavSet(prev => { const n = new Set(prev); has ? n.delete(id) : n.add(id); return n }) // 낙관적
    try { has ? await welfareApi.removeFavorite(id) : await welfareApi.addFavorite(id) }
    catch { setFavSet(prev => { const n = new Set(prev); has ? n.add(id) : n.delete(id); return n }) }
  }

  function onSearch(e: React.FormEvent) {
    e.preventDefault()
    setPage(0)
    setQuery(input.trim())
  }

  const items = data?.content ?? []

  return (
    <div className="px-4 md:px-8 py-8 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-[#1E293B] mb-2">복지 찾기</h1>
      <p className="text-sm text-[#64748B] mb-6">전국 복지 제도를 검색·필터해서 찾아보세요.</p>

      {/* 검색/필터 바 */}
      <div className="flex flex-col sm:flex-row gap-2 mb-6">
        <form onSubmit={onSearch} className="flex-1 relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#94A3B8]" />
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            placeholder="복지 제도 이름·내용 검색"
            className="w-full pl-9 pr-3 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] placeholder-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
          />
        </form>
        <select value={region} onChange={e => { setRegion(e.target.value); setPage(0) }}
          className="px-3 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] focus:outline-none focus:ring-2 focus:ring-[#2563EB]">
          <option value="">전체 지역</option>
          {SIDO_LIST.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
        <select value={sort} onChange={e => { setSort(e.target.value); setPage(0) }}
          className="px-3 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] focus:outline-none focus:ring-2 focus:ring-[#2563EB]">
          {SORTS.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 size={26} className="text-[#2563EB] animate-spin" /></div>
      ) : items.length === 0 ? (
        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-12 text-center text-sm text-[#64748B]">
          검색 결과가 없습니다.
        </div>
      ) : (
        <>
          <div className="text-xs text-[#94A3B8] mb-3">총 {data?.totalElements ?? 0}건</div>
          <div className="space-y-3">
            {items.map(item => (
              <div key={item.id} className="bg-white rounded-2xl border border-[#E2E8F0] shadow-sm p-5 flex items-center gap-4">
                <div className="w-10 h-10 rounded-xl bg-[#EFF6FF] flex items-center justify-center flex-shrink-0 text-lg">🏛️</div>
                <div className="flex-1 min-w-0">
                  <Link href={`/welfare/${item.id}`} className="font-semibold text-[#1E293B] hover:text-[#2563EB] truncate block">
                    {item.serviceName}
                  </Link>
                  <div className="flex items-center gap-1.5 flex-wrap mt-1">
                    <span className="text-xs text-[#64748B]">{item.ministryName}</span>
                    {(item.ctpvNm || item.sggNm) && (
                      <span className="text-xs px-1.5 py-0.5 rounded-full bg-[#EFF6FF] text-[#2563EB] border border-[#DBEAFE]">
                        {[item.ctpvNm, item.sggNm].filter(Boolean).join(' ')}
                      </span>
                    )}
                    {item.crisisTags?.slice(0, 2).map(t => (
                      <span key={t} className="text-xs px-1.5 py-0.5 rounded-full bg-[#F5F3FF] text-[#7C3AED] border border-[#EDE9FE]">
                        {CRISIS_LABELS[t] ?? t}
                      </span>
                    ))}
                  </div>
                </div>
                <button onClick={() => toggleFav(item.id)} title={favSet.has(item.id) ? '즐겨찾기 해제' : '즐겨찾기'}
                  className={`flex-shrink-0 p-2 rounded-lg transition-colors ${favSet.has(item.id) ? 'text-[#F59E0B]' : 'text-[#CBD5E1] hover:text-[#F59E0B]'}`}>
                  <Star size={18} fill={favSet.has(item.id) ? 'currentColor' : 'none'} />
                </button>
              </div>
            ))}
          </div>

          {(data?.totalPages ?? 0) > 1 && (
            <div className="flex items-center justify-center gap-4 mt-6">
              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                className="px-3 py-1.5 text-sm rounded-lg border border-[#E2E8F0] text-[#475569] hover:bg-[#F8FAFC] disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                이전
              </button>
              <span className="text-xs text-[#94A3B8]">{(data?.number ?? 0) + 1} / {data?.totalPages}</span>
              <button onClick={() => setPage(p => Math.min((data?.totalPages ?? 1) - 1, p + 1))} disabled={page >= (data?.totalPages ?? 1) - 1}
                className="px-3 py-1.5 text-sm rounded-lg border border-[#E2E8F0] text-[#475569] hover:bg-[#F8FAFC] disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
