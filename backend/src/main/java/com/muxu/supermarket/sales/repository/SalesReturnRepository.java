package com.muxu.supermarket.sales.repository;

import com.muxu.supermarket.sales.entity.SalesReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesReturnRepository
        extends JpaRepository<SalesReturn, Long>, JpaSpecificationExecutor<SalesReturn> {
}
