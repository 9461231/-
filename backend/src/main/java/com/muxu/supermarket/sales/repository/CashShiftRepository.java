package com.muxu.supermarket.sales.repository;

import com.muxu.supermarket.sales.entity.CashShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashShiftRepository extends JpaRepository<CashShift, Long> {

    Optional<CashShift> findFirstByStatusOrderByStartTimeDesc(Integer status);

    List<CashShift> findTop50ByStatusOrderByStartTimeDesc(Integer status);

    List<CashShift> findTop50ByOrderByIdDesc();
}
