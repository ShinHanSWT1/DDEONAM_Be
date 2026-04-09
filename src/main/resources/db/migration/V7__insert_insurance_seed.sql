-- 1. 보험사 (insurance_companies)
INSERT INTO insurance_companies (id, company_name, code, status, created_at)
VALUES (1, '삼성화재', 'SAMSUNG', 'ACTIVE', NOW()),
       (2, '현대해상', 'HYUNDAI', 'ACTIVE', NOW()),
       (3, 'DB손해보험', 'DB_INSURANCE', 'ACTIVE', NOW()),
       (4, 'KB손해보험', 'KB_INSURANCE', 'ACTIVE', NOW());

-- 보험 상품 (insurance_products)
INSERT INTO insurance_products (id, insurance_company_id, product_name, base_amount, discount_rate, status, created_at)
VALUES (1, 1, '에코드라이브 자동차보험', 850000, 0.00, 'ON_SALE', NOW()),
       (2, 2, '하이카 에코보험', 920000, 0.00, 'ON_SALE', NOW()),
       (3, 1, '온보딩 표준형', 120000, 0.00, 'ACTIVE', NOW()),
       (4, 3, 'DB 에코드라이브', 880000, 0.00, 'ON_SALE', NOW()),
       (5, 4, 'KB 그린카 보험', 870000, 0.00, 'ON_SALE', NOW());
-- 1. [삼성화재 - 상품 ID 1] 특약 세트
INSERT INTO insurance_coverages (insurance_products_id, coverage_name, coverage_amount, category,
                                 is_required, plan_type, status, created_at)
