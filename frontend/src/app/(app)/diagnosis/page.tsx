'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { CheckCircle, Loader2 } from 'lucide-react'
import { analysisApi } from '@/lib/api'
import { analysisStore, pendingAnalysisStore, buildSituationDescription, jobToPersona, CRISIS_KEY_MAP } from '@/lib/utils'
import MyDataSelector from '@/components/mydata/MyDataSelector'
import PaymentModal from '@/components/payment/PaymentModal'
import type { AnalysisRequest, ApplicantProfile, CrisisType, PersonaType } from '@/lib/types'

type Step = 1 | 2 | 3 | 4 | 5
type Obj = Record<string, unknown>

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

/**
 * 위기 유형별로 마이데이터에서 자동으로 불러오는 값을 사용자에게 미리 안내.
 * 손입력을 없애고 다음 단계(마이데이터)에서 채워지는 항목을 명시한다.
 * accident/bereavement는 규칙 엔진이 서류·조회 기반이라 별도 값 없이 안내만 제공.
 */
const CRISIS_FIELD_INFO: Record<string, string> = {
  hospitalization: '연간 본인부담 의료비(본인부담상한제 환급 산정)를 마이데이터에서 자동으로 불러옵니다.',
  'job-loss': '고용보험 가입기간·이직 사유(실업급여 산정)를 마이데이터에서 자동으로 불러옵니다.',
  caregiving: '장기요양 등급(장기요양급여 산정)을 마이데이터에서 자동으로 불러옵니다.',
  accident: '산재보험 급여는 업무상 재해 인정 여부와 평균임금 확인이 필요합니다. 재해 경위를 이후 상세 입력에 적어주세요.',
  bereavement: '안심상속 원스톱 서비스로 사망자의 금융재산을 조회한 뒤 수령액이 산정됩니다. 별도 입력 없이 진행할 수 있습니다.',
}

/** unknown → 유한 숫자 또는 undefined */
function num(v: unknown): number | undefined {
  return typeof v === 'number' && Number.isFinite(v) ? v : undefined
}

/** 마이데이터 계좌 잔액 합계 → 가구 금융재산(원) */
function sumBankBalance(md: Obj): number | undefined {
  const accts = md.bankAccounts
  if (!Array.isArray(accts)) return undefined
  return accts.reduce((s: number, a) => s + (num((a as Obj)?.balance) ?? 0), 0)
}

