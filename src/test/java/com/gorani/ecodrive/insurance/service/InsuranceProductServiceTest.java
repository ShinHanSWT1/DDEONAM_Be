package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.domain.InsuranceProduct;
import com.gorani.ecodrive.insurance.domain.InsuranceProductStatus;
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
class InsuranceProductServiceTest {

    @Mock
    private InsuranceProductRepository insuranceProductRepository;

    @InjectMocks
    private InsuranceProductService insuranceProductService;

    private InsuranceProduct mockProduct(Long id, InsuranceProductStatus status) {
        InsuranceProduct product = mock(InsuranceProduct.class);
        when(product.getId()).thenReturn(id);
        when(product.getStatus()).thenReturn(status);
        return product;
    }

    @Nested
    @DisplayName("getProducts - 보험 상품 목록 조회")
    class GetProductsTest {

        @Test
        @DisplayName("insuranceCompanyId와 status 모두 null이면 전체 목록 반환")
        void getProducts_bothNull_returnsAll() {
            InsuranceProduct p1 = mockProduct(1L, InsuranceProductStatus.ON_SALE);
            InsuranceProduct p2 = mockProduct(2L, InsuranceProductStatus.DISCONTINUED);
            when(insuranceProductRepository.findAll()).thenReturn(List.of(p1, p2));

            List<InsuranceProduct> result = insuranceProductService.getProducts(null, null);

            assertThat(result).hasSize(2);
            verify(insuranceProductRepository).findAll();
            verify(insuranceProductRepository, never()).findAllByInsuranceCompany_Id(any());
            verify(insuranceProductRepository, never()).findAllByStatus(any());
            verify(insuranceProductRepository, never()).findAllByInsuranceCompany_IdAndStatus(any(), any());
        }

        @Test
        @DisplayName("insuranceCompanyId만 있으면 해당 회사 상품만 반환")
        void getProducts_companyIdOnly_returnsByCompanyId() {
            InsuranceProduct p1 = mockProduct(1L, InsuranceProductStatus.ON_SALE);
            when(insuranceProductRepository.findAllByInsuranceCompany_Id(10L)).thenReturn(List.of(p1));

            List<InsuranceProduct> result = insuranceProductService.getProducts(10L, null);

            assertThat(result).hasSize(1);
            verify(insuranceProductRepository).findAllByInsuranceCompany_Id(10L);
            verify(insuranceProductRepository, never()).findAll();
        }

        @Test
        @DisplayName("status만 있으면 해당 상태 상품만 반환")
        void getProducts_statusOnly_returnsByStatus() {
            InsuranceProduct p1 = mockProduct(1L, InsuranceProductStatus.ON_SALE);
            when(insuranceProductRepository.findAllByStatus(InsuranceProductStatus.ON_SALE)).thenReturn(List.of(p1));

            List<InsuranceProduct> result = insuranceProductService.getProducts(null, "ON_SALE");

            assertThat(result).hasSize(1);
            verify(insuranceProductRepository).findAllByStatus(InsuranceProductStatus.ON_SALE);
            verify(insuranceProductRepository, never()).findAll();
        }

        @Test
        @DisplayName("insuranceCompanyId와 status 모두 있으면 조합으로 조회")
        void getProducts_bothProvided_returnsByCompanyIdAndStatus() {
            InsuranceProduct p1 = mockProduct(1L, InsuranceProductStatus.ON_SALE);
            when(insuranceProductRepository.findAllByInsuranceCompany_IdAndStatus(10L, InsuranceProductStatus.ON_SALE))
                    .thenReturn(List.of(p1));

            List<InsuranceProduct> result = insuranceProductService.getProducts(10L, "ON_SALE");

            assertThat(result).hasSize(1);
            verify(insuranceProductRepository).findAllByInsuranceCompany_IdAndStatus(10L, InsuranceProductStatus.ON_SALE);
        }

        @Test
        @DisplayName("DISCONTINUED status로 조회")
        void getProducts_discontinuedStatus() {
            InsuranceProduct p1 = mockProduct(1L, InsuranceProductStatus.DISCONTINUED);
            when(insuranceProductRepository.findAllByStatus(InsuranceProductStatus.DISCONTINUED))
                    .thenReturn(List.of(p1));

            List<InsuranceProduct> result = insuranceProductService.getProducts(null, "DISCONTINUED");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("유효하지 않은 status 문자열이면 IllegalArgumentException 발생")
        void getProducts_invalidStatus_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> insuranceProductService.getProducts(null, "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("결과가 비어 있으면 빈 리스트 반환")
        void getProducts_emptyResult_returnsEmptyList() {
            when(insuranceProductRepository.findAll()).thenReturn(List.of());

            List<InsuranceProduct> result = insuranceProductService.getProducts(null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("insuranceCompanyId와 status 조합 - 회사 ID가 0이어도 동작")
        void getProducts_zeroCompanyId_usesCompanyIdFilter() {
            when(insuranceProductRepository.findAllByInsuranceCompany_Id(0L)).thenReturn(List.of());

            List<InsuranceProduct> result = insuranceProductService.getProducts(0L, null);

            assertThat(result).isEmpty();
            verify(insuranceProductRepository).findAllByInsuranceCompany_Id(0L);
        }
    }

    @Nested
    @DisplayName("getProductById - ID로 보험 상품 조회")
    class GetProductByIdTest {

        @Test
        @DisplayName("존재하는 ID이면 상품 반환")
        void getProductById_existingId_returnsProduct() {
            InsuranceProduct product = mockProduct(1L, InsuranceProductStatus.ON_SALE);
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));

            InsuranceProduct result = insuranceProductService.getProductById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 ID이면 CustomException(INSURANCE_PRODUCT_NOT_FOUND) 발생")
        void getProductById_nonExistingId_throwsCustomException() {
            when(insuranceProductRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> insuranceProductService.getProductById(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ce = (CustomException) e;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND);
                        assertThat(ce.getErrorCode().getCode()).isEqualTo("INSURANCE_002");
                    });
        }
    }

    @Nested
    @DisplayName("getDiscountPolicy - 할인 정책 조회")
    class GetDiscountPolicyTest {

        @Test
        @DisplayName("존재하는 productId이면 InsuranceProduct 반환")
        void getDiscountPolicy_existingId_returnsProduct() {
            InsuranceProduct product = mockProduct(1L, InsuranceProductStatus.ON_SALE);
            when(insuranceProductRepository.findById(1L)).thenReturn(Optional.of(product));

            InsuranceProduct result = insuranceProductService.getDiscountPolicy(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 productId이면 CustomException 발생")
        void getDiscountPolicy_nonExistingId_throwsCustomException() {
            when(insuranceProductRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> insuranceProductService.getDiscountPolicy(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ce = (CustomException) e;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("getDiscountPolicy와 getProductById는 같은 로직으로 동작")
        void getDiscountPolicy_sameLogicAsGetProductById() {
            InsuranceProduct product = mockProduct(42L, InsuranceProductStatus.ON_SALE);
            when(insuranceProductRepository.findById(42L)).thenReturn(Optional.of(product));

            InsuranceProduct byId = insuranceProductService.getProductById(42L);
            InsuranceProduct byDiscount = insuranceProductService.getDiscountPolicy(42L);

            assertThat(byId.getId()).isEqualTo(byDiscount.getId());
        }
    }
}