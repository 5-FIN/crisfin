-- CrisFin V11: 마이데이터 mock에 위기별 공공데이터(publicData) 추가
--
-- 진단 폼이 손입력 대신 마이데이터에서 위기별 필수값을 자동으로 가져오도록 하기 위한 mock.
-- publicData 는 국민건강보험공단·근로복지공단 등 공공 마이데이터를 모사한 값으로,
-- BenefitRuleEngine 이 요구하는 위기별 프로필 필드에 그대로 매핑된다.
--   - annualOutOfPocketMedical  : 최근 1년 본인부담 의료비(원) → 본인부담상한제 환급(입원/수술)
--   - employmentInsuranceMonths : 고용보험 가입기간(개월)        → 실업급여(실직)
--   - involuntarySeparation     : 비자발적 이직 여부             → 실업급여(실직)
--   - careGrade                 : 장기요양 등급(1~5)            → 장기요양급여(간병)
--
-- 페르소나별로 현실감 있는 값을 넣되, 데모에서 각 위기 경로가 실제 금액을 산정하도록 구성.

UPDATE mock_mydata_profiles
SET financial_data = financial_data || jsonb_build_object(
    'publicData', jsonb_build_object(
        'annualOutOfPocketMedical', 3200000,
        'employmentInsuranceMonths', 36,
        'involuntarySeparation', true,
        'careGrade', 3
    ))
WHERE persona = 'OFFICE_WORKER';

UPDATE mock_mydata_profiles
SET financial_data = financial_data || jsonb_build_object(
    'publicData', jsonb_build_object(
        'annualOutOfPocketMedical', 2900000,
        'employmentInsuranceMonths', 12,
        'involuntarySeparation', false,
        'careGrade', 4
    ))
WHERE persona = 'SELF_EMPLOYED';

UPDATE mock_mydata_profiles
SET financial_data = financial_data || jsonb_build_object(
    'publicData', jsonb_build_object(
        'annualOutOfPocketMedical', 2400000,
        'employmentInsuranceMonths', 18,
        'involuntarySeparation', true,
        'careGrade', 3
    ))
WHERE persona = 'FREELANCER';

UPDATE mock_mydata_profiles
SET financial_data = financial_data || jsonb_build_object(
    'publicData', jsonb_build_object(
        'annualOutOfPocketMedical', 2100000,
        'employmentInsuranceMonths', 26,
        'involuntarySeparation', true,
        'careGrade', 4
    ))
WHERE persona = 'LAID_OFF';

UPDATE mock_mydata_profiles
SET financial_data = financial_data || jsonb_build_object(
    'publicData', jsonb_build_object(
        'annualOutOfPocketMedical', 2600000,
        'employmentInsuranceMonths', 60,
        'involuntarySeparation', false,
        'careGrade', 2
    ))
WHERE persona = 'PUBLIC_SERVANT';
