package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.domain.InsuranceCompany;
import com.gorani.ecodrive.insurance.domain.InsuranceCompanyStatus;
import com.gorani.ecodrive.insurance.repository.InsuranceCompanyRepository;
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
class InsuranceCompanyServiceTest {

    @Mock
    private InsuranceCompanyRepository insuranceCompanyRepository;

    @InjectMocks
    private InsuranceCompanyService insuranceCompanyService;

    private InsuranceCompany mockCompany(Long id, String name, String code, InsuranceCompanyStatus status) {
        InsuranceCompany company = mock(InsuranceCompany.class);
        when(company.getId()).thenReturn(id);
        when(company.getCompanyName()).thenReturn(name);
        when(company.getCode()).thenReturn(code);
        when(company.getStatus()).thenReturn(status);
        return company;
    }

    @Nested
    @DisplayName("getCompanies - 보험사 목록 조회")
    class GetCompaniesTest {

        @Test
        @DisplayName("status가 null이면 전체 목록 반환")
        void getCompanies_nullStatus_returnsAll() {
            InsuranceCompany company1 = mockCompany(1L, "삼성화재", "SAMSUNG", InsuranceCompanyStatus.ACTIVE);
            InsuranceCompany company2 = mockCompany(2L, "KB손해보험", "KB", InsuranceCompanyStatus.INACTIVE);
            when(insuranceCompanyRepository.findAll()).thenReturn(List.of(company1, company2));

            List<InsuranceCompany> result = insuranceCompanyService.getCompanies(null);

            assertThat(result).hasSize(2);
            verify(insuranceCompanyRepository).findAll();
            verify(insuranceCompanyRepository, never()).findAllByStatus(any());
        }

        @Test
        @DisplayName("status가 ACTIVE이면 ACTIVE 보험사만 반환")
        void getCompanies_activeStatus_returnsActiveOnly() {
            InsuranceCompany activeCompany = mockCompany(1L, "삼성화재", "SAMSUNG", InsuranceCompanyStatus.ACTIVE);
            when(insuranceCompanyRepository.findAllByStatus(InsuranceCompanyStatus.ACTIVE))
                    .thenReturn(List.of(activeCompany));

            List<InsuranceCompany> result = insuranceCompanyService.getCompanies("ACTIVE");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(InsuranceCompanyStatus.ACTIVE);
            verify(insuranceCompanyRepository).findAllByStatus(InsuranceCompanyStatus.ACTIVE);
            verify(insuranceCompanyRepository, never()).findAll();
        }

        @Test
        @DisplayName("status가 INACTIVE이면 INACTIVE 보험사만 반환")
        void getCompanies_inactiveStatus_returnsInactiveOnly() {
            InsuranceCompany inactiveCompany = mockCompany(2L, "구보험사", "OLD", InsuranceCompanyStatus.INACTIVE);
            when(insuranceCompanyRepository.findAllByStatus(InsuranceCompanyStatus.INACTIVE))
                    .thenReturn(List.of(inactiveCompany));

            List<InsuranceCompany> result = insuranceCompanyService.getCompanies("INACTIVE");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(InsuranceCompanyStatus.INACTIVE);
        }

        @Test
        @DisplayName("유효하지 않은 status 문자열이면 IllegalArgumentException 발생")
        void getCompanies_invalidStatus_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> insuranceCompanyService.getCompanies("INVALID_STATUS"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("결과가 빈 목록이어도 정상 반환")
        void getCompanies_emptyResult_returnsEmptyList() {
            when(insuranceCompanyRepository.findAll()).thenReturn(List.of());

            List<InsuranceCompany> result = insuranceCompanyService.getCompanies(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCompanyById - ID로 보험사 조회")
    class GetCompanyByIdTest {

        @Test
        @DisplayName("존재하는 ID이면 보험사 반환")
        void getCompanyById_existingId_returnsCompany() {
            InsuranceCompany company = mockCompany(1L, "삼성화재", "SAMSUNG", InsuranceCompanyStatus.ACTIVE);
            when(insuranceCompanyRepository.findById(1L)).thenReturn(Optional.of(company));

            InsuranceCompany result = insuranceCompanyService.getCompanyById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCompanyName()).isEqualTo("삼성화재");
        }

        @Test
        @DisplayName("존재하지 않는 ID이면 CustomException(INSURANCE_COMPANY_NOT_FOUND) 발생")
        void getCompanyById_nonExistingId_throwsCustomException() {
            when(insuranceCompanyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> insuranceCompanyService.getCompanyById(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ce = (CustomException) e;
                        assertThat(ce.getErrorCode()).isEqualTo(ErrorCode.INSURANCE_COMPANY_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("INSURANCE_COMPANY_NOT_FOUND 에러코드 속성 검증")
        void getCompanyById_notFound_correctErrorCodeAttributes() {
            when(insuranceCompanyRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> insuranceCompanyService.getCompanyById(1L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> {
                        CustomException ce = (CustomException) e;
                        assertThat(ce.getErrorCode().getCode()).isEqualTo("INSURANCE_001");
                        assertThat(ce.getErrorCode().getMessage()).isEqualTo("해당 보험사를 찾을 수 없습니다.");
                    });
        }
    }
}