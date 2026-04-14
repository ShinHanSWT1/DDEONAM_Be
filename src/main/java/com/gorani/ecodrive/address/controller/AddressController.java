package com.gorani.ecodrive.address.controller;

import com.gorani.ecodrive.address.dto.AddressSearchResponse;
import com.gorani.ecodrive.address.service.AddressService;
import com.gorani.ecodrive.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/address")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/search")
    public ApiResponse<List<AddressSearchResponse>> search(
            @RequestParam String keyword
    ) throws Exception {
        return ApiResponse.success(
                "주소 검색 성공",
                addressService.search(keyword)
        );
    }
}