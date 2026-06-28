-- CrisFin V3: Seed Mock MyData Profiles

INSERT INTO mock_mydata_profiles (persona, financial_data)
VALUES

-- ============================================================
-- OFFICE_WORKER — 일반 직장인
-- ============================================================
(
    'OFFICE_WORKER',
    '{
        "incomeType": "SALARY",
        "monthlyIncome": 3500000,
        "annualIncome": 42000000,
        "bankAccounts": [
            {"bank": "신한은행", "accountType": "입출금", "balance": 2850000},
            {"bank": "카카오뱅크", "accountType": "자유적금", "balance": 4500000}
        ],
        "cards": [
            {"issuer": "신한카드", "cardType": "신용", "monthlyUsage": 780000, "creditLimit": 5000000},
            {"issuer": "현대카드", "cardType": "체크", "monthlyUsage": 320000, "creditLimit": null}
        ],
        "loans": [
            {
                "lender": "국민은행",
                "loanType": "신용대출",
                "principal": 15000000,
                "outstandingBalance": 12400000,
                "interestRate": 4.5,
                "monthlyPayment": 280000
            }
        ],
        "insurance": [
            {"company": "삼성화재", "productName": "실손의료보험", "monthlyPremium": 85000, "coverageType": "실손"},
            {"company": "교보생명", "productName": "종신보험", "monthlyPremium": 120000, "coverageType": "생명"}
        ],
        "autoTransfers": [
            {"description": "월세", "amount": 550000, "transferDay": 25},
            {"description": "전기요금", "amount": 75000, "transferDay": 15},
            {"description": "통신비", "amount": 55000, "transferDay": 10},
            {"description": "OTT 구독", "amount": 17000, "transferDay": 5}
        ],
        "nationalPension": {"monthlyContribution": 157500, "totalAccumulated": 8200000},
        "creditScore": 782
    }'::JSONB
),

-- ============================================================
-- SELF_EMPLOYED — 자영업자
-- ============================================================
(
    'SELF_EMPLOYED',
    '{
        "incomeType": "BUSINESS",
        "monthlyIncome": 4200000,
        "annualIncome": 50400000,
        "monthlyRevenue": 12000000,
        "monthlyBusinessExpenses": 7800000,
        "bankAccounts": [
            {"bank": "기업은행", "accountType": "사업자통장", "balance": 3200000},
            {"bank": "신한은행", "accountType": "개인입출금", "balance": 1500000}
        ],
        "cards": [
            {"issuer": "BC카드", "cardType": "사업자신용", "monthlyUsage": 2400000, "creditLimit": 10000000},
            {"issuer": "삼성카드", "cardType": "신용", "monthlyUsage": 950000, "creditLimit": 8000000}
        ],
        "loans": [
            {
                "lender": "중소기업은행",
                "loanType": "사업자대출",
                "principal": 50000000,
                "outstandingBalance": 43500000,
                "interestRate": 5.8,
                "monthlyPayment": 850000
            },
            {
                "lender": "카카오뱅크",
                "loanType": "사장님대출",
                "principal": 10000000,
                "outstandingBalance": 8700000,
                "interestRate": 7.2,
                "monthlyPayment": 220000
            }
        ],
        "insurance": [
            {"company": "DB손해보험", "productName": "사업장화재보험", "monthlyPremium": 95000, "coverageType": "재산"},
            {"company": "메리츠화재", "productName": "실손의료보험", "monthlyPremium": 110000, "coverageType": "실손"}
        ],
        "autoTransfers": [
            {"description": "사무실 임대료", "amount": 1200000, "transferDay": 1},
            {"description": "전기·수도·가스", "amount": 180000, "transferDay": 10},
            {"description": "통신비(사업자)", "amount": 88000, "transferDay": 15}
        ],
        "nationalPension": {"monthlyContribution": 189000, "totalAccumulated": 15600000},
        "creditScore": 694
    }'::JSONB
),

-- ============================================================
-- FREELANCER — 프리랜서
-- ============================================================
(
    'FREELANCER',
    '{
        "incomeType": "FREELANCE",
        "monthlyIncome": 2800000,
        "annualIncome": 33600000,
        "incomeVariability": "HIGH",
        "bankAccounts": [
            {"bank": "토스뱅크", "accountType": "입출금", "balance": 1850000},
            {"bank": "국민은행", "accountType": "비상금통장", "balance": 2200000}
        ],
        "cards": [
            {"issuer": "토스카드", "cardType": "체크", "monthlyUsage": 420000, "creditLimit": null},
            {"issuer": "현대카드", "cardType": "신용", "monthlyUsage": 280000, "creditLimit": 2000000}
        ],
        "loans": [
            {
                "lender": "카카오뱅크",
                "loanType": "신용대출",
                "principal": 5000000,
                "outstandingBalance": 3800000,
                "interestRate": 6.9,
                "monthlyPayment": 120000
            }
        ],
        "insurance": [
            {"company": "삼성화재", "productName": "실손의료보험", "monthlyPremium": 72000, "coverageType": "실손"}
        ],
        "autoTransfers": [
            {"description": "월세", "amount": 450000, "transferDay": 25},
            {"description": "통신비", "amount": 45000, "transferDay": 10},
            {"description": "클라우드·툴 구독", "amount": 35000, "transferDay": 5}
        ],
        "nationalPension": {"monthlyContribution": 126000, "totalAccumulated": 3400000},
        "creditScore": 651
    }'::JSONB
),

