package com.muxu.supermarket.member.repository;

import com.muxu.supermarket.member.entity.MemberLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberLevelRepository extends JpaRepository<MemberLevel, Long> {

    List<MemberLevel> findAllByOrderBySortOrderAsc();

    Optional<MemberLevel> findFirstByOrderByPointsThresholdAsc();
}
