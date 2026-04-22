package com.gorani.ecodrive.vehicle.repository;

import com.gorani.ecodrive.vehicle.domain.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVehicleRepository extends JpaRepository<UserVehicle, Long> {
    boolean existsByUserId(Long userId);
    boolean existsByUserIdAndStatus(Long userId, String status);

    boolean existsByIdAndUser_Id(Long id, Long userId);
    boolean existsByIdAndUser_IdAndStatus(Long id, Long userId, String status);
}
