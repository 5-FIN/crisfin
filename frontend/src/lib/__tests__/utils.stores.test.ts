import { describe, it, expect, beforeEach } from 'vitest'
import {
  fmtAmount,
  priorityBadge,
  riskBadge,
  jobToPersona,
  buildSituationDescription,
  CRISIS_KEY_MAP,
  tokenStore,
  analysisStore,
  pendingAnalysisStore,
  taskStore,
} from '@/lib/utils'

describe('fmtAmount', () => {
  it('null/undefined는 fallback(기본 —)을 반환한다', () => {
    expect(fmtAmount(null)).toBe('—')
    expect(fmtAmount(undefined)).toBe('—')
    expect(fmtAmount(null, '0원')).toBe('0원')
  })

  it('값이 있으면 fmt 결과를 반환한다', () => {
    expect(fmtAmount(0)).toBe('0원')
    expect(fmtAmount(10_000)).toBe('1만원')
    expect(fmtAmount(150_000_000)).toBe('1.5억')
  })
})

describe('priorityBadge / riskBadge', () => {
  it('HIGH/MED/LOW를 각각 긴급/주의/안전 라벨로 매핑한다', () => {
    expect(priorityBadge('HIGH').label).toBe('긴급')
    expect(priorityBadge('MED').label).toBe('주의')
    expect(priorityBadge('LOW').label).toBe('안전')
  })

  it('배지는 bg/color/border 색상값을 포함한다', () => {
    const high = priorityBadge('HIGH')
    expect(high).toMatchObject({
      bg: expect.any(String),
      color: expect.any(String),
      border: expect.any(String),
    })
  })

  it('riskBadge는 priorityBadge에 위임한다', () => {
    expect(riskBadge('HIGH')).toEqual(priorityBadge('HIGH'))
    expect(riskBadge('MED')).toEqual(priorityBadge('MED'))
    expect(riskBadge('LOW')).toEqual(priorityBadge('LOW'))
  })
})

describe('jobToPersona', () => {
  it('알려진 직업을 페르소나로 매핑한다', () => {
    expect(jobToPersona('employed')).toBe('OFFICE_WORKER')
    expect(jobToPersona('self_employed')).toBe('SELF_EMPLOYED')
    expect(jobToPersona('freelancer')).toBe('FREELANCER')
    expect(jobToPersona('laid_off')).toBe('LAID_OFF')
    expect(jobToPersona('public')).toBe('PUBLIC_SERVANT')
  })

  it('알 수 없는 값은 OFFICE_WORKER로 폴백한다', () => {
    expect(jobToPersona('unknown')).toBe('OFFICE_WORKER')
    expect(jobToPersona('')).toBe('OFFICE_WORKER')
  })
})

describe('buildSituationDescription', () => {
  it('템플릿에 라벨/직업/소득/가구원/상세를 포함한다', () => {
    const result = buildSituationDescription({
      crisisType: 'HOSPITALIZATION',
      job: '회사원',
      income: '300',
      household: '4',
      detail: '갑작스러운 입원으로 소득이 끊겼습니다',
    })
    expect(result).toContain('입원/수술') // CRISIS_LABELS 매핑
    expect(result).toContain('[직업] 회사원')
    expect(result).toContain('[월 소득] 300만원')
    expect(result).toContain('[가구원 수] 4인')
    expect(result).toContain('[상세 상황] 갑작스러운 입원으로 소득이 끊겼습니다')
  })

  it('알 수 없는 crisisType은 원본 키를 그대로 라벨로 쓴다', () => {
    const result = buildSituationDescription({
      crisisType: 'UNKNOWN_TYPE',
      job: '프리랜서',
      income: '0',
      household: '1',
      detail: '없음',
    })
    expect(result).toContain('[위기 유형] UNKNOWN_TYPE')
  })
})

describe('CRISIS_KEY_MAP', () => {
  it('프로토타입 키를 백엔드 enum으로 매핑한다', () => {
    expect(CRISIS_KEY_MAP['hospitalization']).toBe('HOSPITALIZATION')
    expect(CRISIS_KEY_MAP['accident']).toBe('ACCIDENT')
    expect(CRISIS_KEY_MAP['job-loss']).toBe('UNEMPLOYMENT')
    expect(CRISIS_KEY_MAP['caregiving']).toBe('CAREGIVING')
    expect(CRISIS_KEY_MAP['bereavement']).toBe('BEREAVEMENT')
  })
})

