'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { CheckCircle, Loader2 } from 'lucide-react'
import { analysisApi, myDataApi } from '@/lib/api'
import { analysisStore, buildSituationDescription, jobToPersona, CRISIS_KEY_MAP } from '@/lib/utils'
import type { CrisisType, PersonaType } from '@/lib/types'

type Step = 1 | 2 | 3 | 4

const CRISIS_OPTIONS = [
  { key: 'hospitalization', label: '입원/수술',     emoji: '🏥', desc: '갑작스러운 입원·수술' },
  { key: 'accident',        label: '사고/재해',     emoji: '🚑', desc: '교통사고·산업재해 등' },
  { key: 'job-loss',        label: '실직/소득단절', emoji: '💼', desc: '권고사직·폐업·계약만료' },
  { key: 'caregiving',      label: '간병',          emoji: '🩺', desc: '가족 장기요양 부담' },
  { key: 'bereavement',     label: '가족 사망',     emoji: '🕊️', desc: '유족 금융 정리' },
]

const JOB_OPTIONS = [
  { value: 'employed',      label: '직장인' },
  { value: 'self_employed', label: '자영업자' },
  { value: 'freelancer',    label: '프리랜서' },
  { value: 'laid_off',      label: '실직/권고사직' },
  { value: 'public',        label: '공무원' },
]

const FIELD_KEYS = ['cards', 'loans', 'insurances', 'autoTransfers', 'income']

