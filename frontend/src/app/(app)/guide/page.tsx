'use client'

import { useState } from 'react'
import { Loader2, Copy, Check, CheckCircle, BookOpen, ArrowLeft } from 'lucide-react'
import { guideApi } from '@/lib/api'
import { CRISIS_LABELS, CRISIS_EMOJI, cn } from '@/lib/utils'
import type { CrisisType, GuideResponse } from '@/lib/types'

const CRISIS_OPTIONS: { key: CrisisType; desc: string }[] = [
  { key: 'HOSPITALIZATION', desc: '갑작스러운 입원·수술' },
  { key: 'ACCIDENT',        desc: '교통사고·산업재해 등' },
  { key: 'UNEMPLOYMENT',    desc: '권고사직·폐업·계약만료' },
  { key: 'CAREGIVING',      desc: '가족 장기요양 부담' },
  { key: 'BEREAVEMENT',     desc: '유족 금융 정리' },
]

export default function GuidePage() {
  const [selected, setSelected] = useState<CrisisType | null>(null)
  const [guide, setGuide] = useState<GuideResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)

  async function handleSelect(crisisType: CrisisType) {
    setSelected(crisisType)
    setGuide(null)
    setError('')
    setLoading(true)
    try {
      const result = await guideApi.get(crisisType)
      setGuide(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : '정보를 불러오는 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function handleCopy() {
    if (!guide) return
    try {
      await navigator.clipboard.writeText(guide.coachingPrompt)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // clipboard API 실패 시 무시
    }
  }

  function handleReset() {
    setSelected(null)
    setGuide(null)
    setError('')
    setCopied(false)
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      {/* 헤더 */}
      <div className="flex items-center gap-3 mb-2">
        <div className="w-10 h-10 rounded-xl bg-[#EFF6FF] flex items-center justify-center">
          <BookOpen size={20} className="text-[#2563EB]" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-[#1E293B]">무료 정보 길라잡이</h1>
          <p className="text-sm text-[#64748B]">로그인 없이 위기 유형별 핵심 정보와 AI 프롬프트를 제공합니다</p>
        </div>
      </div>

      {/* 위기 유형 선택 (가이드 결과가 없을 때만 표시) */}
      {!guide && !loading && (
        <div className="mt-8">
          <h2 className="text-base font-semibold text-[#1E293B] mb-4">어떤 위기 상황인가요?</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {CRISIS_OPTIONS.map(({ key, desc }) => (
              <button
                key={key}
                onClick={() => handleSelect(key)}
                className={cn(
                  'p-5 rounded-xl border-2 text-left transition-all hover:shadow-md hover:-translate-y-0.5',
                  selected === key && !error
                    ? 'border-[#2563EB] bg-[#EFF6FF]'
                    : 'border-[#E2E8F0] bg-white hover:border-[#DBEAFE]',
                )}
              >
                <div className="text-3xl mb-3">{CRISIS_EMOJI[key]}</div>
                <div className="font-semibold text-[#1E293B] mb-1">{CRISIS_LABELS[key]}</div>
                <div className="text-xs text-[#64748B]">{desc}</div>
              </button>
            ))}
          </div>

          {/* 에러 */}
          {error && (
            <div className="mt-4 px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-600">
              {error}
            </div>
          )}
        </div>
      )}

      {/* 로딩 */}
      {loading && (
        <div className="mt-16 flex flex-col items-center gap-4 text-center">
          <div className="w-16 h-16 rounded-full bg-[#EFF6FF] flex items-center justify-center">
            <Loader2 size={28} className="text-[#2563EB] animate-spin" />
          </div>
          <p className="text-[#64748B] text-sm">정보를 불러오는 중...</p>
        </div>
      )}

      {/* 가이드 결과 */}
      {guide && !loading && (
        <div className="mt-8 space-y-5">
          {/* 제목 */}
          <div className="flex items-center gap-2">
            <span className="text-2xl">{CRISIS_EMOJI[guide.crisisType]}</span>
            <h2 className="text-xl font-bold text-[#1E293B]">{guide.title}</h2>
          </div>

          {/* coachingPrompt 박스 */}
          <div className="rounded-xl border border-[#E2E8F0] bg-[#F8FAFC] overflow-hidden">
            <div className="flex items-center justify-between px-4 py-3 border-b border-[#E2E8F0] bg-white">
              <span className="text-sm font-semibold text-[#1E293B]">AI 프롬프트</span>
              <button
                onClick={handleCopy}
                className={cn(
                  'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors',
                  copied
                    ? 'bg-[#ECFDF5] text-[#059669] border border-[#D1FAE5]'
                    : 'bg-[#EFF6FF] text-[#2563EB] border border-[#DBEAFE] hover:bg-[#DBEAFE]',
                )}
              >
                {copied ? (
                  <><Check size={13} />복사됨 ✓</>
                ) : (
                  <><Copy size={13} />복사</>
                )}
              </button>
            </div>
            <div className="px-4 py-4">
              <p className="text-xs text-[#64748B] mb-3">
                이 프롬프트를 ChatGPT·Claude 등 본인 AI에 붙여넣으세요.
              </p>
              <pre className="text-sm text-[#1E293B] whitespace-pre-wrap leading-relaxed font-sans">
                {guide.coachingPrompt}
              </pre>
            </div>
          </div>

          {/* 핵심 수칙 카드 */}
          {guide.keyRules.length > 0 && (
            <div className="rounded-xl border border-[#E2E8F0] bg-white overflow-hidden">
              <div className="px-4 py-3 border-b border-[#E2E8F0]">
                <span className="text-sm font-semibold text-[#1E293B]">핵심 수칙</span>
              </div>
              <ul className="px-4 py-3 space-y-2.5">
                {guide.keyRules.map((rule, i) => (
                  <li key={i} className="flex items-start gap-2.5">
                    <CheckCircle size={16} className="text-[#2563EB] mt-0.5 flex-shrink-0" />
                    <span className="text-sm text-[#1E293B] leading-relaxed">{rule}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* 근거 법령 */}
          {guide.sourceLaws.length > 0 && (
            <div>
              <p className="text-xs font-semibold text-[#64748B] mb-2">근거 법령</p>
              <div className="flex flex-wrap gap-2">
                {guide.sourceLaws.map((law, i) => (
                  <span
                    key={i}
                    className="px-2.5 py-1 bg-[#F8FAFC] border border-[#E2E8F0] rounded-full text-xs text-[#475569]"
                  >
                    {law}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* 면책 고지 */}
          {guide.disclaimer && (
            <div className="px-4 py-3 bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl">
              <p className="text-xs text-[#94A3B8] leading-relaxed">{guide.disclaimer}</p>
            </div>
          )}

          {/* 다른 위기 보기 */}
          <button
            onClick={handleReset}
            className="flex items-center gap-2 text-sm text-[#2563EB] hover:text-[#1D4ED8] font-medium transition-colors"
          >
            <ArrowLeft size={15} />
            다른 위기 보기
          </button>
        </div>
      )}
    </div>
  )
}