export default function DiagnosisPage() {
  const router = useRouter()
  const [step, setStep] = useState<Step>(1)
  const [crisis, setCrisis]  = useState('')
  // 손입력은 마이데이터로 대체 불가능한 최소값(직업·가구원 수)만 유지.
  // 월 소득·금융재산·위기별 값은 모두 마이데이터(mock)에서 자동으로 채운다.
  const [form, setForm] = useState({ job: 'employed', household: '1' })
  const [filteredMyData, setFilteredMyData] = useState<Obj>({})
  const [detail, setDetail] = useState('')
  const [loadingMsg, setLoadingMsg] = useState('')
  const [error, setError] = useState('')
  // 유료 게이팅(402) 시 페이지 이동 대신 결제 모달을 띄우고, 결제 후 이 요청을 제자리에서 재실행한다.
  const [payReq, setPayReq] = useState<AnalysisRequest | null>(null)

  const pick = (k: string, v: string) => setForm(f => ({ ...f, [k]: v }))
  const persona = jobToPersona(form.job) as PersonaType

  /**
   * applicantProfile(rule 엔진 입력) 조립.
   * householdSize만 손입력이고, 나머지 재정·위기별 값은 마이데이터에서 파생한다.
   */
  function buildApplicantProfile(): ApplicantProfile {
    const householdSize = parseInt(form.household, 10)   // '5이상' → 5
    const md = filteredMyData
    const monthlyIncome = num(md.monthlyIncome)                 // 마이데이터: 소득
    const liquidFinancialAssets = sumBankBalance(md)            // 마이데이터: 계좌 잔액 합계
    const pub = (md.publicData ?? {}) as Obj                    // 마이데이터: 공공데이터

    const p: ApplicantProfile = {
      ...(Number.isFinite(householdSize) ? { householdSize } : {}),
      ...(monthlyIncome !== undefined ? { monthlyIncome } : {}),
      ...(liquidFinancialAssets !== undefined ? { liquidFinancialAssets } : {}),
    }

    // 위기 유형별 값도 마이데이터 publicData에서 파생 — 규칙 엔진이 소비하는 필드만 채운다.
    if (crisis === 'hospitalization') {
      const v = num(pub.annualOutOfPocketMedical)
      if (v !== undefined) p.annualOutOfPocketMedical = v
    } else if (crisis === 'job-loss') {
      const m = num(pub.employmentInsuranceMonths)
      if (m !== undefined) p.employmentInsuranceMonths = m
      if (typeof pub.involuntarySeparation === 'boolean') p.involuntarySeparation = pub.involuntarySeparation
    } else if (crisis === 'caregiving') {
      const g = num(pub.careGrade)
      if (g !== undefined) p.careGrade = g
    }

    return p
  }

  async function runAnalysis() {
    setError('')
    // 위기유형·상황요약·신청자 프로필을 조립해 분석 요청(req)을 만든다.
    const crisisType = CRISIS_KEY_MAP[crisis] as CrisisType
    // 소득은 마이데이터에서 파생(원 → 만원 표기)해 상황 요약 텍스트에 반영.
    const monthlyIncome = num(filteredMyData.monthlyIncome)
    const incomeManwon  = monthlyIncome !== undefined ? String(Math.round(monthlyIncome / 10_000)) : ''
    const situation  = buildSituationDescription({
      crisisType,
      job: JOB_OPTIONS.find(o => o.value === form.job)?.label ?? form.job,
      income: incomeManwon,
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

    submit(req)
  }

  /** 분석 요청을 실행한다. 402(미결제)면 결제 모달을 열어 제자리에서 이어가고, 401은 로그인으로. */
  async function submit(req: AnalysisRequest) {
    setStep(5)
    setError('')
    try {
      setLoadingMsg('AI가 상황을 분석하는 중...')
      const result = await analysisApi.recommend(req)

      setLoadingMsg('결과를 정리하는 중...')
      analysisStore.save(result)
      pendingAnalysisStore.clear()
      router.push('/dashboard')
    } catch (err) {
      const status = (err as { status?: number }).status
      // 요청을 보관해 결제/로그인 후 즉시 재실행할 수 있게 한다.
      if (status === 401) {
        pendingAnalysisStore.save(req)
        router.push('/login')
        return
      }
      if (status === 402) {
        // 미결제 → 결제 모달을 띄우고 입력 화면(step 4)으로 복귀. 결제하면 제자리에서 이어 실행.
        pendingAnalysisStore.save(req)
        setPayReq(req)
        setStep(4)
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
                {['위기 유형', '기본 정보', '마이데이터', '상세 입력'][n - 1]}
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

      {/* Step 2: 기본 정보 (마이데이터로 못 가져오는 최소값만) */}
      {step === 2 && (
        <div>
          <h2 className="text-2xl font-bold text-[#1E293B] mb-2">기본 정보</h2>
          <p className="text-[#64748B] mb-8">소득·재산 등 재정 정보는 다음 단계의 마이데이터에서 자동으로 불러옵니다. 여기서는 최소한만 입력해주세요.</p>
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
            <div>
              <label className="block text-sm font-medium text-[#1E293B] mb-1.5">가구원 수</label>
              <select value={form.household} onChange={e => pick('household', e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] transition bg-white">
                {['1', '2', '3', '4', '5이상'].map(v => <option key={v} value={v}>{v}인</option>)}
              </select>
              <p className="text-xs text-[#94A3B8] mt-1">긴급복지 생계지원 기준(중위소득) 산정에 사용됩니다.</p>
            </div>

            {/* 위기 유형별로 마이데이터에서 자동으로 불러오는 값 안내 */}
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
          <p className="text-[#64748B] mb-8">분석에 포함할 항목을 켜고, 핵심 수치를 직접 수정할 수 있습니다. 소득·계좌·위기별 정보는 여기서 자동으로 불러옵니다.</p>
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

      {/* 결제 모달 (미결제 게이팅 시) — 결제하면 방금 입력한 분석을 제자리에서 이어 실행 */}
      {payReq && (
        <PaymentModal
          onPaid={() => { const r = payReq; setPayReq(null); submit(r) }}
          onClose={() => setPayReq(null)}
        />
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
