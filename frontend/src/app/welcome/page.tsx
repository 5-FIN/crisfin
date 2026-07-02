'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { BookOpen, Zap, MapPin, LayoutDashboard, ArrowRight, Check } from 'lucide-react'
import BrandMark from '@/components/BrandMark'

/** 온보딩 스텝 정의 */
const STEPS = [
  {
    icon: BookOpen,
    title: 'CrisFin에 오신 걸 환영해요',
    desc: '입원·사고·실직 같은 갑작스러운 위기에서 놓치는 보험금·환급금·지원금을 찾아드려요.',
    points: ['무료 길라잡이로 위기별 핵심 정보 확인', 'AI 맞춤 분석으로 내 상황 정리'],
  },
  {
    icon: Zap,
    title: '3분이면 충분해요',
    desc: '위기 유형을 고르고 재정 상황을 입력하면, AI가 4가지로 정리해드려요.',
    points: ['위기 유형 선택 (입원·사고·실직·간병·사망)', '할 일 · 받을 돈 · 미룰 것 · 행동으로 정리'],
  },
  {
    icon: MapPin,
    title: '내 지역 맞춤 복지',
    desc: '지역(시/도·시/군/구)을 설정하면 그 지역의 복지 제도를 추천해드려요.',
    points: ['가입 시 지역을 넣었다면 바로 맞춤 추천', '설정 페이지에서 언제든 변경 가능'],
  },
  {
    icon: LayoutDashboard,
    title: '대시보드 한눈에',
    desc: '분석이 끝나면 대시보드에서 대응 현황을 4분면으로 확인해요.',
    points: ['✅ 할 일 · 💰 받을 돈', '⏸️ 미룰 것 · 📋 행동'],
  },
]

export default function WelcomePage() {
  const router = useRouter()
  const [step, setStep] = useState(0)
  const isLast = step === STEPS.length - 1

  function finish(dest: string) {
    try { localStorage.setItem('cf_onboarded', '1') } catch { /* 무시 */ }
    router.push(dest)
  }

  const s = STEPS[step]
  const Icon = s.icon

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col">
      {/* 상단바 */}
      <header className="h-14 flex items-center justify-between px-4 md:px-8">
        <div className="flex items-center gap-2">
          <BrandMark size={32} />
          <span className="font-bold text-[#1E293B]">CrisFin</span>
        </div>
        <button onClick={() => finish('/diagnosis')} className="text-xs text-[#94A3B8] hover:text-[#64748B]">
          건너뛰기
        </button>
      </header>

      {/* 본문 */}
      <div className="flex-1 flex items-center justify-center px-4">
        <div className="w-full max-w-md">
          <div className="bg-white rounded-2xl border border-[#E2E8F0] shadow-sm p-8 text-center">
            <div className="w-16 h-16 rounded-2xl bg-[#EFF6FF] flex items-center justify-center mx-auto mb-6">
              <Icon size={30} className="text-[#2563EB]" />
            </div>
            <h1 className="text-xl font-bold text-[#1E293B] mb-2">{s.title}</h1>
            <p className="text-sm text-[#64748B] leading-relaxed mb-6">{s.desc}</p>

            <ul className="space-y-2.5 text-left mb-2">
              {s.points.map(p => (
                <li key={p} className="flex items-start gap-2.5 text-sm text-[#475569]">
                  <Check size={16} className="text-[#10B981] flex-shrink-0 mt-0.5" />
                  <span>{p}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* 진행 점 */}
          <div className="flex items-center justify-center gap-2 my-6">
            {STEPS.map((_, i) => (
              <span key={i}
                className={`h-2 rounded-full transition-all ${i === step ? 'w-6 bg-[#2563EB]' : 'w-2 bg-[#CBD5E1]'}`} />
            ))}
          </div>

          {/* 액션 */}
          {isLast ? (
            <div className="space-y-2">
              <button onClick={() => finish('/diagnosis')}
                className="w-full py-3 bg-[#2563EB] text-white font-semibold rounded-xl hover:bg-[#1D4ED8] transition-colors text-sm flex items-center justify-center gap-1.5">
                3분 분석 시작하기 <ArrowRight size={16} />
              </button>
              <button onClick={() => finish('/guide')}
                className="w-full py-3 bg-white border border-[#E2E8F0] text-[#475569] font-medium rounded-xl hover:bg-[#F8FAFC] transition-colors text-sm">
                먼저 무료 길라잡이 둘러보기
              </button>
            </div>
          ) : (
            <button onClick={() => setStep(step + 1)}
              className="w-full py-3 bg-[#2563EB] text-white font-semibold rounded-xl hover:bg-[#1D4ED8] transition-colors text-sm flex items-center justify-center gap-1.5">
              다음 <ArrowRight size={16} />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
