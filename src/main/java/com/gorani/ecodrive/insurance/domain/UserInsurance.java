package com.gorani.ecodrive.insurance.domain;

import com.gorani.ecodrive.vehicle.domain.UserVehicle;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_insurances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInsurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_vehicle_id", nullable = false)
    private UserVehicle userVehicle;
}
