package com.muxu.supermarket.stocktake.repository;

import com.muxu.supermarket.stocktake.entity.LossRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LossRecordRepository
        extends JpaRepository<LossRecord, Long>, JpaSpecificationExecutor<LossRecord> {
}
