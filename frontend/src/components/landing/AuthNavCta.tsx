'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { ArrowRight } from 'lucide-react'
import { tokenStore } from '@/lib/utils'

/**
 * 랜딩 네브의 우측 CTA. 로그인 상태(localStorage 토큰)에 따라 다르게 보여준다.
 * - 로그인됨: "대시보드로 가기" → 다시 로그인할 필요 없이 앱으로 복귀
 * - 비로그인: 로그인 / 시작하기
 * 토큰은 마운트 이후에만 읽어 SSR/CSR 하이드레이션 불일치를 피한다.
 */
export default function AuthNavCta() {
  const [authed, setAuthed] = useState<boolean | null>(null)
  useEffect(() => { setAuthed(!!tokenStore.getAccess()) }, [])

  // 마운트 전에는 자리만 잡아 레이아웃 흔들림/깜빡임 방지
  if (authed === null) return <div className="h-9 w-[132px]" />

  if (authed) {
    return (
      <Link href="/dashboard"
        className="inline-flex items-center gap-1.5 px-4 py-2 bg-[#2563EB] text-white text-sm font-medium rounded-lg hover:bg-[#1D4ED8] transition-colors">
        대시보드로 가기 <ArrowRight size={15} />
      </Link>
    )
  }

  return (
    <>
      <Link href="/login" className="text-sm text-[#475569] hover:text-[#1E293B] transition-colors">
        로그인
      </Link>
      <Link href="/signup"
        className="px-4 py-2 bg-[#2563EB] text-white text-sm font-medium rounded-lg hover:bg-[#1D4ED8] transition-colors">
        시작하기
      </Link>
    </>
  )
}
