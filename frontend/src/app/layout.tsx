import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'CrisFin — 위기 금융 길라잡이',
  description: '갑작스러운 위기 상황에서 지금 당장 해야 할 금융 행동을 AI가 정리해드립니다.',
}

/**
 * 페인트 전에 localStorage의 테마를 <html>.dark 클래스로 적용해 다크모드
 * 깜빡임(FOUC)을 막는다. React 하이드레이션 이전에 실행돼야 하므로 인라인
 * 스크립트로 주입한다.
 */
const THEME_INIT = `(function(){try{if(localStorage.getItem('cf_theme')==='dark')document.documentElement.classList.add('dark')}catch(e){}})()`

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_INIT }} />
      </head>
      <body>{children}</body>
    </html>
  )
}
