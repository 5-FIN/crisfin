import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'CrisFin — 위기 금융 길라잡이',
  description: '갑작스러운 위기 상황에서 지금 당장 해야 할 금융 행동을 AI가 정리해드립니다.',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  )
}
