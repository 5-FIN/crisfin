'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import {
  LayoutDashboard, CreditCard, Gift, CheckSquare,
  History, Menu, LogOut, ChevronRight,
} from 'lucide-react'
import { cn, tokenStore, analysisStore, CRISIS_LABELS } from '@/lib/utils'

const NAV = [
  { href: '/dashboard', icon: LayoutDashboard, label: '대시보드' },
  { href: '/payments',  icon: CreditCard,      label: '납부 관리', badge: '!' },
  { href: '/benefits',  icon: Gift,             label: '혜택 매처' },
  { href: '/tasks',     icon: CheckSquare,      label: '액션 체크리스트' },
  { href: '/history',   icon: History,          label: '히스토리' },
]

function SidebarNav({
  pathname,
  crisisLabel,
  onClose,
  onLogout,
}: {
  pathname: string
  crisisLabel: string | null
  onClose: () => void
  onLogout: () => void
}) {
  return (
    <div className="flex flex-col h-full">
      {/* 로고 */}
      <div className="flex items-center gap-2 px-5 py-5 border-b border-[#E2E8F0]">
        <div className="w-8 h-8 rounded-lg bg-[#2563EB] flex items-center justify-center text-white text-sm font-bold">C</div>
        <span className="font-bold text-[#1E293B] text-lg">CrisFin</span>
      </div>

      {/* 위기 상태 배지 */}
      {crisisLabel && (
        <div className="mx-4 mt-4 px-3 py-2 rounded-lg bg-[#FFFBEB] border border-[#FEF3C7] flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#F59E0B] animate-pulse" />
          <span className="text-xs font-medium text-[#D97706]">{crisisLabel} 대응 중</span>
        </div>
      )}

      {/* 네비게이션 */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {NAV.map(({ href, icon: Icon, label, badge }) => {
          const active = pathname === href
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
              {badge && (
                <span className="w-5 h-5 rounded-full bg-[#F59E0B] text-white text-[10px] font-bold flex items-center justify-center">
                  {badge}
                </span>
              )}
            </Link>
          )
        })}
      </nav>

      {/* 하단 로그아웃 */}
      <div className="px-3 py-4 border-t border-[#E2E8F0]">
        <button
          onClick={onLogout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-[#64748B] hover:bg-[#F8FAFC] hover:text-[#EF4444] transition-colors"
        >
          <LogOut size={18} />
          <span>로그아웃</span>
        </button>
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
  useEffect(() => {
    const analysis = analysisStore.load()
    setCrisisLabel(
      analysis ? (CRISIS_LABELS[analysis.crisisType as string] ?? analysis.crisisType) : null,
    )
  }, [pathname])

  function handleLogout() {
    tokenStore.clear()
    analysisStore.clear()
    router.push('/')
  }

  return (
    <div className="flex h-screen bg-[#F9FAFB] overflow-hidden">
      {/* 데스크탑 사이드바 */}
      <aside className="hidden md:flex w-60 flex-col bg-white border-r border-[#E2E8F0] flex-shrink-0">
        <SidebarNav pathname={pathname} crisisLabel={crisisLabel} onClose={() => setOpen(false)} onLogout={handleLogout} />
      </aside>

      {/* 모바일 드로어 오버레이 */}
      {open && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/30" onClick={() => setOpen(false)} />
          <aside className="absolute left-0 top-0 h-full w-64 bg-white shadow-xl z-50">
            <SidebarNav pathname={pathname} crisisLabel={crisisLabel} onClose={() => setOpen(false)} onLogout={handleLogout} />
          </aside>
        </div>
      )}

      {/* 메인 영역 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 탑바 */}
        <header className="h-14 bg-white border-b border-[#E2E8F0] flex items-center px-4 gap-3 flex-shrink-0">
          <button
            className="md:hidden p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#475569]"
            onClick={() => setOpen(true)}
          >
            <Menu size={20} />
          </button>
          <div className="flex-1" />
          <Link
            href="/"
            className="flex items-center gap-1 text-xs text-[#64748B] hover:text-[#2563EB] transition-colors"
          >
            처음으로 <ChevronRight size={12} />
          </Link>
        </header>

        {/* 페이지 콘텐츠 */}
        <main className="flex-1 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  )
}
