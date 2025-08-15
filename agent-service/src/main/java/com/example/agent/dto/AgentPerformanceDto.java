package com.example.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;
import java.util.List;

@Schema(description = "代理业绩信息DTO")
public class AgentPerformanceDto {

    @Schema(description = "代理用户ID", example = "10")
    private Long agentId;

    @Schema(description = "总业绩", example = "125000.00")
    private String totalRevenue;

    @Schema(description = "当月业绩", example = "15000.00")
    private String currentMonthRevenue;

    @Schema(description = "客户总数", example = "89")
    private Integer totalClients;

    @Schema(description = "活跃客户数", example = "67")
    private Integer activeClients;

    @Schema(description = "转化率 [0,1]", example = "0.75")
    private Double conversionRate;

    @Schema(description = "趋势数据点")
    private List<TrendDataPoint> trendData;

    @Schema(description = "最后更新时间")
    private Date lastUpdated;

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }

    public String getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(String totalRevenue) { this.totalRevenue = totalRevenue; }

    public String getCurrentMonthRevenue() { return currentMonthRevenue; }
    public void setCurrentMonthRevenue(String currentMonthRevenue) { this.currentMonthRevenue = currentMonthRevenue; }

    public Integer getTotalClients() { return totalClients; }
    public void setTotalClients(Integer totalClients) { this.totalClients = totalClients; }

    public Integer getActiveClients() { return activeClients; }
    public void setActiveClients(Integer activeClients) { this.activeClients = activeClients; }

    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }

    public List<TrendDataPoint> getTrendData() { return trendData; }
    public void setTrendData(List<TrendDataPoint> trendData) { this.trendData = trendData; }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
}

