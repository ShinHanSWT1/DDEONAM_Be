package com.gorani.ecodrive.auth.oauth;

import com.gorani.ecodrive.user.domain.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthAttributesTest {

    @Test
    @DisplayName("카카오 OAuth 속성 정상 파싱 - 이메일, 닉네임, 프로필 이미지 모두 있는 경우")
    void ofKakao_withAllAttributes_parsesCorrectly() {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");

        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "테스트유저");
        properties.put("profile_image", "https://example.com/profile.jpg");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123456L);
        attributes.put("kakao_account", kakaoAccount);
        attributes.put("properties", properties);

        // when
        OAuthAttributes result = OAuthAttributes.ofKakao(attributes);

        // then
        assertThat(result.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(result.providerId()).isEqualTo("123456");
        assertThat(result.email()).isEqualTo("test@kakao.com");
        assertThat(result.nickname()).isEqualTo("테스트유저");
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");
    }

    @Test
    @DisplayName("카카오 OAuth 속성 - kakao_account가 null이면 email은 null")
    void ofKakao_withNullKakaoAccount_emailIsNull() {
        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "테스트유저");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 789L);
        attributes.put("kakao_account", null);
        attributes.put("properties", properties);

        // when
        OAuthAttributes result = OAuthAttributes.ofKakao(attributes);

        // then
        assertThat(result.email()).isNull();
        assertThat(result.nickname()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("카카오 OAuth 속성 - properties가 null이면 닉네임은 기본값, 프로필은 null")
    void ofKakao_withNullProperties_usesDefaultNickname() {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 999L);
        attributes.put("kakao_account", kakaoAccount);
        attributes.put("properties", null);

        // when
        OAuthAttributes result = OAuthAttributes.ofKakao(attributes);

        // then
        assertThat(result.nickname()).isEqualTo("카카오사용자");
        assertThat(result.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("카카오 OAuth 속성 - kakao_account에 email 없으면 email은 null")
    void ofKakao_withMissingEmailInKakaoAccount_emailIsNull() {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();
        // no email key

        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "닉네임");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 111L);
        attributes.put("kakao_account", kakaoAccount);
        attributes.put("properties", properties);

        // when
        OAuthAttributes result = OAuthAttributes.ofKakao(attributes);

        // then
        assertThat(result.email()).isNull();
    }

    @Test
    @DisplayName("카카오 OAuth 속성 - id가 null이어도 'null' 문자열로 처리됨")
    void ofKakao_withNullId_providerIdIsNullString() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", null);
        attributes.put("kakao_account", null);
        attributes.put("properties", null);

        // when
        OAuthAttributes result = OAuthAttributes.ofKakao(attributes);

        // then
        assertThat(result.providerId()).isEqualTo("null");
    }

    @Test
    @DisplayName("카카오 OAuth 속성 - 정수형 id도 문자열로 변환됨")
    void ofKakao_withIntegerId_convertsToString() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 42);
        attributes.put("kakao_account", null);
        attributes.put("properties", null);

        // when
        OAuthAttributes result = OAuthAttributes.ofKakao(attributes);

        // then
        assertThat(result.providerId()).isEqualTo("42");
        assertThat(result.provider()).isEqualTo(OAuthProvider.KAKAO);
    }

    @Test
    @DisplayName("카카오 OAuth 속성 - properties에 profile_image 없으면 profileImageUrl은 null")
    void ofKakao_withMissingProfileImage_profileImageUrlIsNull() {
        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "닉네임만있음");
        // no profile_image

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 555L);
        attributes.put("kakao_account", null);
        attributes.put("properties", properties);

        // when
        OAuthAttributes result = OAuthAttributes.ofKakao(attributes);

        // then
        assertThat(result.profileImageUrl()).isNull();
        assertThat(result.nickname()).isEqualTo("닉네임만있음");
    }
}