package com.gorani.ecodrive.notification.scheduler;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.insurance.domain.InsuranceContract;
import com.gorani.ecodrive.insurance.repository.InsuranceContractRepository;
import com.gorani.ecodrive.notification.domain.NotificationType;
import com.gorani.ecodrive.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsuranceExpiryNotificationScheduler {

    private final InsuranceContractRepository insuranceContractRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *", zone = TimeZoneConstants.ASIA_SEOUL)
    public void notifyInsuranceExpiringSoon() {
        LocalDateTime from = LocalDateTime.now().plusDays(30).toLocalDate().atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        List<InsuranceContract> contracts = insuranceContractRepository.findActiveContractsEndingBetween(from, to);
        log.info("보험 만료 D-30 알림 대상 조회. count={}", contracts.size());

        for (InsuranceContract contract : contracts) {
            try {
                notificationService.save(
                        contract.getUser().getId(),
                        NotificationType.INSURANCE_EXPIRY,
                        "보험 만료 예정",
                        contract.getInsuranceProduct().getProductName() + " 보험이 30일 후 만료됩니다."
                );
            } catch (Exception e) {
                log.error("보험 만료 알림 저장 실패. contractId={}", contract.getId(), e);
            }
        }
    }
}