export default function DiagnosisPage() {
  const router = useRouter()
  const [step, setStep] = useState<Step>(1)
  const [crisis, setCrisis]  = useState('')
  const [form, setForm] = useState({ job: 'employed', income: '', household: '1', expenses: '' })
  const [detail, setDetail] = useState('')
  const [loadingMsg, setLoadingMsg] = useState('')
  const [error, setError] = useState('')

  const pick = (k: string, v: string) => setForm(f => ({ ...f, [k]: v }))

  async function runAnalysis() {
    setStep(4)
    setError('')
    try {
      const crisisType = CRISIS_KEY_MAP[crisis] as CrisisType
      const persona    = jobToPersona(form.job) as PersonaType

      setLoadingMsg('재정 데이터를 불러오는 중...')
      let filteredMyData: Record<string, unknown> | undefined
      try {
        filteredMyData = await myDataApi.filter(persona, FIELD_KEYS)
      } catch {
        // mydata 실패해도 분석은 계속
      }

      setLoadingMsg('AI가 상황을 분석하는 중...')
      const situation = buildSituationDescription({
        crisisType,
        job: JOB_OPTIONS.find(o => o.value === form.job)?.label ?? form.job,
        income: form.income,
        household: form.household,
        detail,
      })

      const result = await analysisApi.recommend({
        crisisType,
        situationDescription: situation,
        filteredMyData,
      })

      setLoadingMsg('결과를 정리하는 중...')
      analysisStore.save(result)
      router.push('/dashboard')
    } catch (err) {
      setError(err instanceof Error ? err.message : '분석 중 오류가 발생했습니다.')
      setStep(3)
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      {/* 스텝 인디케이터 */}
      {step < 4 && (
        <div className="flex items-center gap-2 mb-10">
          {([1, 2, 3] as const).map(n => (
            <div key={n} className="flex items-center gap-2">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-colors
                ${step > n ? 'bg-[#10B981] text-white' : step === n ? 'bg-[#2563EB] text-white' : 'bg-[#E2E8F0] text-[#94A3B8]'}`}>
                {step > n ? <CheckCircle size={16} /> : n}
              </div>
              <span className={`text-sm ${step === n ? 'text-[#1E293B] font-medium' : 'text-[#94A3B8]'}`}>
                {['위기 유형', '재정 정보', '상세 입력'][n - 1]}
              </span>
              {n < 3 && <div className="w-8 h-px bg-[#E2E8F0]" />}
            </div>
          ))}
        </div>
      )}

      {/* Step 1: 위기 유형 선택 */}
      {step === 1 && (
        <div>
          <h2 className="text-2xl font-bold text-[#1E293B] mb-2">어떤 위기 상황인가요?</h2>
          <p className="text-[#64748B] mb-8">해당하는 위기 유형을 선택해주세요.</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {CRISIS_OPTIONS.map(({ key, label, emoji, desc }) => (
              <button
                key={key}
                onClick={() => { setCrisis(key); setStep(2) }}
                className={`p-5 rounded-xl border-2 text-left transition-all hover:shadow-md hover:-translate-y-0.5
                  ${crisis === key ? 'border-[#2563EB] bg-[#EFF6FF]' : 'border-[#E2E8F0] bg-white hover:border-[#DBEAFE]'}`}
              >
                <div className="text-3xl mb-3">{emoji}</div>
                <div className="font-semibold text-[#1E293B] mb-1">{label}</div>
                <div className="text-xs text-[#64748B]">{desc}</div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Step 2: 기초 재정 정보 */}
      {step === 2 && (
        <div>
          <h2 className="text-2xl font-bold text-[#1E293B] mb-2">기본 재정 정보</h2>
          <p className="text-[#64748B] mb-8">정확한 분석을 위해 간략한 정보를 입력해주세요.</p>
          <div className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-[#1E293B] mb-2">직업 유형</label>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {JOB_OPTIONS.map(({ value, label }) => (
                  <button key={value} onClick={() => pick('job', value)}
                    className={`py-2.5 px-4 rounded-lg border text-sm font-medium transition-colors
                      ${form.job === value ? 'border-[#2563EB] bg-[#EFF6FF] text-[#2563EB]' : 'border-[#E2E8F0] text-[#475569] hover:border-[#DBEAFE]'}`}>
                    {label}
                  </button>
                ))}
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-[#1E293B] mb-1.5">월 소득 (만원)</label>
                <input type="number" value={form.income} onChange={e => pick('income', e.target.value)}
                  placeholder="예: 300"
                  className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition" />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#1E293B] mb-1.5">가구원 수</label>
                <select value={form.household} onChange={e => pick('household', e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition bg-white">
                  {['1', '2', '3', '4', '5이상'].map(v => <option key={v} value={v}>{v}인</option>)}
                </select>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-[#1E293B] mb-1.5">월 고정 지출 (만원, 선택)</label>
              <input type="number" value={form.expenses} onChange={e => pick('expenses', e.target.value)}
                placeholder="예: 200"
                className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition" />
            </div>
          </div>
          <div className="flex gap-3 mt-8">
            <button onClick={() => setStep(1)}
              className="flex-1 py-3 border border-[#E2E8F0] rounded-xl text-sm font-medium text-[#475569] hover:bg-[#F8FAFC] transition-colors">
              이전
            </button>
            <button onClick={() => setStep(3)}
              className="flex-1 py-3 bg-[#2563EB] text-white rounded-xl text-sm font-semibold hover:bg-[#1D4ED8] transition-colors">
              다음
            </button>
          </div>
        </div>
      )}

      {/* Step 3: 상세 상황 */}
      {step === 3 && (
        <div>
          <h2 className="text-2xl font-bold text-[#1E293B] mb-2">상황을 자세히 알려주세요</h2>
          <p className="text-[#64748B] mb-8">구체적일수록 더 정확한 분석이 가능합니다.</p>
          <textarea
            value={detail}
            onChange={e => setDetail(e.target.value)}
            placeholder={`예: 지난주 출근길 교통사고로 6주 입원 진단을 받았습니다. 현재 실손보험 가입되어 있고, 다음 달 카드값이 걱정됩니다.`}
            rows={6}
            maxLength={500}
            className="w-full px-4 py-3 rounded-xl border border-[#E2E8F0] text-sm resize-none focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition"
          />
          <div className="text-right text-xs text-[#94A3B8] mt-1">{detail.length}/500</div>
          {error && (
            <div className="mt-3 px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-600">{error}</div>
          )}
          <div className="flex gap-3 mt-6">
            <button onClick={() => setStep(2)}
              className="flex-1 py-3 border border-[#E2E8F0] rounded-xl text-sm font-medium text-[#475569] hover:bg-[#F8FAFC] transition-colors">
              이전
            </button>
            <button onClick={runAnalysis} disabled={!detail.trim()}
              className="flex-1 py-3 bg-[#2563EB] text-white rounded-xl text-sm font-semibold hover:bg-[#1D4ED8] disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              AI 분석 시작
            </button>
          </div>
        </div>
      )}

      {/* Step 4: AI 분석 중 */}
      {step === 4 && (
        <div className="text-center py-16">
          <div className="w-20 h-20 rounded-full bg-[#EFF6FF] flex items-center justify-center mx-auto mb-6">
            <Loader2 size={36} className="text-[#2563EB] animate-spin" />
          </div>
          <h2 className="text-2xl font-bold text-[#1E293B] mb-3">AI가 분석 중입니다</h2>
          <p className="text-[#64748B] mb-8">{loadingMsg}</p>
          <div className="max-w-xs mx-auto space-y-2">
            {['재정 데이터 수집', 'AI 위기 분석', '맞춤 액션 플랜 생성'].map((msg, i) => (
              <div key={msg} className="flex items-center gap-3 px-4 py-3 bg-[#F8FAFC] rounded-lg">
                <div className={`w-5 h-5 rounded-full flex items-center justify-center text-xs
                  ${loadingMsg.includes('결과') && i < 3 ? 'bg-[#10B981] text-white' :
                    loadingMsg.includes('분석') && i < 2 ? 'bg-[#10B981] text-white' :
                    loadingMsg.includes('불러') && i < 1 ? 'bg-[#10B981] text-white' :
                    'bg-[#E2E8F0]'}`}>
                  {i + 1}
                </div>
                <span className="text-sm text-[#475569]">{msg}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
