package com.gorani.ecodrive.vehicle.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.vehicle.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VehicleModelController {

    private final VehicleModelService vehicleModelService;

    @GetMapping("/api/vehicles/models")
    public ApiResponse<List<VehicleModelService.VehicleModelSummary>> searchVehicleModels(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(
                "차량 모델 목록 조회 성공",
                vehicleModelService.searchModels(keyword)
        );
    }
}
