package com.example.agent.service.impl;

import com.example.agent.dto.AgentPerformanceDto;
import com.example.agent.dto.TrendDataPoint;
import com.example.agent.service.AgentPerformanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
public class AgentPerformanceServiceImpl implements AgentPerformanceService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${agent.cache.performance-ttl-seconds:300}")
    private long perfTtlSeconds;

    public AgentPerformanceServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public AgentPerformanceDto getPerformance(Long userId,
                                              LocalDate startDate,
                                              LocalDate endDate,
                                              Boolean includeTrend,
                                              String granularity) {
        String key = buildPerfKey(userId, startDate, endDate, includeTrend, granularity);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof AgentPerformanceDto) {
            return (AgentPerformanceDto) cached;
        }

        AgentPerformanceDto dto = new AgentPerformanceDto();
        dto.setAgentId(userId);
        dto.setTotalRevenue("0.00");
        dto.setCurrentMonthRevenue("0.00");
        dto.setTotalClients(0);
        dto.setActiveClients(0);
        dto.setConversionRate(0.0);
        dto.setLastUpdated(new Date());

        if (Boolean.TRUE.equals(includeTrend)) {
            List<TrendDataPoint> trendData = new ArrayList<>();
            LocalDate from = startDate != null ? startDate : LocalDate.now().minusMonths(1).withDayOfMonth(1);
            String gran = (granularity == null || granularity.isEmpty()) ? "month" : granularity.toLowerCase(Locale.ROOT);

            DecimalFormat df = new DecimalFormat("0.00");
            for (int i = 0; i < 3; i++) {
                String dateLabel = gran.equals("day") ? from.plusDays(i).toString() : (gran.equals("week") ? "W" + (i + 1) : from.plusMonths(i).toString().substring(0,7));
                trendData.add(new TrendDataPoint(dateLabel, df.format(0.0), 0));
            }
            dto.setTrendData(trendData);
        }

        redisTemplate.opsForValue().set(key, dto, Duration.ofSeconds(perfTtlSeconds));
        return dto;
    }

    private String buildPerfKey(Long userId,
                                LocalDate startDate,
                                LocalDate endDate,
                                Boolean includeTrend,
                                String granularity) {
        String raw = String.format("%s|%s|%s|%s|%s",
                userId,
                startDate == null ? "" : startDate,
                endDate == null ? "" : endDate,
                includeTrend == null ? "" : includeTrend,
                granularity == null ? "" : granularity.toLowerCase(Locale.ROOT));
        String hash = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        return "agent:perf:" + userId + ":" + hash;
    }
}

