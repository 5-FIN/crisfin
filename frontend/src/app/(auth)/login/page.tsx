'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { Zap } from 'lucide-react'
import { authApi, paymentApi } from '@/lib/api'
import { tokenStore } from '@/lib/utils'
import { resumePendingAnalysis } from '@/lib/resumeAnalysis'
import BrandMark from '@/components/BrandMark'

/** 데모용 체험 계정 — 원클릭으로 로그인을 스킵한다. */
const DEMO = { email: 'demo@crisfin.app', password: 'DemoPass1!', nickname: '데모' }

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

  /** 데모 계정으로 바로 로그인(없으면 생성) + 이용권 자동 발급 → 진단으로. 로그인 입력을 스킵한다. */
  async function demoStart() {
    setError('')
    setLoading(true)
    try {
      let res
      try {
        res = await authApi.login({ email: DEMO.email, password: DEMO.password })
      } catch {
        // 계정이 아직 없으면 생성(이미 있으면 dup → 다시 로그인)
        try { res = await authApi.signup(DEMO) }
        catch { res = await authApi.login({ email: DEMO.email, password: DEMO.password }) }
      }
      tokenStore.set(res.accessToken, res.refreshToken)
      // 이용권이 없으면 데모용 무제한 이용권을 자동 발급(결제 스킵)
      try {
        const ent = await paymentApi.entitlement()
        if (!ent.active) {
          const { orderUid } = await paymentApi.checkout('UNLIMITED_30D')
          await paymentApi.confirm(orderUid)
        }
      } catch { /* 이용권 발급 실패해도 로그인은 진행 */ }
      router.push('/diagnosis')
    } catch (err) {
      setError(err instanceof Error ? err.message : '데모 시작 중 오류가 발생했습니다.')
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#F9FAFB] flex flex-col items-center px-4 pt-12 md:pt-20 pb-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-10">
          <Link href="/" className="inline-flex items-center gap-2.5 mb-6">
            <BrandMark size={44} />
            <span className="font-bold text-[#1E293B] text-2xl">CrisFin</span>
          </Link>
          <h1 className="text-3xl font-bold text-[#1E293B]">로그인</h1>
          <p className="text-base text-[#64748B] mt-2">계속하려면 로그인하세요</p>
        </div>

        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-8 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-[#1E293B] mb-1.5">이메일</label>
              <input
                type="email"
                value={form.email}
                onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
                placeholder="example@email.com"
                required
                className="w-full px-3.5 py-3 rounded-lg border border-[#E2E8F0] text-base text-[#1E293B] placeholder-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
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
                className="w-full px-3.5 py-3 rounded-lg border border-[#E2E8F0] text-base text-[#1E293B] placeholder-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
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
              className="w-full py-3 bg-[#2563EB] text-white font-semibold rounded-lg hover:bg-[#1D4ED8] disabled:opacity-60 disabled:cursor-not-allowed transition-colors text-base"
            >
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          {/* 데모 바로 시작 — 로그인 스킵 */}
          <div className="mt-4 pt-4 border-t border-[#E2E8F0]">
            <button
              onClick={demoStart}
              disabled={loading}
              className="w-full py-3 bg-[#F1F5F9] text-[#475569] font-semibold rounded-lg hover:bg-[#E2E8F0] disabled:opacity-60 disabled:cursor-not-allowed transition-colors text-base flex items-center justify-center gap-1.5"
            >
              <Zap size={15} className="text-[#2563EB]" /> 데모로 바로 시작 (로그인 스킵)
            </button>
            <p className="text-center text-[11px] text-[#94A3B8] mt-2">체험 계정 즉시 로그인 · 이용권 자동 적용</p>
          </div>
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