-- ============================================================
-- LAID_OFF — 실직자 (소득 없음, 재정 취약)
-- ============================================================
(
    'LAID_OFF',
    '{
        "incomeType": "NONE",
        "monthlyIncome": 0,
        "annualIncome": 0,
        "unemploymentBenefitStatus": "APPLYING",
        "bankAccounts": [
            {"bank": "신한은행", "accountType": "입출금", "balance": 620000},
            {"bank": "카카오뱅크", "accountType": "자유적금", "balance": 500000}
        ],
        "cards": [
            {"issuer": "신한카드", "cardType": "신용", "monthlyUsage": 180000, "creditLimit": 3000000, "delinquentAmount": 0}
        ],
        "loans": [
            {
                "lender": "신한은행",
                "loanType": "신용대출",
                "principal": 8000000,
                "outstandingBalance": 7600000,
                "interestRate": 8.5,
                "monthlyPayment": 195000,
                "isOverdue": false
            },
            {
                "lender": "저축은행",
                "loanType": "중금리대출",
                "principal": 3000000,
                "outstandingBalance": 2900000,
                "interestRate": 14.9,
                "monthlyPayment": 95000,
                "isOverdue": false
            }
        ],
        "insurance": [
            {"company": "흥국생명", "productName": "실손의료보험", "monthlyPremium": 68000, "coverageType": "실손", "paymentStatus": "OVERDUE_1M"}
        ],
        "autoTransfers": [
            {"description": "월세", "amount": 380000, "transferDay": 25},
            {"description": "통신비", "amount": 45000, "transferDay": 10}
        ],
        "nationalPension": {"monthlyContribution": 0, "paymentExemptionApplied": true, "totalAccumulated": 4100000},
        "creditScore": 592,
        "previousJob": {"company": "중소기업", "tenure": 26, "monthlyIncomePrior": 2900000}
    }'::JSONB
),

-- ============================================================
-- PUBLIC_SERVANT — 공무원 (고소득·안정, 대출 규모 큼)
-- ============================================================
(
    'PUBLIC_SERVANT',
    '{
        "incomeType": "SALARY",
        "monthlyIncome": 4800000,
        "annualIncome": 57600000,
        "employerType": "GOVERNMENT",
        "bankAccounts": [
            {"bank": "농협은행", "accountType": "급여통장", "balance": 6500000},
            {"bank": "국민은행", "accountType": "정기예금", "balance": 20000000}
        ],
        "cards": [
            {"issuer": "농협카드", "cardType": "신용", "monthlyUsage": 1100000, "creditLimit": 10000000},
            {"issuer": "우리카드", "cardType": "체크", "monthlyUsage": 400000, "creditLimit": null}
        ],
        "loans": [
            {
                "lender": "공무원연금공단",
                "loanType": "주택담보대출(모기지)",
                "principal": 250000000,
                "outstandingBalance": 218000000,
                "interestRate": 3.2,
                "monthlyPayment": 1150000
            },
            {
                "lender": "농협은행",
                "loanType": "전세자금대출",
                "principal": 0,
                "outstandingBalance": 0,
                "interestRate": null,
                "monthlyPayment": 0
            }
        ],
        "insurance": [
            {"company": "공무원연금공단", "productName": "공무원단체보험", "monthlyPremium": 45000, "coverageType": "단체"},
            {"company": "삼성생명", "productName": "변액종신보험", "monthlyPremium": 220000, "coverageType": "생명"},
            {"company": "현대해상", "productName": "실손의료보험", "monthlyPremium": 92000, "coverageType": "실손"}
        ],
        "autoTransfers": [
            {"description": "주택담보대출 원리금", "amount": 1150000, "transferDay": 25},
            {"description": "아파트 관리비", "amount": 210000, "transferDay": 20},
            {"description": "통신비", "amount": 65000, "transferDay": 10},
            {"description": "자녀 학원비", "amount": 480000, "transferDay": 5}
        ],
        "nationalPension": {
            "type": "GOVERNMENT_PENSION",
            "monthlyContribution": 432000,
            "totalAccumulated": 87000000,
            "estimatedMonthlyBenefit": 1850000
        },
        "creditScore": 851
    }'::JSONB
)

ON CONFLICT (persona) DO NOTHING;
