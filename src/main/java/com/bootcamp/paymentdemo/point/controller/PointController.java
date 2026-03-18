package com.bootcamp.paymentdemo.point.controller;

import com.bootcamp.paymentdemo.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    // 현재 포인트 + 등급 조회

    // 포인트 거래내역 목록 조회

    // 등급 정책 조회
}