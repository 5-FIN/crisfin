'use client'

import { useEffect, useState } from 'react'
import { Loader2, Check } from 'lucide-react'
import { usersApi } from '@/lib/api'
import { SIDO_LIST, sigunguOf } from '@/lib/regions'
import type { UserResponse } from '@/lib/types'

export default function SettingsPage() {
  const [profile, setProfile] = useState<UserResponse | null>(null)
  const [nickname, setNickname] = useState('')
  const [regionCtpv, setRegionCtpv] = useState('')
  const [regionSgg, setRegionSgg] = useState('')

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)

  // 현재 프로필 로드 → 폼 프리필
  useEffect(() => {
    usersApi.me()
      .then(me => {
        setProfile(me)
        setNickname(me.nickname ?? '')
        setRegionCtpv(me.regionCtpv ?? '')
        setRegionSgg(me.regionSgg ?? '')
      })
      .catch(() => setError('프로필을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSaved(false)
    if (!nickname.trim()) {
      setError('닉네임은 필수입니다.')
      return
    }
    setSaving(true)
    try {
      const updated = await usersApi.updateMe({
        nickname: nickname.trim(),
        regionCtpv: regionCtpv || undefined,
        regionSgg: regionSgg || undefined,
      })
      setProfile(updated)
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } catch (err) {
      setError(err instanceof Error ? err.message : '저장 중 오류가 발생했습니다.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <Loader2 size={28} className="text-[#2563EB] animate-spin" />
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 md:px-8 py-10">
      <h1 className="text-2xl font-bold text-[#1E293B] mb-1">설정</h1>
      <p className="text-sm text-[#64748B] mb-8">프로필과 지역을 관리하세요. 지역은 복지 추천에 사용됩니다.</p>

      <div className="bg-white rounded-2xl border border-[#E2E8F0] p-6 shadow-sm">
        <form onSubmit={handleSubmit} className="space-y-5">
          {/* 이메일 (읽기 전용) */}
          <div>
            <label className="block text-sm font-medium text-[#1E293B] mb-1.5">이메일</label>
            <input
              type="email"
              value={profile?.email ?? ''}
              disabled
              className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#94A3B8] bg-[#F1F5F9] cursor-not-allowed"
            />
          </div>

          {/* 닉네임 */}
          <div>
            <label className="block text-sm font-medium text-[#1E293B] mb-1.5">닉네임</label>
            <input
              type="text"
              value={nickname}
              onChange={e => setNickname(e.target.value)}
              maxLength={50}
              placeholder="닉네임"
              className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] placeholder-[#94A3B8] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
            />
          </div>

          {/* 지역 (시도 + 시군구) */}
          <div>
            <label className="block text-sm font-medium text-[#1E293B] mb-1.5">지역</label>
            <div className="grid grid-cols-2 gap-2">
              <select
                value={regionCtpv}
                onChange={e => { setRegionCtpv(e.target.value); setRegionSgg('') }}
                className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition"
              >
                <option value="">시/도 선택</option>
                {SIDO_LIST.map(sido => (
                  <option key={sido} value={sido}>{sido}</option>
                ))}
              </select>
              <select
                value={regionSgg}
                onChange={e => setRegionSgg(e.target.value)}
                disabled={!regionCtpv || sigunguOf(regionCtpv).length === 0}
                className="w-full px-3.5 py-2.5 rounded-lg border border-[#E2E8F0] text-sm text-[#1E293B] focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition disabled:bg-[#F1F5F9] disabled:text-[#94A3B8] disabled:cursor-not-allowed"
              >
                <option value="">시/군/구 선택</option>
                {sigunguOf(regionCtpv).map(sgg => (
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
            disabled={saving}
            className="w-full py-2.5 bg-[#2563EB] text-white font-semibold rounded-lg hover:bg-[#1D4ED8] disabled:opacity-60 disabled:cursor-not-allowed transition-colors text-sm flex items-center justify-center gap-2"
          >
            {saving ? '저장 중...' : saved ? (<><Check size={16} />저장됨</>) : '변경사항 저장'}
          </button>
        </form>
      </div>
    </div>
  )
}
