package com.gorani.ecodrive.user.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.infra.s3.S3Service;
import com.gorani.ecodrive.insurance.repository.UserInsuranceRepository;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.repository.UserRepository;
import com.gorani.ecodrive.vehicle.repository.UserVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final UserVehicleRepository userVehicleRepository;
    private final UserInsuranceRepository userInsuranceRepository;

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean calculateOnboardingCompleted(Long userId) {
        boolean hasVehicle = userVehicleRepository.existsByUserId(userId);
        boolean hasLinkedInsurance = userInsuranceRepository.existsByUserVehicleUserId(userId);
        return hasVehicle && hasLinkedInsurance;
    }

    @Transactional
    public String uploadProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String imageUrl = s3Service.upload(file, "users/profile");

        user.updateProfile(
                user.getEmail(),
                user.getNickname(),
                imageUrl
        );

        return imageUrl;
    }
}
