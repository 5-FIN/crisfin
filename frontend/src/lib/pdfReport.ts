import type { AnalysisResultResponse } from './types'
import { fmt, fmtAmount, CRISIS_LABELS, CRISIS_EMOJI } from './utils'

/* HTML 인젝션 방지 — LLM/사용자 입력 텍스트를 그대로 마크업에 넣지 않는다. */
function esc(v: unknown): string {
  return String(v ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const PRIORITY_KO: Record<string, string> = { HIGH: '긴급', MED: '주의', LOW: '안전' }
const PRIORITY_COLOR: Record<string, string> = { HIGH: '#D97706', MED: '#EA580C', LOW: '#059669' }
const RISK_KO: Record<string, string> = { HIGH: '높음', MED: '중간', LOW: '낮음' }

function section(title: string, body: string): string {
  if (!body.trim()) return ''
  return `<section><h2>${esc(title)}</h2>${body}</section>`
}

/**
 * 분석 결과를 인쇄용 새 창에 렌더하고 브라우저 인쇄 대화상자를 띄운다.
 * 사용자는 "PDF로 저장"을 선택해 리포트를 내려받을 수 있다. 별도 라이브러리
 * 없이 브라우저 기본 인쇄 파이프라인을 사용하므로 의존성이 늘지 않는다.
 *
 * @returns 팝업이 차단되면 false, 정상 진행되면 true
 */
export function printAnalysisReport(analysis: AnalysisResultResponse): boolean {
  const { result, crisisType, createdAt, llmProvider, situationDescription } = analysis
  const { summary, todos, receivable, holdable, actions, disclaimer } = result

  const label = CRISIS_LABELS[crisisType] ?? crisisType
  const emoji = CRISIS_EMOJI[crisisType] ?? ''
  const created = createdAt ? new Date(createdAt).toLocaleString('ko-KR') : ''

  const todosHtml = todos.length
    ? `<table><thead><tr><th>시기</th><th>할 일</th><th>기한</th><th>우선</th></tr></thead><tbody>${todos
        .map(
          (t) => `<tr>
            <td class="nowrap">${esc(t.dayRange)}</td>
            <td><div class="strong">${esc(t.action)}</div>${t.reason ? `<div class="muted">${esc(t.reason)}</div>` : ''}</td>
            <td class="nowrap">${esc(t.deadline)}</td>
            <td class="nowrap" style="color:${PRIORITY_COLOR[t.priority] ?? '#64748B'}">${esc(PRIORITY_KO[t.priority] ?? t.priority)}</td>
          </tr>`,
        )
        .join('')}</tbody></table>`
    : ''

  const receivableHtml = receivable.length
    ? `<table><thead><tr><th>혜택</th><th>예상 금액</th><th>출처</th><th>기한</th></tr></thead><tbody>${receivable
        .map((r) => {
          const amount =
            r.status === 'NEEDS_MORE_INPUT' || r.estimatedMin == null
              ? '<span class="muted">추가입력 필요</span>'
              : `${esc(fmt(r.estimatedMin))}${r.estimatedMax != null ? ' ~ ' + esc(fmt(r.estimatedMax)) : '+'}`
          return `<tr>
            <td><div class="strong">${esc(r.name)}</div>${r.basis ? `<div class="muted">${esc(r.basis)}</div>` : ''}</td>
            <td class="nowrap money">${amount}</td>
            <td class="nowrap">${esc(r.source)}</td>
            <td class="nowrap">${esc(r.deadline)}</td>
          </tr>`
        })
        .join('')}</tbody></table>`
    : ''

  const holdableHtml = holdable.length
    ? `<table><thead><tr><th>항목</th><th>유예 기간</th><th>리스크</th><th>방법</th></tr></thead><tbody>${holdable
        .map(
          (h) => `<tr>
            <td class="strong">${esc(h.name)}</td>
            <td class="nowrap">${esc(h.deferPeriod)}</td>
            <td class="nowrap">${esc(RISK_KO[h.riskLevel] ?? h.riskLevel)}</td>
            <td>${esc(h.howTo)}${h.caution ? `<div class="muted">⚠ ${esc(h.caution)}</div>` : ''}</td>
          </tr>`,
        )
        .join('')}</tbody></table>`
    : ''

  const actionsHtml = actions.length
    ? `<table><thead><tr><th>행동</th><th>필요 서류</th><th>기한</th><th>연락처</th></tr></thead><tbody>${actions
        .map(
          (a) => `<tr>
            <td class="strong" style="color:${PRIORITY_COLOR[a.priority] ?? '#1E293B'}">${esc(a.name)}</td>
            <td>${a.requiredDocs?.length ? esc(a.requiredDocs.join(', ')) : '<span class="muted">—</span>'}</td>
            <td class="nowrap">${esc(a.deadline)}</td>
            <td class="nowrap">${esc(a.contactInfo)}</td>
          </tr>`,
        )
        .join('')}</tbody></table>`
    : ''

  const html = `<!DOCTYPE html>
<html lang="ko"><head><meta charset="utf-8" />
<title>CrisFin 위기 대응 리포트 - ${esc(label)}</title>
<style>
  @page { size: A4; margin: 16mm 14mm; }
  * { box-sizing: border-box; }
  body { font-family: "Malgun Gothic", "맑은 고딕", -apple-system, sans-serif; color: #1E293B; margin: 0; font-size: 12px; line-height: 1.5; }
  header { border-bottom: 3px solid #2563EB; padding-bottom: 12px; margin-bottom: 18px; }
  .brand { color: #2563EB; font-weight: 800; font-size: 18px; }
  h1 { font-size: 20px; margin: 6px 0 4px; }
  .badge { display: inline-block; background: #FFFBEB; border: 1px solid #FEF3C7; color: #D97706; font-size: 11px; padding: 2px 8px; border-radius: 999px; font-weight: 600; }
  .meta { color: #94A3B8; font-size: 11px; margin-top: 4px; }
  .situation { background: #F8FAFC; border-left: 3px solid #CBD5E1; padding: 8px 12px; margin: 12px 0 4px; color: #475569; border-radius: 0 6px 6px 0; }
  .summary { display: flex; gap: 10px; margin: 16px 0; }
  .card { flex: 1; border: 1px solid #E2E8F0; border-radius: 10px; padding: 10px 12px; }
  .card .k { color: #64748B; font-size: 11px; }
  .card .v { font-size: 16px; font-weight: 800; margin-top: 2px; }
  .plan { background: #EFF6FF; border: 1px solid #DBEAFE; border-radius: 10px; padding: 10px 12px; color: #1E40AF; margin-bottom: 16px; }
  section { margin-bottom: 18px; page-break-inside: avoid; }
  h2 { font-size: 14px; border-left: 4px solid #2563EB; padding-left: 8px; margin: 0 0 8px; }
  table { width: 100%; border-collapse: collapse; }
  th { text-align: left; background: #F1F5F9; color: #475569; font-size: 11px; padding: 6px 8px; border-bottom: 1px solid #E2E8F0; }
  td { padding: 6px 8px; border-bottom: 1px solid #F1F5F9; vertical-align: top; }
  .strong { font-weight: 600; }
  .muted { color: #94A3B8; font-size: 11px; margin-top: 2px; }
  .money { color: #10B981; font-weight: 700; }
  .nowrap { white-space: nowrap; }
  footer { margin-top: 20px; padding-top: 10px; border-top: 1px solid #E2E8F0; color: #94A3B8; font-size: 10px; }
  @media print { body { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }
</style></head>
<body>
  <header>
    <div class="brand">CrisFin</div>
    <h1>${esc(emoji)} 금융 위기 대응 리포트</h1>
    <span class="badge">${esc(label)}</span>
    <div class="meta">생성일: ${esc(created)}${llmProvider ? ` · 분석 엔진: ${esc(llmProvider)}` : ''}</div>
    ${situationDescription ? `<div class="situation">${esc(situationDescription)}</div>` : ''}
  </header>

  <div class="summary">
    <div class="card"><div class="k">받을 수 있는 돈</div><div class="v money">${esc(fmtAmount(summary.totalReceivableMin, '0원'))} ~ ${esc(fmtAmount(summary.totalReceivableMax, '0원'))}</div></div>
    <div class="card"><div class="k">긴급 처리 필요</div><div class="v" style="color:#F59E0B">${esc(summary.urgentCount)}건</div></div>
  </div>
  ${summary.thirtyDayPlan ? `<div class="plan"><strong>30일 계획</strong> · ${esc(summary.thirtyDayPlan)}</div>` : ''}

  ${section('할 일', todosHtml)}
  ${section('받을 수 있는 혜택', receivableHtml)}
  ${section('미룰 수 있는 것', holdableHtml)}
  ${section('행동 항목', actionsHtml)}

  <footer>${esc(disclaimer)}</footer>
</body></html>`

  const win = window.open('', '_blank', 'width=900,height=1000')
  if (!win) return false // 팝업 차단됨

  win.document.open()
  win.document.write(html)
  win.document.close()

  // 렌더/폰트 로드 후 인쇄. onload가 즉시 걸리지 않는 브라우저를 위해 setTimeout 백업.
  // 두 경로가 중복 호출되지 않도록 1회만 실행한다.
  let printed = false
  const triggerPrint = () => {
    if (printed) return
    printed = true
    win.focus()
    win.print()
  }
  win.onload = triggerPrint
  setTimeout(triggerPrint, 400)

  return true
}
