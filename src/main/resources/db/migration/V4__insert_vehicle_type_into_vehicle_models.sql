-- 국산차 vehicle_models 시드 데이터
-- 기준
-- 1. manufacturer: 국산 브랜드
-- 2. fuel_type: GASOLINE / DIESEL / HYBRID
-- 3. body_type: SMALL / MIDSIZE / LARGE
-- 4. 동일 차종이라도 연료 타입이 다르면 별도 row로 분리
INSERT INTO vehicle_models
(manufacturer, model_name, model_year, fuel_type, body_type, base_insurance_factor, created_at)
VALUES
    ('현대', '캐스퍼', 2025, 'GASOLINE', 'SMALL', 0.9300, CURRENT_TIMESTAMP),
    ('현대', '베뉴', 2025, 'GASOLINE', 'SMALL', 0.9500, CURRENT_TIMESTAMP),
    ('현대', '아반떼', 2025, 'GASOLINE', 'SMALL', 0.9700, CURRENT_TIMESTAMP),
    ('현대', '코나', 2025, 'GASOLINE', 'SMALL', 0.9800, CURRENT_TIMESTAMP),
    ('현대', '소나타', 2025, 'GASOLINE', 'MIDSIZE', 1.0000, CURRENT_TIMESTAMP),
    ('현대', '소나타', 2025, 'DIESEL', 'MIDSIZE', 1.0300, CURRENT_TIMESTAMP),
    ('현대', '소나타', 2025, 'HYBRID', 'MIDSIZE', 0.9800, CURRENT_TIMESTAMP),
    ('현대', '투싼', 2025, 'GASOLINE', 'MIDSIZE', 1.0200, CURRENT_TIMESTAMP),
    ('현대', '투싼', 2025, 'DIESEL', 'MIDSIZE', 1.0500, CURRENT_TIMESTAMP),
    ('현대', '싼타페', 2025, 'GASOLINE', 'LARGE', 1.0800, CURRENT_TIMESTAMP),
    ('현대', '싼타페', 2025, 'HYBRID', 'LARGE', 1.0500, CURRENT_TIMESTAMP),
    ('현대', '그랜저', 2025, 'GASOLINE', 'LARGE', 1.1000, CURRENT_TIMESTAMP),
    ('현대', '그랜저', 2025, 'HYBRID', 'LARGE', 1.0700, CURRENT_TIMESTAMP),
    ('현대', '팰리세이드', 2025, 'GASOLINE', 'LARGE', 1.1400, CURRENT_TIMESTAMP),

    ('기아', '모닝', 2025, 'GASOLINE', 'SMALL', 0.9200, CURRENT_TIMESTAMP),
    ('기아', '레이', 2025, 'GASOLINE', 'SMALL', 0.9400, CURRENT_TIMESTAMP),
    ('기아', 'K3', 2025, 'GASOLINE', 'SMALL', 0.9700, CURRENT_TIMESTAMP),
    ('기아', '셀토스', 2025, 'GASOLINE', 'SMALL', 0.9900, CURRENT_TIMESTAMP),
    ('기아', 'K5', 2025, 'GASOLINE', 'MIDSIZE', 1.0000, CURRENT_TIMESTAMP),
    ('기아', 'K5', 2025, 'DIESEL', 'MIDSIZE', 1.0300, CURRENT_TIMESTAMP),
    ('기아', 'K5', 2025, 'HYBRID', 'MIDSIZE', 0.9800, CURRENT_TIMESTAMP),
    ('기아', '스포티지', 2025, 'GASOLINE', 'MIDSIZE', 1.0300, CURRENT_TIMESTAMP),
    ('기아', '스포티지', 2025, 'DIESEL', 'MIDSIZE', 1.0600, CURRENT_TIMESTAMP),
    ('기아', '쏘렌토', 2025, 'GASOLINE', 'LARGE', 1.1000, CURRENT_TIMESTAMP),
    ('기아', '쏘렌토', 2025, 'HYBRID', 'LARGE', 1.0700, CURRENT_TIMESTAMP),
    ('기아', '카니발', 2025, 'GASOLINE', 'LARGE', 1.1500, CURRENT_TIMESTAMP),
    ('기아', '카니발', 2025, 'DIESEL', 'LARGE', 1.1800, CURRENT_TIMESTAMP),
    ('기아', 'K8', 2025, 'GASOLINE', 'LARGE', 1.0900, CURRENT_TIMESTAMP),
    ('기아', 'K8', 2025, 'HYBRID', 'LARGE', 1.0600, CURRENT_TIMESTAMP),

    ('제네시스', 'G70', 2025, 'GASOLINE', 'MIDSIZE', 1.0900, CURRENT_TIMESTAMP),
    ('제네시스', 'GV70', 2025, 'GASOLINE', 'MIDSIZE', 1.1300, CURRENT_TIMESTAMP),
    ('제네시스', 'G80', 2025, 'GASOLINE', 'LARGE', 1.1600, CURRENT_TIMESTAMP),
    ('제네시스', 'GV80', 2025, 'GASOLINE', 'LARGE', 1.1900, CURRENT_TIMESTAMP),
    ('제네시스', 'G90', 2025, 'GASOLINE', 'LARGE', 1.2200, CURRENT_TIMESTAMP),

    ('KG모빌리티', '티볼리', 2025, 'GASOLINE', 'SMALL', 0.9600, CURRENT_TIMESTAMP),
    ('KG모빌리티', '티볼리', 2025, 'DIESEL', 'SMALL', 0.9900, CURRENT_TIMESTAMP),
    ('KG모빌리티', '토레스', 2025, 'GASOLINE', 'MIDSIZE', 1.0400, CURRENT_TIMESTAMP),
    ('KG모빌리티', '렉스턴', 2025, 'DIESEL', 'LARGE', 1.1300, CURRENT_TIMESTAMP),
    ('KG모빌리티', '렉스턴 스포츠', 2025, 'DIESEL', 'LARGE', 1.1200, CURRENT_TIMESTAMP),

    ('르노코리아', 'XM3', 2025, 'GASOLINE', 'SMALL', 0.9700, CURRENT_TIMESTAMP),
    ('르노코리아', 'SM6', 2025, 'GASOLINE', 'MIDSIZE', 1.0000, CURRENT_TIMESTAMP),
    ('르노코리아', 'QM6', 2025, 'GASOLINE', 'MIDSIZE', 1.0200, CURRENT_TIMESTAMP),
    ('르노코리아', 'QM6', 2025, 'DIESEL', 'MIDSIZE', 1.0500, CURRENT_TIMESTAMP),

    ('쉐보레', '스파크', 2025, 'GASOLINE', 'SMALL', 0.9100, CURRENT_TIMESTAMP),
    ('쉐보레', '트랙스 크로스오버', 2025, 'GASOLINE', 'SMALL', 0.9800, CURRENT_TIMESTAMP),
    ('쉐보레', '말리부', 2025, 'GASOLINE', 'MIDSIZE', 1.0100, CURRENT_TIMESTAMP),
    ('쉐보레', '트래버스', 2025, 'GASOLINE', 'LARGE', 1.1400, CURRENT_TIMESTAMP);

