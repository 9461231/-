package com.muxu.supermarket.member.repository;

import com.muxu.supermarket.member.entity.MemberPointsRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberPointsRecordRepository extends JpaRepository<MemberPointsRecord, Long> {

    List<MemberPointsRecord> findTop50ByMemberIdOrderByIdDesc(Long memberId);
}
