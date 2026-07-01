'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { CheckCircle, Loader2 } from 'lucide-react'
import { analysisApi } from '@/lib/api'
import { analysisStore, pendingAnalysisStore, buildSituationDescription, jobToPersona, CRISIS_KEY_MAP } from '@/lib/utils'
import MyDataSelector from '@/components/mydata/MyDataSelector'
import type { AnalysisRequest, ApplicantProfile, CrisisType, PersonaType } from '@/lib/types'

type Step = 1 | 2 | 3 | 4 | 5

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

const CARE_GRADE_OPTIONS = [
  { value: '',  label: '모름/미판정' },
  { value: '1', label: '1등급' },
  { value: '2', label: '2등급' },
  { value: '3', label: '3등급' },
  { value: '4', label: '4등급' },
  { value: '5', label: '5등급' },
]

/**
 * 위기 유형별로 규칙 엔진(BenefitRuleEngine)이 실제 소비하는 추가 입력.
 * 공통(긴급복지 생계지원)은 모든 위기에서 '가구 금융재산'을 요구하므로 별도로 항상 노출한다.
 * accident/bereavement는 규칙 엔진이 서류·조회 기반이라 수집할 프로필 필드가 없어 안내만 제공.
 */
const CRISIS_FIELD_INFO: Record<string, string> = {
  accident: '산재보험 급여는 업무상 재해 인정 여부와 평균임금 확인이 필요합니다. 재해 경위를 아래 상세 입력에 적어주세요.',
  bereavement: '안심상속 원스톱 서비스로 사망자의 금융재산을 조회한 뒤 수령액이 산정됩니다. 별도 입력 없이 진행할 수 있습니다.',
}

