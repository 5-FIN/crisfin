'use client'

import { useState } from 'react'
import { BookMarked, ChevronDown } from 'lucide-react'
import type { Citation } from '@/lib/types'

const SOURCE_LABEL: Record<string, string> = {
  WELFARE: '복지제도',
  GUIDE: '길라잡이',
}

/**
 * 이 분석이 근거로 삼은 공식 문서(RAG 검색 결과)를 노출한다. "GPT 붙인 챗봇"이 아니라
 * 근거 기반 서비스임을 사용자에게 투명하게 보여주는 역할. 근거가 없으면 렌더하지 않는다.
 */
export default function Citations({ citations }: { citations?: Citation[] }) {
  const [open, setOpen] = useState(false)
  const list = citations ?? []
  if (list.length === 0) return null

  return (
    <div className="mt-6 rounded-xl border border-[#E2E8F0] bg-white">
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center gap-2 px-4 py-3 text-left text-[#475569]"
      >
        <BookMarked size={15} className="text-[#2563EB] flex-shrink-0" />
        <span className="text-sm font-medium flex-1">
          분석 근거 {list.length}건 — 공식 문서 기반(RAG)
        </span>
        <ChevronDown size={14} className={`flex-shrink-0 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <ul className="px-4 pb-4 space-y-2">
          {list.map((c, i) => (
            <li key={i} className="flex items-start gap-2 text-xs text-[#64748B]">
              <span className="mt-0.5 px-1.5 py-0.5 rounded text-[10px] font-semibold flex-shrink-0 bg-[#EFF6FF] text-[#2563EB]">
                {SOURCE_LABEL[c.sourceType] ?? c.sourceType}
              </span>
              <span className="leading-relaxed">{c.snippet}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
