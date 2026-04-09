package com.gorani.ecodrive.mission.service;

import com.gorani.ecodrive.mission.domain.*;
import com.gorani.ecodrive.mission.repository.MissionPolicyRepository;
import com.gorani.ecodrive.mission.repository.UserMissionRepository;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = MissionServiceTests.TestConfig.class)
class MissionServiceTests {

    @SpringBootConfiguration
    @Import(MissionService.class)
    static class TestConfig {

        @Bean
        MissionPolicyRepository missionPolicyRepository() {
            return Mockito.mock(MissionPolicyRepository.class);
        }

        @Bean
        UserMissionRepository userMissionRepository() {
            return Mockito.mock(UserMissionRepository.class);
        }

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }
    }

    @Autowired
    private MissionService missionService;

    @Autowired
    private MissionPolicyRepository missionPolicyRepository;

    @Autowired
    private UserMissionRepository userMissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void ensureAssignedSkipsWhenAlreadyAssigned() {
        given(userMissionRepository.existsAssignedInPeriod(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(MissionType.DAILY),
                ArgumentMatchers.any(LocalDate.class)
        )).willReturn(true);

        missionService.ensureAssigned(1L, MissionType.DAILY);

        verify(userRepository, never()).findById(ArgumentMatchers.anyLong());
        verify(userMissionRepository, never()).saveAll(ArgumentMatchers.anyList());
    }

    @Test
    void ensureAssignedSavesAssignmentsWhenNotAssigned() {
        User user = Mockito.mock(User.class);
        MissionPolicy policy1 = buildPolicyMock(MissionType.DAILY, MissionCategory.ECO, MissionTargetType.DISTANCE_KM_GTE, "20.00", 80, 100);
        MissionPolicy policy2 = buildPolicyMock(MissionType.DAILY, MissionCategory.SAFETY, MissionTargetType.SAFE_SCORE_GTE, "85.00", 100, 100);

        given(userMissionRepository.existsAssignedInPeriod(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(MissionType.DAILY),
                ArgumentMatchers.any(LocalDate.class)
        )).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(missionPolicyRepository.findAllByMissionTypeAndStatus(MissionType.DAILY, MissionPolicyStatus.ACTIVE))
                .willReturn(List.of(policy1, policy2));

        missionService.ensureAssigned(1L, MissionType.DAILY);

        verify(userMissionRepository, times(1)).saveAll(ArgumentMatchers.anyList());
    }

    private MissionPolicy buildPolicyMock(
            MissionType missionType,
            MissionCategory category,
            MissionTargetType targetType,
            String targetValue,
            int rewardPoint,
            int weight
    ) {
        MissionPolicy policy = Mockito.mock(MissionPolicy.class);
        given(policy.getMissionType()).willReturn(missionType);
        given(policy.getCategory()).willReturn(category);
        given(policy.getTargetType()).willReturn(targetType);
        given(policy.getTargetValue()).willReturn(new BigDecimal(targetValue));
        given(policy.getRewardPoint()).willReturn(rewardPoint);
        given(policy.getWeight()).willReturn(weight);
        return policy;
    }
}
