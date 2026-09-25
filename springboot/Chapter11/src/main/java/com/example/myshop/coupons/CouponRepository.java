package com.example.myshop.coupons;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /** Coupon.objects.get(code__iexact=code, valid_from__lte=now, valid_to__gte=now, active=True) */
    @Query("""
            select c from Coupon c
            where lower(c.code) = lower(:code)
              and c.validFrom <= :now and c.validTo >= :now and c.active = true
            """)
    Optional<Coupon> findValid(@Param("code") String code, @Param("now") OffsetDateTime now);

    boolean existsByCodeIgnoreCase(String code);

    List<Coupon> findAllByOrderByValidToDesc();
}
