package com.muxu.supermarket.member.controller;

import com.muxu.supermarket.common.ApiResponse;
import com.muxu.supermarket.member.entity.MemberLevel;
import com.muxu.supermarket.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/member-levels")
@RequiredArgsConstructor
public class MemberLevelController {

    private final MemberService memberService;

    @GetMapping
    public ApiResponse<List<MemberLevel>> list() {
        return ApiResponse.ok(memberService.levels());
    }
}
