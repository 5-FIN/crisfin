-- 시설·운영자 대상 보조금(개인 대상 아님)의 위기 태그를 제거해 "내 위기 맞춤" 추천에서 제외한다.
-- 예: "노숙인 재활시설 기능보강"은 요약의 '자활·재활' 때문에 UNEMPLOYMENT/HOSPITALIZATION로
-- 잘못 태깅되어 개인에게 추천되던 문제. 태그만 비우므로 지역 검색으로는 계속 노출된다.
-- (WelfareCrisisTagger.OPERATOR_MARKERS와 동일한 고정밀 시설/운영 마커. 긱워커 개인 혜택인
--  '플랫폼 종사자 …'는 매칭되지 않도록 '종사자'는 포함하지 않는다.)
UPDATE welfare_benefits
SET crisis_tags = '[]'::jsonb
WHERE crisis_tags <> '[]'::jsonb
  AND service_name ~ '기능보강|운영비|운영지원|운영 지원|위탁운영|환경개선|개보수|리모델링|건립|시설보수|시설 보수|시설개선|시설 개선|설치·운영|설치운영';
