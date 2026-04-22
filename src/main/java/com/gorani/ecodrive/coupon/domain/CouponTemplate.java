package com.gorani.ecodrive.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_templates")
@Getter
@NoArgsConstructor
public class CouponTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "discount_label", nullable = false, length = 50)
    private String discountLabel;

    @Column(name = "valid_days", nullable = false)
    private Integer validDays;

    @Column(name = "is_pay_usable", nullable = false)
    private Boolean isPayUsable;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
