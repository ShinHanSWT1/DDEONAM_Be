-- 안전점수 미션은 기간 종료 시점 평가 성격을 반영해 "이상 유지" 문구로 정리한다.
update mission_policies
set title = '주간 안전점수 88점 이상 유지',
    description = '주간 마지막 날 기준 안전점수 88점 이상 유지',
    updated_at = current_timestamp
where mission_type = 'WEEKLY'
  and target_type = 'SAFE_SCORE_GTE'
  and category = 'SAFETY';

update mission_policies
set title = '월간 안전점수 90점 이상 유지',
    description = '월간 마지막 날 기준 안전점수 90점 이상 유지',
    updated_at = current_timestamp
where mission_type = 'MONTHLY'
  and target_type = 'SAFE_SCORE_GTE'
  and category = 'SAFETY';

