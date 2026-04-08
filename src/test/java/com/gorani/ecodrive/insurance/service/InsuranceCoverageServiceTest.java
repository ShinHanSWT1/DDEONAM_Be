package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverage;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverageStatus;
import com.gorani.ecodrive.insurance.domain.InsurancePlanType;
import com.gorani.ecodrive.insurance.domain.InsuranceProduct;
import com.gorani.ecodrive.insurance.repository.InsuranceCoverageRepository;
import com.gorani.ecodrive.insurance.repository.InsuranceProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsuranceCoverageServiceTest {

    @Mock
    private InsuranceCoverageRepository insuranceCoverageRepository;

    @Mock
    private InsuranceProductRepository insuranceProductRepository;

    @InjectMocks
    private InsuranceCoverageService insuranceCoverageService;

    private InsuranceProduct mockProduct(Long id) {
        InsuranceProduct product = mock(InsuranceProduct.class);
        when(product.getId()).thenReturn(id);
        return product;
    }

    private InsuranceCoverage mockCoverage(Long id, InsurancePlanType planType, String category) {
        InsuranceCoverage coverage = mock(InsuranceCoverage.class);
        when(coverage.getId()).thenReturn(id);
        when(coverage.getPlanType()).thenReturn(planType);
        when(coverage.getCategory()).thenReturn(category);
        return coverage;
    }

    @Nested
    @DisplayName("getCoverages - 보장 목록 조회")
    class GetCoveragesTest {

        @Test
        @DisplayName("존재하지 않는 productId이면 CustomException 발생")
        void getCoverages_productNotFound_throwsCustomException() {
            when(insuranceProductRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> insuranceCoverageService.getCoverages(999L, null, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ce = (CustomException) e;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("planType과 category 모두 null이면 status만으로 조회")
        void getCoverages_noPlanTypeNoCategory_findsByStatusOnly() {
            InsuranceProduct product = mockProduct(1L);
            InsuranceCoverage coverage = mockCoverage(1L, InsurancePlanType.BASIC, "사고");
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));
            when(insuranceCoverageRepository.findAllByInsuranceProduct_IdAndStatus(1L, InsuranceCoverageStatus.ACTIVE))
                    .thenReturn(List.of(coverage));

            List<InsuranceCoverage> result = insuranceCoverageService.getCoverages(1L, null, null);

            assertThat(result).hasSize(1);
            verify(insuranceCoverageRepository).findAllByInsuranceProduct_IdAndStatus(1L, InsuranceCoverageStatus.ACTIVE);
            verify(insuranceCoverageRepository, never()).findAllByInsuranceProduct_IdAndPlanTypeAndStatus(any(), any(), any());
            verify(insuranceCoverageRepository, never()).findAllByInsuranceProduct_IdAndCategoryAndStatus(any(), any(), any());
            verify(insuranceCoverageRepository, never()).findAllByInsuranceProduct_IdAndPlanTypeAndCategoryAndStatus(any(), any(), any(), any());
        }

        @Test
        @DisplayName("planType만 있으면 planType과 status로 조회")
        void getCoverages_planTypeOnly_findsByPlanTypeAndStatus() {
            InsuranceProduct product = mockProduct(1L);
            InsuranceCoverage coverage = mockCoverage(1L, InsurancePlanType.BASIC, "사고");
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));
            when(insuranceCoverageRepository.findAllByInsuranceProduct_IdAndPlanTypeAndStatus(
                    1L, InsurancePlanType.BASIC, InsuranceCoverageStatus.ACTIVE))
                    .thenReturn(List.of(coverage));

            List<InsuranceCoverage> result = insuranceCoverageService.getCoverages(1L, "BASIC", null);

            assertThat(result).hasSize(1);
            verify(insuranceCoverageRepository).findAllByInsuranceProduct_IdAndPlanTypeAndStatus(
                    1L, InsurancePlanType.BASIC, InsuranceCoverageStatus.ACTIVE);
        }

        @Test
        @DisplayName("category만 있으면 category와 status로 조회")
        void getCoverages_categoryOnly_findsByCategoryAndStatus() {
            InsuranceProduct product = mockProduct(1L);
            InsuranceCoverage coverage = mockCoverage(1L, InsurancePlanType.STANDARD, "사고");
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));
            when(insuranceCoverageRepository.findAllByInsuranceProduct_IdAndCategoryAndStatus(
                    1L, "사고", InsuranceCoverageStatus.ACTIVE))
                    .thenReturn(List.of(coverage));

            List<InsuranceCoverage> result = insuranceCoverageService.getCoverages(1L, null, "사고");

            assertThat(result).hasSize(1);
            verify(insuranceCoverageRepository).findAllByInsuranceProduct_IdAndCategoryAndStatus(
                    1L, "사고", InsuranceCoverageStatus.ACTIVE);
        }

        @Test
        @DisplayName("planType과 category 모두 있으면 전체 조건으로 조회")
        void getCoverages_planTypeAndCategory_findsByAll() {
            InsuranceProduct product = mockProduct(1L);
            InsuranceCoverage coverage = mockCoverage(1L, InsurancePlanType.PREMIUM, "의료");
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));
            when(insuranceCoverageRepository.findAllByInsuranceProduct_IdAndPlanTypeAndCategoryAndStatus(
                    1L, InsurancePlanType.PREMIUM, "의료", InsuranceCoverageStatus.ACTIVE))
                    .thenReturn(List.of(coverage));

            List<InsuranceCoverage> result = insuranceCoverageService.getCoverages(1L, "PREMIUM", "의료");

            assertThat(result).hasSize(1);
            verify(insuranceCoverageRepository).findAllByInsuranceProduct_IdAndPlanTypeAndCategoryAndStatus(
                    1L, InsurancePlanType.PREMIUM, "의료", InsuranceCoverageStatus.ACTIVE);
        }

        @Test
        @DisplayName("planType이 소문자여도 정상 처리")
        void getCoverages_lowercasePlanType_parsesCorrectly() {
            InsuranceProduct product = mockProduct(1L);
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));
            when(insuranceCoverageRepository.findAllByInsuranceProduct_IdAndPlanTypeAndStatus(
                    1L, InsurancePlanType.BASIC, InsuranceCoverageStatus.ACTIVE))
                    .thenReturn(List.of());

            List<InsuranceCoverage> result = insuranceCoverageService.getCoverages(1L, "basic", null);

            assertThat(result).isEmpty();
            verify(insuranceCoverageRepository).findAllByInsuranceProduct_IdAndPlanTypeAndStatus(
                    1L, InsurancePlanType.BASIC, InsuranceCoverageStatus.ACTIVE);
        }

        @Test
        @DisplayName("유효하지 않은 planType이면 CustomException(INVALID_PLAN_TYPE) 발생")
        void getCoverages_invalidPlanType_throwsCustomException() {
            InsuranceProduct product = mockProduct(1L);
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> insuranceCoverageService.getCoverages(1L, "GOLD", null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ce = (CustomException) e;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INVALID_PLAN_TYPE);
                        assertThat(ce.getErrorCode().getCode()).isEqualTo("COMMON_003");
                    });
        }

        @Test
        @DisplayName("planType이 공백 문자열이면 null로 처리되어 status만으로 조회")
        void getCoverages_blankPlanType_treatedAsNull() {
            InsuranceProduct product = mockProduct(1L);
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));
            when(insuranceCoverageRepository.findAllByInsuranceProduct_IdAndStatus(1L, InsuranceCoverageStatus.ACTIVE))
                    .thenReturn(List.of());

            List<InsuranceCoverage> result = insuranceCoverageService.getCoverages(1L, "  ", null);

            assertThat(result).isEmpty();
            verify(insuranceCoverageRepository).findAllByInsuranceProduct_IdAndStatus(1L, InsuranceCoverageStatus.ACTIVE);
        }

        @Test
        @DisplayName("STANDARD planType 처리")
        void getCoverages_standardPlanType() {
            InsuranceProduct product = mockProduct(1L);
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));
            when(insuranceCoverageRepository.findAllByInsuranceProduct_IdAndPlanTypeAndStatus(
                    1L, InsurancePlanType.STANDARD, InsuranceCoverageStatus.ACTIVE))
                    .thenReturn(List.of());

            insuranceCoverageService.getCoverages(1L, "STANDARD", null);

            verify(insuranceCoverageRepository).findAllByInsuranceProduct_IdAndPlanTypeAndStatus(
                    1L, InsurancePlanType.STANDARD, InsuranceCoverageStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("getDiscountPolicy - 할인 정책 조회")
    class GetDiscountPolicyTest {

        @Test
        @DisplayName("존재하는 productId이면 InsuranceProduct 반환")
        void getDiscountPolicy_existingId_returnsProduct() {
            InsuranceProduct product = mockProduct(1L);
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));

            InsuranceProduct result = insuranceCoverageService.getDiscountPolicy(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 productId이면 CustomException 발생")
        void getDiscountPolicy_nonExistingId_throwsCustomException() {
            when(insuranceProductRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> insuranceCoverageService.getDiscountPolicy(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ce = (CustomException) e;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND);
                    });
        }
    }
}