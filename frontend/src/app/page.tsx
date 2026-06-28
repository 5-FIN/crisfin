import Link from 'next/link'
import { ArrowRight, Shield, Clock, TrendingUp, CheckCircle } from 'lucide-react'

const CRISIS_TYPES = [
  { emoji: '🏥', label: '입원/수술' },
  { emoji: '🚑', label: '사고/재해' },
  { emoji: '💼', label: '실직/소득단절' },
  { emoji: '🩺', label: '간병' },
  { emoji: '🕊️', label: '가족 사망' },
]

const STEPS = [
  { num: '01', title: '위기 유형 선택', desc: '5가지 위기 상황 중 해당하는 유형을 선택합니다.' },
  { num: '02', title: 'AI 분석', desc: '재정 데이터를 바탕으로 AI가 상황을 분석합니다.' },
  { num: '03', title: '액션 플랜 수령', desc: '할 일·받을 돈·미룰 것·행동 4가지로 정리된 플랜을 받습니다.' },
]

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* 네브바 */}
      <nav className="sticky top-0 z-10 bg-white/90 backdrop-blur border-b border-[#E2E8F0]">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-[#2563EB] flex items-center justify-center text-white text-sm font-bold">C</div>
            <span className="font-bold text-[#1E293B] text-lg">CrisFin</span>
          </div>
          <div className="flex items-center gap-3">
            <Link href="/login" className="text-sm text-[#475569] hover:text-[#1E293B] transition-colors">
              로그인
            </Link>
            <Link
              href="/diagnosis"
              className="px-4 py-2 bg-[#2563EB] text-white text-sm font-medium rounded-lg hover:bg-[#1D4ED8] transition-colors"
            >
              무료 시작하기
            </Link>
          </div>
        </div>
      </nav>

      {/* 히어로 */}
      <section className="max-w-6xl mx-auto px-6 pt-20 pb-16 text-center">
        <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-[#EFF6FF] rounded-full text-xs font-medium text-[#2563EB] mb-6">
          <span className="w-1.5 h-1.5 rounded-full bg-[#2563EB]" />
          AI 기반 위기 금융 분석 서비스
        </div>
        <h1 className="text-4xl md:text-5xl font-bold text-[#1E293B] leading-tight mb-6">
          위기 상황, 지금 당장<br />
          <span className="text-[#2563EB]">뭘 해야 하는지</span> 알려드립니다
        </h1>
        <p className="text-lg text-[#64748B] mb-10 max-w-2xl mx-auto">
          입원·실직·사고 같은 갑작스러운 위기에서 놓치는 보험금, 환급금, 지원금을
          AI가 3분 만에 찾아드립니다. 11.2조원의 미청구 보험금, 이제 내 것을 챙기세요.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            href="/diagnosis"
            className="inline-flex items-center justify-center gap-2 px-8 py-4 bg-[#2563EB] text-white font-semibold rounded-xl hover:bg-[#1D4ED8] transition-colors text-base"
          >
            3분 무료 분석 시작 <ArrowRight size={18} />
          </Link>
          <Link
            href="/login"
            className="inline-flex items-center justify-center gap-2 px-8 py-4 bg-white text-[#1E293B] font-semibold rounded-xl border-2 border-[#E2E8F0] hover:border-[#2563EB] hover:text-[#2563EB] transition-colors text-base"
          >
            로그인하고 이어하기
          </Link>
        </div>
      </section>

      {/* 통계 */}
      <section className="bg-[#F9FAFB] py-12 border-y border-[#E2E8F0]">
        <div className="max-w-6xl mx-auto px-6 grid grid-cols-1 sm:grid-cols-3 gap-8 text-center">
          {[
            { icon: TrendingUp, value: '580만원+', label: '사용자 평균 혜택', color: '#10B981' },
            { icon: Clock,      value: '47일',     label: '평균 생존 기간 연장', color: '#2563EB' },
            { icon: Shield,     value: '3분',      label: 'AI 분석 소요 시간', color: '#8B5CF6' },
          ].map(({ icon: Icon, value, label, color }) => (
            <div key={label} className="flex flex-col items-center gap-2">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center mb-1"
                   style={{ background: color + '15' }}>
                <Icon size={24} style={{ color }} />
              </div>
              <div className="text-3xl font-bold font-mono" style={{ color }}>{value}</div>
              <div className="text-sm text-[#64748B]">{label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* 4분면 미리보기 */}
      <section className="max-w-6xl mx-auto px-6 py-16">
        <div className="text-center mb-10">
          <h2 className="text-2xl font-bold text-[#1E293B] mb-3">분석 결과, 이렇게 정리됩니다</h2>
          <p className="text-[#64748B]">복잡한 금융 상황을 4가지로 명확하게 구분</p>
        </div>
        <div className="grid grid-cols-2 gap-4 max-w-2xl mx-auto">
          {[
            { label: '할 일',   sub: '당장 해야 할 액션',  color: '#F59E0B', bg: '#FFFBEB', emoji: '✅' },
            { label: '받을 돈', sub: '청구 가능한 혜택',   color: '#10B981', bg: '#ECFDF5', emoji: '💰' },
            { label: '미룰 것', sub: '유예 가능한 납부',   color: '#2563EB', bg: '#EFF6FF', emoji: '⏸️' },
            { label: '행동',    sub: '제출 서류·연락처',   color: '#8B5CF6', bg: '#F5F3FF', emoji: '📋' },
          ].map(({ label, sub, color, bg, emoji }) => (
            <div key={label} className="rounded-2xl p-6 border border-[#E2E8F0]"
                 style={{ background: bg }}>
              <div className="text-2xl mb-3">{emoji}</div>
              <div className="font-bold text-[#1E293B] mb-1">{label}</div>
              <div className="text-xs text-[#64748B]">{sub}</div>
            </div>
          ))}
        </div>
      </section>

      {/* 위기 유형 */}
      <section className="bg-[#F9FAFB] py-16 border-y border-[#E2E8F0]">
        <div className="max-w-6xl mx-auto px-6">
          <h2 className="text-2xl font-bold text-[#1E293B] text-center mb-8">이런 상황에서 도와드립니다</h2>
          <div className="flex flex-wrap justify-center gap-4">
            {CRISIS_TYPES.map(({ emoji, label }) => (
              <div key={label}
                   className="flex items-center gap-3 px-5 py-3 bg-white rounded-xl border border-[#E2E8F0] shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all cursor-pointer">
                <span className="text-2xl">{emoji}</span>
                <span className="font-medium text-[#1E293B]">{label}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 3단계 프로세스 */}
      <section className="max-w-6xl mx-auto px-6 py-16">
        <h2 className="text-2xl font-bold text-[#1E293B] text-center mb-10">3단계면 충분합니다</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {STEPS.map(({ num, title, desc }) => (
            <div key={num} className="flex flex-col items-center text-center p-6 rounded-2xl border border-[#E2E8F0] bg-white hover:shadow-md transition-shadow">
              <div className="w-12 h-12 rounded-xl bg-[#EFF6FF] flex items-center justify-center text-[#2563EB] font-bold text-lg mb-4">
                {num}
              </div>
              <div className="font-bold text-[#1E293B] mb-2">{title}</div>
              <div className="text-sm text-[#64748B] leading-relaxed">{desc}</div>
            </div>
          ))}
        </div>
      </section>

      {/* CTA 배너 */}
      <section className="mx-6 mb-16 rounded-2xl overflow-hidden"
               style={{ background: 'linear-gradient(135deg, #2563EB 0%, #7C3AED 100%)' }}>
        <div className="max-w-6xl mx-auto px-8 py-12 flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="text-white">
            <div className="text-xl font-bold mb-2">지금 바로 시작하세요</div>
            <div className="text-white/80 text-sm">로그인 없이 3분 무료 분석 가능합니다.</div>
          </div>
          <Link
            href="/diagnosis"
            className="flex-shrink-0 px-8 py-3 bg-white text-[#2563EB] font-bold rounded-xl hover:bg-[#EFF6FF] transition-colors flex items-center gap-2"
          >
            무료 분석 시작 <ArrowRight size={16} />
          </Link>
        </div>
      </section>

      {/* 푸터 */}
      <footer className="border-t border-[#E2E8F0] py-8 text-center text-xs text-[#94A3B8]">
        © 2026 CrisFin (FIN5). 본 서비스는 참고용이며 전문가 상담을 권장합니다.
      </footer>
    </div>
  )
}
