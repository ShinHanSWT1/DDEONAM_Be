package com.gorani.ecodrive.pay.scheduler;

import com.gorani.ecodrive.pay.service.PayChargeAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayChargeAttemptScheduler {

    private final PayChargeAttemptService payChargeAttemptService;

    @Scheduled(cron = "0 */5 * * * *")
    public void expirePreparedAttempts() {
        int expiredCount = payChargeAttemptService.expirePreparedAttempts();
        if (expiredCount > 0) {
            log.info("만료된 충전 시도 상태 전이 완료. expiredCount={}", expiredCount);
        }
    }
}
