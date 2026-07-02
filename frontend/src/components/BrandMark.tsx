/**
 * CrisFin 로고 마크 — 배지형 맥박 C (시안 1).
 * 둥근 파란 배지 안에 흰 C 아크 + 밝은 파랑 심전도 스파이크로 '위기 대응' 신호를 표현.
 * 앱 전역(사이드바·랜딩·인증·파비콘)에서 재사용한다.
 */
export default function BrandMark({
  size = 32,
  className = '',
}: {
  size?: number
  className?: string
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      className={className}
      role="img"
      aria-label="CrisFin 로고"
    >
      <rect width="40" height="40" rx="11" fill="#2563EB" />
      {/* C 아크(오른쪽이 열린 C) */}
      <path
        d="M28.3 11.5 A13 13 0 1 0 28.3 28.5"
        stroke="#ffffff"
        strokeWidth="3.4"
        strokeLinecap="round"
      />
      {/* 심전도(맥박) 스파이크 */}
      <polyline
        points="12,20 16.5,20 18.5,14.5 21.5,25.5 23.5,20 27,20"
        stroke="#93C5FD"
        strokeWidth="2.4"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
