'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Loader2, Star, ExternalLink } from 'lucide-react'
import { welfareApi } from '@/lib/api'
import type { WelfareBenefitResponse } from '@/lib/types'

export default function FavoritesPage() {
  const [items, setItems] = useState<WelfareBenefitResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  function load() {
    setLoading(true)
    welfareApi.favorites()
      .then(setItems)
      .catch(() => setError('즐겨찾기를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  async function remove(id: number) {
    setItems(prev => prev.filter(x => x.id !== id)) // 낙관적 제거
    try { await welfareApi.removeFavorite(id) } catch { load() /* 실패 시 재동기화 */ }
  }

  return (
    <div className="px-4 md:px-8 py-10 max-w-3xl mx-auto">
      <h1 className="text-3xl font-bold text-[#1E293B] mb-2">즐겨찾기</h1>
      <p className="text-sm text-[#64748B] mb-8">저장해 둔 복지 제도를 모아봤어요.</p>

      {error && (
        <div className="mb-6 px-3.5 py-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600">{error}</div>
      )}

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 size={26} className="text-[#2563EB] animate-spin" /></div>
      ) : items.length === 0 ? (
        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-12 text-center">
          <Star size={28} className="text-[#CBD5E1] mx-auto mb-3" />
          <p className="text-sm text-[#64748B]">아직 즐겨찾기한 복지 제도가 없어요.</p>
          <Link href="/benefits" className="mt-4 inline-block text-sm text-[#2563EB] hover:underline">혜택 매처에서 찾아보기 →</Link>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map(item => (
            <div key={item.id} className="bg-white rounded-2xl border border-[#E2E8F0] shadow-sm p-5 flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-[#EFF6FF] flex items-center justify-center flex-shrink-0 text-lg">🏛️</div>
              <div className="flex-1 min-w-0">
                <Link href={`/welfare/${item.id}`} className="font-semibold text-[#1E293B] hover:text-[#2563EB] truncate block">
                  {item.serviceName}
                </Link>
                <div className="flex items-center gap-1.5 flex-wrap mt-0.5">
                  <span className="text-xs text-[#64748B]">{item.ministryName}</span>
                  {(item.ctpvNm || item.sggNm) && (
                    <span className="text-xs px-1.5 py-0.5 rounded-full bg-[#EFF6FF] text-[#2563EB] border border-[#DBEAFE]">
                      {[item.ctpvNm, item.sggNm].filter(Boolean).join(' ')}
                    </span>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-3 flex-shrink-0">
                <Link href={`/welfare/${item.id}`} className="text-xs text-[#2563EB] hover:underline hidden sm:flex items-center gap-1">
                  상세 <ExternalLink size={12} />
                </Link>
                <button onClick={() => remove(item.id)} title="즐겨찾기 해제"
                  className="text-[#F59E0B] hover:text-[#D97706]">
                  <Star size={20} fill="currentColor" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
