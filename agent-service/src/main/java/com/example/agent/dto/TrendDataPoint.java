package com.example.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "业绩趋势数据点")
public class TrendDataPoint {
    @Schema(description = "日期（按粒度 day|week|month 格式化）", example = "2025-08" )
    private String date;

    @Schema(description = "业绩金额", example = "10000.00")
    private String revenue;

    @Schema(description = "客户数", example = "15")
    private Integer clients;

    public TrendDataPoint() {}

    public TrendDataPoint(String date, String revenue, Integer clients) {
        this.date = date;
        this.revenue = revenue;
        this.clients = clients;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getRevenue() { return revenue; }
    public void setRevenue(String revenue) { this.revenue = revenue; }

    public Integer getClients() { return clients; }
    public void setClients(Integer clients) { this.clients = clients; }
}

