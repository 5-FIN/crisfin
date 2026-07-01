'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { X, Loader2, Sparkles } from 'lucide-react'
import { analysisApi, paymentApi } from '@/lib/api'
import { analysisStore } from '@/lib/utils'
import type { AnalysisResultResponse, ApplicantProfile } from '@/lib/types'

/** 개인화 재분석 입력 필드 — 비우면 부모 분석값을 상속 */
const PROFILE_FIELDS: { key: keyof ApplicantProfile; label: string; placeholder: string }[] = [
  { key: 'householdSize',         label: '가구원 수',       placeholder: '예: 3' },
  { key: 'monthlyIncome',         label: '월 소득(원)',     placeholder: '예: 2500000' },
  { key: 'age',                   label: '나이',            placeholder: '예: 42' },
  { key: 'liquidFinancialAssets', label: '유동 자산(원)',   placeholder: '예: 5000000' },
]

interface Props {
  analysis: AnalysisResultResponse
  onClose: () => void
  onUpdated: (result: AnalysisResultResponse) => void
}

/**
 * 기존 분석 결과를 바탕으로 개인화 재분석(reinfer)을 실행하는 모달.
 * 상황 설명 + 일부 재정 정보만 조정해 다시 추론한다(생략한 필드는 상속).
 * 유료 기능이므로 실행 전 이용권(entitlement)을 확인해 게이팅한다.
 */
export default function ReinferModal({ analysis, onClose, onUpdated }: Props) {
  const router = useRouter()
  const [situation, setSituation] = useState(analysis.situationDescription ?? '')
  const [profile, setProfile] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  function setField(key: string, value: string) {
    setProfile(p => ({ ...p, [key]: value }))
  }

  /** 입력한 숫자 필드만 모아 ApplicantProfile로 변환(빈 값은 상속되도록 생략) */
  function buildProfile(): ApplicantProfile | undefined {
    const out: Record<string, number> = {}
    for (const { key } of PROFILE_FIELDS) {
      const raw = profile[key]
      if (raw != null && raw.trim() !== '') {
        const n = Number(raw)
        if (!Number.isNaN(n)) out[key] = n
      }
    }
    return Object.keys(out).length > 0 ? (out as ApplicantProfile) : undefined
  }

  async function handleSubmit() {
    setError('')
    setLoading(true)
    try {
      // 결제 게이팅: 이용권이 없으면 재분석 호출 전에 결제 페이지로 유도
      const ent = await paymentApi.entitlement()
      if (!ent.active) {
        router.push('/unlock')
        return
      }

      const updated = await analysisApi.reinfer(analysis.id, {
        situationDescription: situation.trim() || undefined,
        applicantProfile: buildProfile(),
      })
      analysisStore.save(updated)
      onUpdated(updated)
      onClose()
    } catch (err) {
      const status = (err as Error & { status?: number }).status
      if (status === 401) { router.push('/login'); return }
      if (status === 402) { router.push('/unlock'); return }
      setError(err instanceof Error ? err.message : '재분석 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40" onClick={loading ? undefined : onClose} />
      <div className="relative w-full max-w-md bg-white rounded-2xl shadow-xl p-6 max-h-[90vh] overflow-y-auto">
        <button onClick={onClose} disabled={loading}
          className="absolute top-4 right-4 text-[#94A3B8] hover:text-[#1E293B] disabled:opacity-50">
          <X size={18} />
        </button>

        <div className="flex items-center gap-2 mb-1">
          <div className="w-9 h-9 rounded-xl bg-[#F5F3FF] flex items-center justify-center">
            <Sparkles size={18} className="text-[#8B5CF6]" />
          </div>
          <h2 className="text-lg font-bold text-[#1E293B]">개인화 재분석</h2>
        </div>
        <p className="text-xs text-[#64748B] mb-5">바뀐 상황·재정 정보만 수정해 다시 분석합니다. 비운 항목은 기존 분석값이 유지됩니다.</p>

        <label className="block text-sm font-medium text-[#1E293B] mb-1.5">상황 설명</label>
        <textarea
          value={situation}
          onChange={e => setSituation(e.target.value)}
          rows={3}
          maxLength={500}
          className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] placeholder-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#8B5CF6] focus:border-transparent transition resize-none mb-4"
        />

        <div className="grid grid-cols-2 gap-3 mb-4">
          {PROFILE_FIELDS.map(({ key, label, placeholder }) => (
            <div key={key}>
              <label className="block text-xs font-medium text-[#475569] mb-1">{label}</label>
              <input
                type="number"
                inputMode="numeric"
                value={profile[key] ?? ''}
                onChange={e => setField(key, e.target.value)}
                placeholder={placeholder}
                className="w-full px-3 py-2 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] placeholder-[#CBD5E1] focus:outline-none focus:ring-2 focus:ring-[#8B5CF6] focus:border-transparent transition"
              />
            </div>
          ))}
        </div>

        {error && (
          <div className="px-3.5 py-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600 mb-4">
            {error}
          </div>
        )}

        <button
          onClick={handleSubmit}
          disabled={loading}
          className="w-full py-2.5 bg-[#8B5CF6] text-white font-semibold rounded-lg hover:bg-[#7C3AED] disabled:opacity-60 disabled:cursor-not-allowed transition-colors text-sm flex items-center justify-center gap-2"
        >
          {loading ? (<><Loader2 size={16} className="animate-spin" />분석 중...</>) : '재분석 실행'}
        </button>
      </div>
    </div>
  )
}