VALUES
-- BASIC
(1, '대인배상 I', 150000000, '기본배상', true, 'BASIC', 'ACTIVE', NOW()),
(1, '대물배상', 20000000, '기본배상', true, 'BASIC', 'ACTIVE', NOW()),
(1, '긴급출동서비스', 0, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
(1, '견인서비스', 300000, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
(1, '비상급유서비스', 50000, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
-- STANDARD
(1, '자기신체사고', 30000000, '신체보상', true, 'STANDARD', 'ACTIVE', NOW()),
(1, '자기차량손해', 50000000, '차량보상', false, 'STANDARD', 'ACTIVE', NOW()),
(1, '대인배상 II', 300000000, '기본배상', false, 'STANDARD', 'ACTIVE', NOW()),
(1, '법률비용지원', 3000000, '법률지원', false, 'STANDARD', 'ACTIVE', NOW()),
(1, '자동차사고 부상치료비', 10000000, '신체보상', false, 'STANDARD', 'ACTIVE', NOW()),
-- PREMIUM
(1, '자동차상해', 100000000, '신체보상', true, 'PREMIUM', 'ACTIVE', NOW()),
(1, '무보험차상해', 200000000, '추가보상', false, 'PREMIUM', 'ACTIVE', NOW()),
(1, '렌터카비용지원', 3000000, '서비스', false, 'PREMIUM', 'ACTIVE', NOW()),
(1, '해외운전담보', 50000000, '추가보상', false, 'PREMIUM', 'ACTIVE', NOW()),
(1, '마일리지특약', 0, '할인특약', false, 'PREMIUM', 'ACTIVE', NOW());

-- 2. [현대해상 - 상품 ID 2] 특약 세트
INSERT INTO insurance_coverages (insurance_products_id, coverage_name, coverage_amount, category,
                                 is_required, plan_type, status, created_at)
VALUES
-- BASIC
(2, '대인배상 I', 150000000, '기본배상', true, 'BASIC', 'ACTIVE', NOW()),
(2, '대물배상', 20000000, '기본배상', true, 'BASIC', 'ACTIVE', NOW()),
(2, '긴급출동서비스', 0, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
(2, '배터리충전서비스', 100000, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
(2, '타이어교체서비스', 200000, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
-- STANDARD
(2, '자기신체사고', 30000000, '신체보상', true, 'STANDARD', 'ACTIVE', NOW()),
(2, '자기차량손해', 50000000, '차량보상', false, 'STANDARD', 'ACTIVE', NOW()),
(2, '대인배상 II', 300000000, '기본배상', false, 'STANDARD', 'ACTIVE', NOW()),
(2, '자녀사고처리지원금', 5000000, '신체보상', false, 'STANDARD', 'ACTIVE', NOW()),
(2, '스마트폰파손손해', 500000, '차량보상', false, 'STANDARD', 'ACTIVE', NOW()),
-- PREMIUM
(2, '자동차상해', 100000000, '신체보상', true, 'PREMIUM', 'ACTIVE', NOW()),
(2, '무보험차상해', 200000000, '추가보상', false, 'PREMIUM', 'ACTIVE', NOW()),
(2, '렌터카비용지원', 3000000, '서비스', false, 'PREMIUM', 'ACTIVE', NOW()),
(2, '운전자보험', 30000000, '추가보상', false, 'PREMIUM', 'ACTIVE', NOW()),
(2, '블랙박스장착할인', 0, '할인특약', false, 'PREMIUM', 'ACTIVE', NOW());

-- 3. [DB손해보험 - 상품 ID 4] 특약 세트 (이전 이미지 기준 DB손해보험은 ID 4)
INSERT INTO insurance_coverages (insurance_products_id, coverage_name, coverage_amount, category,
                                 is_required, plan_type, status, created_at)
VALUES
-- BASIC
(4, '대인배상 I', 150000000, '기본배상', true, 'BASIC', 'ACTIVE', NOW()),
(4, '대물배상', 20000000, '기본배상', true, 'BASIC', 'ACTIVE', NOW()),
(4, '긴급출동서비스', 0, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
(4, '잠금장치해제서비스', 100000, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
(4, '사고현장출동서비스', 0, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
-- STANDARD
(4, '자기신체사고', 30000000, '신체보상', true, 'STANDARD', 'ACTIVE', NOW()),
(4, '자기차량손해', 50000000, '차량보상', false, 'STANDARD', 'ACTIVE', NOW()),
(4, '대인배상 II', 300000000, '기본배상', false, 'STANDARD', 'ACTIVE', NOW()),
(4, '법률비용지원', 3000000, '법률지원', false, 'STANDARD', 'ACTIVE', NOW()),
(4, '주차장사고보상', 10000000, '차량보상', false, 'STANDARD', 'ACTIVE', NOW()),
-- PREMIUM
(4, '자동차상해', 100000000, '신체보상', true, 'PREMIUM', 'ACTIVE', NOW()),
(4, '무보험차상해', 200000000, '추가보상', false, 'PREMIUM', 'ACTIVE', NOW()),
(4, '렌터카비용지원', 3000000, '서비스', false, 'PREMIUM', 'ACTIVE', NOW()),
(4, '자연재해차량손해', 20000000, '추가보상', false, 'PREMIUM', 'ACTIVE', NOW()),
(4, '에코드라이브할인', 0, '할인특약', false, 'PREMIUM', 'ACTIVE', NOW());

-- 4. [KB손해보험 - 상품 ID 5] 특약 세트 (이전 이미지 기준 KB손해보험은 ID 5)
INSERT INTO insurance_coverages (insurance_products_id, coverage_name, coverage_amount, category,
                                 is_required, plan_type, status, created_at)
VALUES
-- BASIC
(5, '대인배상 I', 150000000, '기본배상', true, 'BASIC', 'ACTIVE', NOW()),
(5, '대물배상', 20000000, '기본배상', true, 'BASIC', 'ACTIVE', NOW()),
(5, '긴급출동서비스', 0, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
(5, '견인서비스', 300000, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
(5, '대리운전비용지원', 200000, '서비스', false, 'BASIC', 'ACTIVE', NOW()),
-- STANDARD
(5, '자기신체사고', 30000000, '신체보상', true, 'STANDARD', 'ACTIVE', NOW()),
(5, '자기차량손해', 50000000, '차량보상', false, 'STANDARD', 'ACTIVE', NOW()),
(5, '대인배상 II', 300000000, '기본배상', false, 'STANDARD', 'ACTIVE', NOW()),
(5, '교통사고처리지원금', 5000000, '법률지원', false, 'STANDARD', 'ACTIVE', NOW()),
(5, '차량부속품손해', 2000000, '차량보상', false, 'STANDARD', 'ACTIVE', NOW()),
-- PREMIUM
(5, '자동차상해', 100000000, '신체보상', true, 'PREMIUM', 'ACTIVE', NOW()),
(5, '무보험차상해', 200000000, '추가보상', false, 'PREMIUM', 'ACTIVE', NOW()),
(5, '렌터카비용지원', 3000000, '서비스', false, 'PREMIUM', 'ACTIVE', NOW()),
(5, '해외운전담보', 50000000, '추가보상', false, 'PREMIUM', 'ACTIVE', NOW()),
(5, '안전운전마일리지할인', 0, '할인특약', false, 'PREMIUM', 'ACTIVE', NOW());