package com.gorani.ecodrive.address.dto;

public record AddressSearchResponse (
    String roadAddr,    // 전체 도로명주소
    String jibunAddr,   // 지번 주소
    String zipNo,       // 우편번호
    String bdNm         // 건물명
){
}