'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { authApi } from '@/lib/api'
import { tokenStore } from '@/lib/utils'
import { resumePendingAnalysis } from '@/lib/resumeAnalysis'
import BrandMark from '@/components/BrandMark'

export default function LoginPage() {
  const router = useRouter()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await authApi.login(form)
      tokenStore.set(res.accessToken, res.refreshToken)
      // 보류된 분석이 있으면 즉시 재실행(결제 필요 시 /unlock으로), 없으면 대시보드
      const resumed = await resumePendingAnalysis(router.push)
      if (!resumed) router.push('/dashboard')
    } catch (err) {
      setError(err instanceof Error ? err.message : '로그인 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <Link href="/" className="inline-flex items-center gap-2 mb-6">
            <BrandMark size={36} />
            <span className="font-bold text-[#1E293B] text-xl">CrisFin</span>
          </Link>
          <h1 className="text-2xl font-bold text-[#1E293B]">로그인</h1>
          <p className="text-sm text-[#64748B] mt-1">계속하려면 로그인하세요</p>
        </div>

        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-6 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-[#1E293B] mb-1.5">이메일</label>
              <input
                type="email"
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                placeholder="example@email.com"
                required
                className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] placeholder-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-[#1E293B] mb-1.5">비밀번호</label>
              <input
                type="password"
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                placeholder="8자 이상"
                required
                className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] placeholder-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
              />
            </div>
            {error && (
              <div className="px-3.5 py-2.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-600">
                {error}
              </div>
            )}
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-[#2563EB] text-white font-semibold rounded-lg hover:bg-[#1D4ED8] disabled:opacity-60 disabled:cursor-not-allowed transition-colors text-sm"
            >
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>

        </div>

        <p className="text-center text-sm text-[#64748B] mt-4">
          계정이 없으신가요?{' '}
          <Link href="/signup" className="text-[#2563EB] font-medium hover:underline">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  )
}
