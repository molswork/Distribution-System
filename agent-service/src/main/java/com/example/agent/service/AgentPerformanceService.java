package com.example.agent.service;

import com.example.agent.dto.AgentPerformanceDto;

import java.time.LocalDate;

public interface AgentPerformanceService {
    AgentPerformanceDto getPerformance(Long userId,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       Boolean includeTrend,
                                       String granularity);
}

