package com.ondam.family.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가족", description = "가족 생성·참여 API")
@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    @Operation(
            summary = "가족 생성",
            description = "가족을 만들고 초대코드를 발급합니다."
    )
    @PostMapping
    public String create() {
        return "가족 생성 API";
    }
}