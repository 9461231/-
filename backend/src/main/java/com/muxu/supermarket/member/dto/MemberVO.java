package com.muxu.supermarket.member.dto;

import com.muxu.supermarket.member.entity.Member;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MemberVO {

    private Long id;
    private String memberNo;
    private String name;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
    private Long levelId;
    private String levelName;
    private Integer points;
    private BigDecimal totalSpent;
    private Integer status;
    private LocalDateTime createdAt;

    public static MemberVO from(Member m, String levelName) {
        MemberVO vo = new MemberVO();
        vo.setId(m.getId());
        vo.setMemberNo(m.getMemberNo());
        vo.setName(m.getName());
        vo.setPhone(m.getPhone());
        vo.setGender(m.getGender());
        vo.setBirthday(m.getBirthday());
        vo.setLevelId(m.getLevelId());
        vo.setLevelName(levelName);
        vo.setPoints(m.getPoints());
        vo.setTotalSpent(m.getTotalSpent());
        vo.setStatus(m.getStatus());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }
}
