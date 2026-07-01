'use client'

import { useRouter } from 'next/navigation'
import { FileSearch } from 'lucide-react'

/**
 * 분석 결과(analysisStore)가 없을 때 데이터 게이팅 페이지에서 보여주는 빈 상태.
 * 이전에는 결과가 없으면 /diagnosis로 강제 리다이렉트해 "링크가 엉뚱한 곳으로 간다"는
 * 오해를 유발했다. 이제 제자리에서 안내 + 진단 CTA를 렌더한다.
 */
export default function NoAnalysisEmptyState({ title }: { title?: string }) {
  const router = useRouter()
  return (
    <div className="px-4 md:px-8 py-8 max-w-2xl mx-auto">
      {title && <h1 className="text-2xl font-bold text-[#1E293B] mb-6">{title}</h1>}
      <div className="bg-white rounded-xl border border-[#E2E8F0] p-12 text-center">
        <div className="w-14 h-14 rounded-full bg-[#EFF6FF] flex items-center justify-center mx-auto mb-4">
          <FileSearch size={26} className="text-[#2563EB]" />
        </div>
        <h2 className="text-lg font-semibold text-[#1E293B] mb-1">아직 분석 결과가 없어요</h2>
        <p className="text-sm text-[#64748B] mb-6">진단을 먼저 진행하면 이 화면에서 결과를 확인할 수 있어요.</p>
        <button
          onClick={() => router.push('/diagnosis')}
          className="px-5 py-2.5 bg-[#2563EB] text-white text-sm font-semibold rounded-lg hover:bg-[#1D4ED8] transition-colors"
        >
          진단 시작하기
        </button>
      </div>
    </div>
  )
}