export default function DiagnosisPage() {
  const router = useRouter()
  const [step, setStep] = useState<Step>(1)
  const [crisis, setCrisis]  = useState('')
  const [form, setForm] = useState({
    job: 'employed', income: '', household: '1',
    assets: '',          // 가구 금융재산(만원) — 긴급복지 생계지원 자산기준
    medical: '',         // 연간 본인부담 의료비(만원) — 입원/수술
    insuranceMonths: '', // 고용보험 가입기간(개월) — 실직
    separation: '',      // 비자발적 이직 여부('yes'|'no') — 실직
    careGrade: '',       // 장기요양 등급(1~5) — 간병
  })
  const [filteredMyData, setFilteredMyData] = useState<Record<string, unknown>>({})
  const [detail, setDetail] = useState('')
  const [loadingMsg, setLoadingMsg] = useState('')
  const [error, setError] = useState('')

  const pick = (k: string, v: string) => setForm(f => ({ ...f, [k]: v }))
  const persona = jobToPersona(form.job) as PersonaType

  /** 만원 단위 입력 문자열 → 원 단위 정수(비었거나 숫자 아니면 undefined) */
  function manwonToWon(v: string): number | undefined {
    if (v === '') return undefined
    const n = Number(v)
    return Number.isFinite(n) ? n * 10_000 : undefined
  }

  /** 폼값 → applicantProfile (rule 엔진 입력). 입력된 값만 포함. */
  function buildApplicantProfile(): ApplicantProfile {
    const householdSize = parseInt(form.household, 10)   // '5이상' → 5
    const monthlyIncome = manwonToWon(form.income)
    const liquidFinancialAssets = manwonToWon(form.assets)   // 공통(긴급복지 생계지원)

    const p: ApplicantProfile = {
      ...(Number.isFinite(householdSize) ? { householdSize } : {}),
      ...(monthlyIncome !== undefined ? { monthlyIncome } : {}),
      ...(liquidFinancialAssets !== undefined ? { liquidFinancialAssets } : {}),
    }

    // 위기 유형별 추가 입력 — 규칙 엔진이 소비하는 필드만 조건부로 채운다.
    if (crisis === 'hospitalization') {
      const annualOutOfPocketMedical = manwonToWon(form.medical)
      if (annualOutOfPocketMedical !== undefined) p.annualOutOfPocketMedical = annualOutOfPocketMedical
    } else if (crisis === 'job-loss') {
      const months = parseInt(form.insuranceMonths, 10)
      if (Number.isFinite(months)) p.employmentInsuranceMonths = months
      if (form.separation === 'yes') p.involuntarySeparation = true
      else if (form.separation === 'no') p.involuntarySeparation = false
    } else if (crisis === 'caregiving') {
      const grade = parseInt(form.careGrade, 10)
      if (Number.isFinite(grade)) p.careGrade = grade
    }

    return p
  }

  async function runAnalysis() {
    setStep(5)
    setError('')
    // 위기유형·상황요약·신청자 프로필을 조립해 분석 요청(req)을 만든다.
    // catch에서 결제/로그인 게이팅 시 req를 보관하므로 try 밖에서 선언한다.
    const crisisType = CRISIS_KEY_MAP[crisis] as CrisisType
    const situation  = buildSituationDescription({
      crisisType,
      job: JOB_OPTIONS.find(o => o.value === form.job)?.label ?? form.job,
      income: form.income,
      household: form.household,
      detail,
    })
    const applicantProfile = buildApplicantProfile()
    const req: AnalysisRequest = {
      crisisType,
      situationDescription: situation,
      // 마이데이터 단계(MyDataSelector)에서 켜고 수정한 항목만 전달 (비어있으면 생략)
      ...(Object.keys(filteredMyData).length > 0 ? { filteredMyData } : {}),
      ...(Object.keys(applicantProfile).length > 0 ? { applicantProfile } : {}),
    }

    try {
      setLoadingMsg('AI가 상황을 분석하는 중...')
      const result = await analysisApi.recommend(req)

      setLoadingMsg('결과를 정리하는 중...')
      analysisStore.save(result)
      pendingAnalysisStore.clear()
      router.push('/dashboard')
    } catch (err) {
      const status = (err as { status?: number }).status
      // 유료: 미로그인 → 로그인, 미결제 → 결제(페이월). 요청을 보관해 결제/로그인 후 즉시 재실행.
      if (status === 401 || status === 402) {
        pendingAnalysisStore.save(req)
        router.push(status === 401 ? '/login' : '/unlock')
        return
      }
      setError(err instanceof Error ? err.message : '분석 중 오류가 발생했습니다.')
      setStep(4)
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      {/* 스텝 인디케이터 */}
      {step < 5 && (
        <div className="flex items-center gap-2 mb-10">
          {([1, 2, 3, 4] as const).map(n => (
            <div key={n} className="flex items-center gap-2">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-colors
                ${step > n ? 'bg-[#10B981] text-white' : step === n ? 'bg-[#2563EB] text-white' : 'bg-[#E2E8F0] text-[#94A3B8]'}`}>
                {step > n ? <CheckCircle size={16} /> : n}
              </div>
              <span className={`text-sm ${step === n ? 'text-[#1E293B] font-medium' : 'text-[#94A3B8]'}`}>
                {['위기 유형', '재정 정보', '마이데이터', '상세 입력'][n - 1]}
              </span>
              {n < 4 && <div className="w-8 h-px bg-[#E2E8F0]" />}
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

            {/* 공통: 긴급복지 생계지원 자산기준 */}
            <div>
              <label className="block text-sm font-medium text-[#1E293B] mb-1.5">가구 금융재산 (만원)</label>
              <input type="number" value={form.assets} onChange={e => pick('assets', e.target.value)}
                placeholder="예: 500 (예적금·현금 등 합계)"
                className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition" />
              <p className="text-xs text-[#94A3B8] mt-1">긴급복지 생계지원 자격 판정에 사용됩니다.</p>
            </div>

            {/* 위기 유형별 추가 입력 */}
            {crisis === 'hospitalization' && (
              <div>
                <label className="block text-sm font-medium text-[#1E293B] mb-1.5">최근 1년 본인부담 의료비 (만원)</label>
                <input type="number" value={form.medical} onChange={e => pick('medical', e.target.value)}
                  placeholder="예: 300 (비급여 제외, 건강보험 본인부담금)"
                  className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition" />
                <p className="text-xs text-[#94A3B8] mt-1">본인부담상한제 환급액 산정에 사용됩니다.</p>
              </div>
            )}

            {crisis === 'job-loss' && (
              <div className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-[#1E293B] mb-1.5">고용보험 가입기간 (개월)</label>
                  <input type="number" value={form.insuranceMonths} onChange={e => pick('insuranceMonths', e.target.value)}
                    placeholder="예: 18 (이직 전 사업장 기준)"
                    className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition" />
                  <p className="text-xs text-[#94A3B8] mt-1">실업급여 자격(180일 이상)·금액 산정에 사용됩니다.</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#1E293B] mb-2">비자발적 이직 여부</label>
                  <div className="grid grid-cols-2 gap-2">
                    {[{ v: 'yes', l: '비자발적 (권고사직·해고·폐업 등)' }, { v: 'no', l: '자발적 (자진 퇴사)' }].map(({ v, l }) => (
                      <button key={v} type="button" onClick={() => pick('separation', v)}
                        className={`py-2.5 px-4 rounded-lg border text-sm font-medium transition-colors text-left
                          ${form.separation === v ? 'border-[#2563EB] bg-[#EFF6FF] text-[#2563EB]' : 'border-[#E2E8F0] text-[#475569] hover:border-[#DBEAFE]'}`}>
                        {l}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {crisis === 'caregiving' && (
              <div>
                <label className="block text-sm font-medium text-[#1E293B] mb-1.5">장기요양 등급</label>
                <select value={form.careGrade} onChange={e => pick('careGrade', e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition bg-white">
                  {CARE_GRADE_OPTIONS.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
                </select>
                <p className="text-xs text-[#94A3B8] mt-1">노인장기요양보험 재가급여 월 지원액 산정에 사용됩니다.</p>
              </div>
            )}

            {CRISIS_FIELD_INFO[crisis] && (
              <div className="px-4 py-3 bg-[#F0F9FF] border border-[#BAE6FD] rounded-lg text-sm text-[#0369A1]">
                {CRISIS_FIELD_INFO[crisis]}
              </div>
            )}
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

      {/* Step 3: 마이데이터 항목 선택/미리보기/수정 */}
      {step === 3 && (
        <div>
          <h2 className="text-2xl font-bold text-[#1E293B] mb-2">마이데이터를 확인해주세요</h2>
          <p className="text-[#64748B] mb-8">분석에 포함할 항목을 켜고, 핵심 수치를 직접 수정할 수 있습니다.</p>
          <MyDataSelector persona={persona} onChange={setFilteredMyData} />
          <div className="flex gap-3 mt-8">
            <button onClick={() => setStep(2)}
              className="flex-1 py-3 border border-[#E2E8F0] rounded-xl text-sm font-medium text-[#475569] hover:bg-[#F8FAFC] transition-colors">
              이전
            </button>
            <button onClick={() => setStep(4)}
              className="flex-1 py-3 bg-[#2563EB] text-white rounded-xl text-sm font-semibold hover:bg-[#1D4ED8] transition-colors">
              다음
            </button>
          </div>
        </div>
      )}

      {/* Step 4: 상세 상황 */}
      {step === 4 && (
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
            <button onClick={() => setStep(3)}
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

      {/* Step 5: AI 분석 중 */}
      {step === 5 && (
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
