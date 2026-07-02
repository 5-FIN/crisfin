'use client'

import { useEffect, useState } from 'react'
import { Moon, Sun } from 'lucide-react'

type Theme = 'light' | 'dark'

/** <html>.dark 클래스 + localStorage('cf_theme')를 토글하는 라이트/다크 전환 버튼. */
export default function ThemeToggle() {
  // 초기값은 서버/클라 불일치를 막기 위해 마운트 후에만 확정한다.
  const [theme, setTheme] = useState<Theme>('light')
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
    setTheme(document.documentElement.classList.contains('dark') ? 'dark' : 'light')
  }, [])

  function toggle() {
    const next: Theme = theme === 'dark' ? 'light' : 'dark'
    setTheme(next)
    document.documentElement.classList.toggle('dark', next === 'dark')
    try { localStorage.setItem('cf_theme', next) } catch { /* 저장 실패 무시 */ }
  }

  // 마운트 전에는 아이콘을 비워 하이드레이션 불일치를 피한다(레이아웃은 유지).
  return (
    <button
      onClick={toggle}
      aria-label={theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'}
      title={theme === 'dark' ? '라이트 모드' : '다크 모드'}
      className="p-1.5 rounded-lg hover:bg-[#F1F5F9] text-[#475569] transition-colors"
    >
      {mounted && (theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />)}
      {!mounted && <span className="block w-[18px] h-[18px]" />}
    </button>
  )
}
