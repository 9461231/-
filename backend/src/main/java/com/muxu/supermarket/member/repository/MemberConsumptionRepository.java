package com.muxu.supermarket.member.repository;

import com.muxu.supermarket.member.entity.MemberConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberConsumptionRepository extends JpaRepository<MemberConsumption, Long> {

    List<MemberConsumption> findTop50ByMemberIdOrderByIdDesc(Long memberId);
}
