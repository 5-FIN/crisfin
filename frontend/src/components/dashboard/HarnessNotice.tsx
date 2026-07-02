'use client'

import { useState } from 'react'
import { ShieldCheck, ShieldAlert, ChevronDown } from 'lucide-react'
import type { HarnessFlag } from '@/lib/types'

/**
 * AI 응답 검증(할루시네이션 하네스) 결과를 사용자에게 노출한다.
 * - HARD: 검증되지 않은 정보가 감지되어 조정/제거됨 → 눈에 띄게 경고
 * - SOFT: 자격 미확정·단정 표현 등 주의 → 완만한 안내
 * 플래그가 없으면 "AI 응답 검증 통과" 배지만 간단히 보여준다.
 */
export default function HarnessNotice({ flags }: { flags?: HarnessFlag[] }) {
  const [open, setOpen] = useState(false)

  const list = flags ?? []
  const hard = list.filter(f => f.severity === 'HARD')
  const soft = list.filter(f => f.severity === 'SOFT')

  // 검증 통과 — 신뢰 배지
  if (list.length === 0) {
    return (
      <div className="flex items-center gap-2 text-xs text-[#059669] bg-[#ECFDF5] border border-[#D1FAE5] rounded-lg px-3 py-2 mb-4">
        <ShieldCheck size={14} />
        <span>AI 응답 검증 통과 — 금액은 규칙 엔진이 산정했고, 검증되지 않은 연락처·수치는 발견되지 않았습니다.</span>
      </div>
    )
  }

  const tone = hard.length > 0
    ? { bg: '#FFFBEB', border: '#FEF3C7', color: '#D97706' }
    : { bg: '#EFF6FF', border: '#DBEAFE', color: '#2563EB' }

  return (
    <div className="rounded-lg border mb-4" style={{ background: tone.bg, borderColor: tone.border }}>
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center gap-2 px-3 py-2.5 text-left"
        style={{ color: tone.color }}
      >
        <ShieldAlert size={15} className="flex-shrink-0" />
        <span className="text-xs font-medium flex-1">
          {hard.length > 0
            ? `AI 응답에서 검증되지 않은 정보 ${hard.length}건이 감지되어 조정했습니다.`
            : `참고할 주의사항 ${soft.length}건이 있습니다.`}
          {soft.length > 0 && hard.length > 0 && ` (주의 ${soft.length}건 포함)`}
        </span>
        <ChevronDown size={14} className={`flex-shrink-0 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <ul className="px-3 pb-3 space-y-1.5">
          {[...hard, ...soft].map((f, i) => (
            <li key={i} className="flex items-start gap-2 text-xs text-[#475569]">
              <span
                className="mt-0.5 px-1.5 py-0.5 rounded text-[10px] font-semibold flex-shrink-0"
                style={f.severity === 'HARD'
                  ? { background: '#FEF2F2', color: '#DC2626' }
                  : { background: '#F1F5F9', color: '#64748B' }}
              >
                {f.severity === 'HARD' ? (f.action === 'STRIPPED' ? '제거됨' : '경고') : '주의'}
              </span>
              <span>{f.message}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
