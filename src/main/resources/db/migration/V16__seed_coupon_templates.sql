INSERT INTO coupon_templates (category, name, discount_label, valid_days, is_pay_usable, status, created_at)
VALUES
    ('fuel', 'GS칼텍스 3,000원 주유 할인권', '3,000원', 30, true, 'ACTIVE', NOW()),
    ('fuel', 'SK에너지 5,000원 주유 할인권', '5,000원', 30, true, 'ACTIVE', NOW()),
    ('fuel', 'S-OIL 10% 주유 할인권', '10%', 14, true, 'ACTIVE', NOW()),

    ('parking', '모두의주차장 1,000원 할인권', '1,000원', 30, true, 'ACTIVE', NOW()),
    ('parking', '아이파킹 2시간 무료권', '2시간', 20, true, 'ACTIVE', NOW()),

    ('wash', '코인세차 3,000원 할인권', '3,000원', 20, true, 'ACTIVE', NOW()),
    ('wash', '스팀세차 20% 할인권', '20%', 14, true, 'ACTIVE', NOW()),

    ('maintenance', '엔진오일 교환 10,000원 할인권', '10,000원', 45, true, 'ACTIVE', NOW()),
    ('maintenance', '타이어 점검 무료권', '무료', 60, true, 'ACTIVE', NOW()),
    ('maintenance', '와이퍼 교체 30% 할인권', '30%', 30, true, 'ACTIVE', NOW()),

    ('store', 'GS25 2,000원 금액권', '2,000원', 30, true, 'ACTIVE', NOW()),
    ('store', 'CU 10% 할인권', '10%', 30, true, 'ACTIVE', NOW()),

    ('cafe', '스타벅스 아메리카노 1잔', '상품권', 30, true, 'ACTIVE', NOW()),
    ('cafe', '이디야 3,000원 할인권', '3,000원', 21, true, 'ACTIVE', NOW()),
    ('cafe', '메가커피 1+1 쿠폰', '1+1', 14, true, 'ACTIVE', NOW());
