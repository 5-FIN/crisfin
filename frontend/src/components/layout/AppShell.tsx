'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import {
  LayoutDashboard, CreditCard, Gift, CheckSquare, Menu, LogOut,
} from 'lucide-react'
import { cn, tokenStore, analysisStore, paymentStore, CRISIS_LABELS } from '@/lib/utils'
import { authApi } from '@/lib/api'
import ThemeToggle from './ThemeToggle'
import BrandMark from '@/components/BrandMark'

// 좌측 사이드바 — 분석 결과 핵심 메뉴
const NAV = [
  { href: '/dashboard', icon: LayoutDashboard, label: '대시보드' },
  { href: '/payments',  icon: CreditCard,      label: '납부 관리' },
  { href: '/benefits',  icon: Gift,            label: '받을 수 있는 혜택' },
  { href: '/tasks',     icon: CheckSquare,     label: '액션 체크리스트' },
]

// 상단 탭 — 보조/유틸리티 메뉴
const TOP_TABS = [
  { href: '/guide',     label: '무료 길라잡이' },
  { href: '/welfare',   label: '복지 찾기' },
  { href: '/favorites', label: '즐겨찾기' },
  { href: '/history',   label: '히스토리' },
  { href: '/settings',  label: '설정' },
]

function SidebarNav({
  pathname,
  crisisLabel,
  paymentBadge,
  onClose,
  onLogout,
}: {
  pathname: string
  crisisLabel: string | null
  paymentBadge: number
  onClose: () => void
  onLogout: () => void
}) {
  return (
    <div className="flex flex-col h-full">
      {/* 로고 — 클릭 시 홈(대시보드)으로 */}
      <Link
        href="/"
        onClick={onClose}
        className="flex items-center gap-2 px-5 py-5 border-b border-[#E2E8F0] hover:bg-[#F8FAFC] transition-colors"
      >
        <BrandMark size={32} />
        <span className="font-bold text-[#1E293B] text-lg">CrisFin</span>
      </Link>

      {/* 위기 상태 배지 */}
      {crisisLabel && (
        <div className="mx-4 mt-4 px-3 py-2 rounded-lg bg-[#FFFBEB] border border-[#FEF3C7] flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#F59E0B] animate-pulse" />
          <span className="text-xs font-medium text-[#D97706]">{crisisLabel} 대응 중</span>
        </div>
      )}

      {/* 네비게이션 */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {NAV.map(({ href, icon: Icon, label }) => {
          const active = pathname === href
          // 납부 관리 배지는 미처리 긴급 건수(0이면 숨김)로 동적 표시
          const effectiveBadge = href === '/payments' && paymentBadge > 0
            ? String(paymentBadge)
            : null
          return (
            <Link
              key={href}
              href={href}
              onClick={onClose}
              className={cn(
                'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                active
                  ? 'bg-[#EFF6FF] text-[#2563EB]'
                  : 'text-[#475569] hover:bg-[#F8FAFC] hover:text-[#1E293B]',
              )}
            >
              <Icon size={18} />
              <span className="flex-1">{label}</span>
              {effectiveBadge && (
                <span className="min-w-5 h-5 px-1 rounded-full bg-[#F59E0B] text-white text-[10px] font-bold flex items-center justify-center">
                  {effectiveBadge}
                </span>
              )}
            </Link>
          )
        })}
      </nav>

      {/* 하단 로그아웃 + 다크모드 토글 */}
      <div className="px-3 py-4 border-t border-[#E2E8F0] flex items-center gap-2">
        <button
          onClick={onLogout}
          className="flex-1 flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#EF4444] transition-colors"
        >
          <LogOut size={18} />
          <span>로그아웃</span>
        </button>
        <ThemeToggle />
      </div>
    </div>
  )
}

export default function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const router   = useRouter()
  const [open, setOpen] = useState(false)

  // localStorage는 마운트 이후에만 읽어 SSR/CSR 하이드레이션 불일치를 방지한다.
  const [crisisLabel, setCrisisLabel] = useState<string | null>(null)
  const [paymentBadge, setPaymentBadge] = useState(0)
  useEffect(() => {
    const analysis = analysisStore.load()
    setCrisisLabel(
      analysis ? (CRISIS_LABELS[analysis.crisisType as string] ?? analysis.crisisType) : null,
    )
    // 납부 관리 배지 = 미처리 긴급(HIGH) 항목 수. pathname 변경 + 납부 페이지의
    // 완료 토글(커스텀 이벤트)마다 재계산해 사이드바 배지를 실시간 반영한다.
    function computeBadge() {
      const a = analysisStore.load()
      if (a?.result?.todos) {
        const urgentKeys = (a.result.todos as { priority: string }[])
          .filter(t => t.priority === 'HIGH')
          .map((_, i) => `u${i}`)
        setPaymentBadge(paymentStore.pendingCount(a.id, urgentKeys))
      } else {
        setPaymentBadge(0)
      }
    }
    computeBadge()
    window.addEventListener('cf-payment-updated', computeBadge)
    return () => window.removeEventListener('cf-payment-updated', computeBadge)
  }, [pathname])

  async function handleLogout() {
    // 서버에서 refresh 토큰을 무효화한 뒤 로컬 상태를 정리한다. 서버 호출이
    // 실패하더라도(만료 등) 로컬 로그아웃은 그대로 진행한다.
    const refresh = tokenStore.getRefresh()
    if (refresh) {
      try { await authApi.logout(refresh) } catch { /* 무효화 실패는 무시 */ }
    }
    tokenStore.clear()
    analysisStore.clear()
    router.push('/')
  }

  return (
    <div className="flex h-screen bg-[#F9FAFB] overflow-hidden">
      {/* 데스크탑 사이드바 */}
      <aside className="hidden md:flex w-60 flex-col bg-white border-r border-[#E2E8F0] flex-shrink-0">
        <SidebarNav pathname={pathname} crisisLabel={crisisLabel} paymentBadge={paymentBadge} onClose={() => setOpen(false)} onLogout={handleLogout} />
      </aside>

      {/* 모바일 드로어 오버레이 */}
      {open && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/30" onClick={() => setOpen(false)} />
          <aside className="absolute left-0 top-0 h-full w-64 bg-white shadow-xl z-50">
            <SidebarNav pathname={pathname} crisisLabel={crisisLabel} paymentBadge={paymentBadge} onClose={() => setOpen(false)} onLogout={handleLogout} />
          </aside>
        </div>
      )}

      {/* 메인 영역 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 상단 탭바 — 보조 메뉴(무료 길라잡이·복지 찾기·즐겨찾기·히스토리·설정) */}
        <header className="h-14 bg-white border-b border-[#E2E8F0] flex items-center px-3 gap-2 flex-shrink-0">
          <button
            className="md:hidden p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#475569] flex-shrink-0"
            onClick={() => setOpen(true)}
          >
            <Menu size={20} />
          </button>
          <nav className="flex items-center gap-1 overflow-x-auto">
            {TOP_TABS.map(({ href, label }) => {
              const active = pathname === href
              return (
                <Link
                  key={href}
                  href={href}
                  className={cn(
                    'px-3 py-1.5 rounded-lg text-sm font-medium whitespace-nowrap transition-colors',
                    active
                      ? 'bg-[#EFF6FF] text-[#2563EB]'
                      : 'text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#1E293B]',
                  )}
                >
                  {label}
                </Link>
              )
            })}
          </nav>
        </header>

        {/* 페이지 콘텐츠 */}
        <main className="flex-1 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  )
}
