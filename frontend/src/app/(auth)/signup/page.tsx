'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { authApi } from '@/lib/api'
import { tokenStore } from '@/lib/utils'
import { SIDO_LIST, sigunguOf } from '@/lib/regions'
import BrandMark from '@/components/BrandMark'

export default function SignupPage() {
  const router = useRouter()
  const [form, setForm] = useState({ email: '', password: '', nickname: '', regionCtpv: '', regionSgg: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (form.password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.')
      return
    }
    setLoading(true)
    try {
      const res = await authApi.signup({
        ...form,
        regionCtpv: form.regionCtpv || undefined,
        regionSgg: form.regionSgg || undefined,
      })
      tokenStore.set(res.accessToken, res.refreshToken)
      router.push('/welcome')
    } catch (err) {
      setError(err instanceof Error ? err.message : '회원가입 중 오류가 발생했습니다.')
    } finally {
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
          <h1 className="text-3xl font-bold text-[#1E293B]">회원가입</h1>
          <p className="text-base text-[#64748B] mt-2">3분 무료 분석을 위해 가입하세요</p>
        </div>

        <div className="bg-white rounded-2xl border border-[#E2E8F0] p-8 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-4">
            {[
              { key: 'email',    label: '이메일',    type: 'email',    placeholder: 'example@email.com' },
              { key: 'nickname', label: '닉네임',    type: 'text',     placeholder: '홍길동' },
              { key: 'password', label: '비밀번호',  type: 'password', placeholder: '8자 이상' },
            ].map(({ key, label, type, placeholder }) => (
              <div key={key}>
                <label className="block text-sm font-medium text-[#1E293B] mb-1.5">{label}</label>
                <input
                  type={type}
                  value={form[key as keyof typeof form]}
                  onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))}
                  placeholder={placeholder}
                  required
                  className="w-full px-3.5 py-3 rounded-lg border border-[#E2E8F0] text-base text-[#1E293B] placeholder-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
                />
              </div>
            ))}
            <div>
              <label className="block text-sm font-medium text-[#1E293B] mb-1.5">지역 (선택)</label>
              <div className="grid grid-cols-2 gap-2">
                {/* 시/도 — 변경 시 하위 시/군/구 선택을 초기화 */}
                <select
                  value={form.regionCtpv}
                  onChange={e => setForm(f => ({ ...f, regionCtpv: e.target.value, regionSgg: '' }))}
                  className="w-full px-3.5 py-3 rounded-lg border border-[#E2E8F0] text-base text-[#1E293B] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
                >
                  <option value="">시/도 선택</option>
                  {SIDO_LIST.map(sido => (
                    <option key={sido} value={sido}>{sido}</option>
                  ))}
                </select>
                {/* 시/군/구 — 시/도 선택 후 활성화. 하위가 없는 지역(세종)은 비활성 */}
                <select
                  value={form.regionSgg}
                  onChange={e => setForm(f => ({ ...f, regionSgg: e.target.value }))}
                  disabled={!form.regionCtpv || sigunguOf(form.regionCtpv).length === 0}
                  className="w-full px-3.5 py-3 rounded-lg border border-[#E2E8F0] text-base text-[#1E293B] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition disabled:bg-[#F1F5F9] disabled:text-[#94A3B8] disabled:cursor-not-allowed"
                >
                  <option value="">시/군/구 선택</option>
                  {sigunguOf(form.regionCtpv).map(sgg => (
                    <option key={sgg} value={sgg}>{sgg}</option>
                  ))}
                </select>
              </div>
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
              {loading ? '가입 중...' : '회원가입 & 분석 시작'}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-[#64748B] mt-4">
          이미 계정이 있으신가요?{' '}
          <Link href="/login" className="text-[#2563EB] font-medium hover:underline">
            로그인
          </Link>
        </p>
      </div>
    </div>
  )
}
