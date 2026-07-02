import Link from 'next/link'
import { ArrowRight, Shield, Clock, TrendingUp, CheckCircle, BookOpen, Scale, Database, ShieldCheck } from 'lucide-react'
import BrandMark from '@/components/BrandMark'

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
          <Link href="/" className="flex items-center gap-2">
            <BrandMark size={32} />
            <span className="font-bold text-[#1E293B] text-lg">CrisFin</span>
          </Link>
          <div className="flex items-center gap-3">
            <Link href="/login" className="text-sm text-[#475569] hover:text-[#1E293B] transition-colors">
              로그인
            </Link>
            <Link
              href="/signup"
              className="px-4 py-2 bg-[#2563EB] text-white text-sm font-medium rounded-lg hover:bg-[#1D4ED8] transition-colors"
            >
              시작하기
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
            href="/signup"
            className="inline-flex items-center justify-center gap-2 px-8 py-4 bg-[#2563EB] text-white font-semibold rounded-xl hover:bg-[#1D4ED8] transition-colors text-base"
          >
            3분 만에 분석 시작 <ArrowRight size={18} />
          </Link>
          <Link
            href="/guide"
            className="inline-flex items-center justify-center gap-2 px-8 py-4 bg-white text-[#1E293B] font-semibold rounded-xl border-2 border-[#E2E8F0] hover:border-[#2563EB] hover:text-[#2563EB] transition-colors text-base"
          >
            <BookOpen size={18} />무료 길라잡이 바로가기
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
              <div className="text-3xl font-bold tabular-nums" style={{ color }}>{value}</div>
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
            <div className="text-white/80 text-sm">가입 후 3분 만에 위기 금융 분석을 받아보세요.</div>
          </div>
          <Link
            href="/signup"
            className="flex-shrink-0 px-8 py-3 bg-white text-[#2563EB] font-bold rounded-xl hover:bg-[#EFF6FF] transition-colors flex items-center gap-2"
          >
            지금 시작하기 <ArrowRight size={16} />
          </Link>
        </div>
      </section>

      {/* 푸터 */}
      <footer className="border-t border-[#E2E8F0] bg-[#F9FAFB]">
        <div className="max-w-6xl mx-auto px-6 py-12">
          {/* 신뢰 배지 3종 */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-10">
            {[
              { icon: Scale, title: '공식 근거 기반', desc: '보건복지부·복지로 등 공공 제도와 관련 법령·행정 안내문을 근거로 안내합니다.' },
              { icon: Database, title: '복지로 1,000+ 제도', desc: '전국 공식 복지 제도 데이터를 정기 동기화해 위기·지역별로 매칭합니다.' },
              { icon: ShieldCheck, title: 'AI + 규칙 엔진 이중 검증', desc: '금액은 AI가 아닌 공식 산정식(규칙 엔진)으로 계산하고, 응답은 할루시네이션 하네스로 검증합니다.' },
            ].map(({ icon: Icon, title, desc }) => (
              <div key={title} className="flex items-start gap-3">
                <div className="w-9 h-9 rounded-lg bg-[#EFF6FF] flex items-center justify-center flex-shrink-0">
                  <Icon size={18} className="text-[#2563EB]" />
                </div>
                <div>
                  <div className="text-sm font-semibold text-[#1E293B] mb-0.5">{title}</div>
                  <div className="text-xs text-[#64748B] leading-relaxed">{desc}</div>
                </div>
              </div>
            ))}
          </div>

          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6 pt-8 border-t border-[#E2E8F0]">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <BrandMark size={28} />
                <span className="font-bold text-[#1E293B]">CrisFin</span>
              </div>
              <p className="text-xs text-[#94A3B8] max-w-md leading-relaxed">
                갑작스러운 금융 위기에서 지금 당장 해야 할 행동을 공식 근거 기반으로 정리해 드립니다.
              </p>
            </div>
            <nav className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
              <Link href="/guide" className="text-[#475569] hover:text-[#2563EB] transition-colors">무료 길라잡이</Link>
              <Link href="/login" className="text-[#475569] hover:text-[#2563EB] transition-colors">로그인</Link>
              <Link href="/signup" className="text-[#475569] hover:text-[#2563EB] transition-colors">분석 시작</Link>
            </nav>
          </div>

          <div className="mt-8 pt-6 border-t border-[#E2E8F0] text-[11px] text-[#94A3B8] leading-relaxed space-y-1">
            <p>
              참고 기준: 금융분야 AI 가이드라인 · 개인정보보호위원회 생성형 AI 안내서 · 복지로 공공데이터.
              민감정보(계좌·카드·의료 원문)는 외부 AI에 전송하지 않으며 비식별 요약만 사용합니다.
            </p>
            <p>
              본 서비스는 참고용 정보이며 법률·세무·금융 자문이 아닙니다. 실제 자격·금액·기한은 관할 기관에 반드시 확인하세요.
            </p>
            <p className="pt-1">© 2026 CrisFin (FIN5). All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}
