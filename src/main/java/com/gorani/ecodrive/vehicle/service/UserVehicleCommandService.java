package com.gorani.ecodrive.vehicle.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVehicleCommandService {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    @Transactional
    public void deactivateMyVehicle(Long userId, Long userVehicleId) {
        log.info("내 차량 삭제(비활성화) 시작. userId={}, userVehicleId={}", userId, userVehicleId);

        int updatedRows = jdbcTemplate.update(
                """
                update user_vehicles
                set status = 'INACTIVE',
                    updated_at = ?
                where id = ?
                  and user_id = ?
                  and status = 'ACTIVE'
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                userVehicleId,
                userId
        );

        if (updatedRows == 0) {
            log.warn("내 차량 삭제(비활성화) 실패. 활성 차량을 찾지 못함. userId={}, userVehicleId={}", userId, userVehicleId);
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        reassignRepresentativeVehicle(userId, userVehicleId);
        log.info("내 차량 삭제(비활성화) 완료. userId={}, userVehicleId={}", userId, userVehicleId);
    }

    private void reassignRepresentativeVehicle(Long userId, Long deletedUserVehicleId) {
        User user = userService.getById(userId);
        if (!deletedUserVehicleId.equals(user.getRepresentativeUserVehicleId())) {
            return;
        }

        // 삭제된 대표 차량이었으면, 남은 활성 차량 중 최신 등록 차량으로 대표를 재지정한다.
        Long fallbackUserVehicleId = jdbcTemplate.query(
                """
                select uv.id
                from user_vehicles uv
                where uv.user_id = ?
                  and uv.status = 'ACTIVE'
                order by uv.registered_at desc, uv.id desc
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId
        );

        user.updateRepresentativeUserVehicleId(fallbackUserVehicleId);
        log.info("대표 차량 재지정 완료. userId={}, deletedUserVehicleId={}, newRepresentativeUserVehicleId={}",
                userId, deletedUserVehicleId, fallbackUserVehicleId);
    }
}