describe('tokenStore', () => {
  beforeEach(() => localStorage.clear())

  it('set 후 getAccess/getRefresh로 토큰을 읽는다', () => {
    tokenStore.set('access-1', 'refresh-1')
    expect(tokenStore.getAccess()).toBe('access-1')
    expect(tokenStore.getRefresh()).toBe('refresh-1')
  })

  it('토큰이 없으면 null을 반환한다', () => {
    expect(tokenStore.getAccess()).toBeNull()
    expect(tokenStore.getRefresh()).toBeNull()
  })

  it('clear는 두 토큰을 모두 제거한다', () => {
    tokenStore.set('a', 'b')
    tokenStore.clear()
    expect(tokenStore.getAccess()).toBeNull()
    expect(tokenStore.getRefresh()).toBeNull()
  })
})

describe('analysisStore', () => {
  beforeEach(() => localStorage.clear())

  it('save/load로 객체를 왕복 저장한다', () => {
    const obj = { id: 7, result: { foo: 'bar' } }
    analysisStore.save(obj)
    expect(analysisStore.load()).toEqual(obj)
  })

  it('저장된 값이 없으면 null을 반환한다', () => {
    expect(analysisStore.load()).toBeNull()
  })

  it('JSON 파싱 실패 시 null을 안전 반환한다', () => {
    localStorage.setItem('cf_analysis', '{잘못된 json')
    expect(analysisStore.load()).toBeNull()
  })

  it('clear는 저장된 값을 제거한다', () => {
    analysisStore.save({ id: 1 })
    analysisStore.clear()
    expect(analysisStore.load()).toBeNull()
  })
})

describe('pendingAnalysisStore', () => {
  beforeEach(() => localStorage.clear())

  it('save/load로 보류 요청을 왕복 저장한다', () => {
    const req = { crisisType: 'HOSPITALIZATION', situationDescription: '입원' }
    pendingAnalysisStore.save(req)
    expect(pendingAnalysisStore.load()).toEqual(req)
  })

  it('저장된 값이 없으면 null을 반환한다', () => {
    expect(pendingAnalysisStore.load()).toBeNull()
  })

  it('JSON 파싱 실패 시 null을 안전 반환한다', () => {
    localStorage.setItem('cf_pending_analysis', 'not-json{')
    expect(pendingAnalysisStore.load()).toBeNull()
  })

  it('clear는 저장된 값을 제거한다', () => {
    pendingAnalysisStore.save({ a: 1 })
    pendingAnalysisStore.clear()
    expect(pendingAnalysisStore.load()).toBeNull()
  })
})

describe('taskStore', () => {
  beforeEach(() => localStorage.clear())

  it('key는 analysisId별 고유 키를 만든다', () => {
    expect(taskStore.key(42)).toBe('cf_tasks_42')
  })

  it('저장된 값이 없으면 빈 객체를 반환한다', () => {
    expect(taskStore.load(1)).toEqual({})
  })

  it('JSON 파싱 실패 시 빈 객체를 안전 반환한다', () => {
    localStorage.setItem(taskStore.key(1), 'broken{')
    expect(taskStore.load(1)).toEqual({})
  })

  it('toggle은 키 완료 상태를 켜고 끄며 저장한다', () => {
    const after1 = taskStore.toggle(1, 'p0-0')
    expect(after1['p0-0']).toBe(true)
    // 저장도 반영되는지 확인
    expect(taskStore.load(1)).toEqual({ 'p0-0': true })

    const after2 = taskStore.toggle(1, 'p0-0')
    expect(after2['p0-0']).toBe(false)
    expect(taskStore.load(1)).toEqual({ 'p0-0': false })
  })

  it('toggle은 analysisId별로 독립적으로 동작한다', () => {
    taskStore.toggle(1, 'a')
    taskStore.toggle(2, 'b')
    expect(taskStore.load(1)).toEqual({ a: true })
    expect(taskStore.load(2)).toEqual({ b: true })
  })
})
