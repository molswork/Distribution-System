package com.example.agent.controller;

import com.example.common.dto.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Map;

import com.example.agent.service.AgentService;
import com.example.agent.service.AgentPerformanceService;
import com.example.data.entity.Agent;
import com.example.agent.dto.AgentCreateRequest;
import com.example.agent.dto.AgentUpdateRequest;
import com.example.agent.dto.BatchDeleteRequest;

@RestController
@RequestMapping("/api/agents")
@Tag(name = "代理管理", description = "代理管理相关接口")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentPerformanceService performanceService;

    @GetMapping("/ping")
    @Operation(summary = "Agent Service 心跳", description = "用于验证服务是否正常运行")
    public CommonResult<Map<String, Object>> ping() {
        return CommonResult.success(agentService.heartbeat());
    }

    @GetMapping("/all")
    @Operation(summary = "代理列表", description = "分页、筛选与排序")
    public CommonResult<Map<String, Object>> list(
            @Parameter(description = "页码", schema = @Schema(example = "1")) @RequestParam(required = false) Integer page,
            @Parameter(description = "每页大小", schema = @Schema(example = "20")) @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "等级名称") @RequestParam(required = false) String levelName,
            @Parameter(description = "上级代理UserId") @RequestParam(required = false) Long parentAgentId,
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "排序字段") @RequestParam(required = false) String sortBy,
            @Parameter(description = "排序方向 asc|desc") @RequestParam(required = false) String sortOrder
    ) {
        return CommonResult.success(agentService.getAgents(page, pageSize, keyword, status, levelName, parentAgentId, dateFrom, dateTo, sortBy, sortOrder));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "代理详情(按UserId)", description = "返回基础代理信息")
    public CommonResult<Agent> getByUserId(@PathVariable Long userId) {
        return agentService.getAgentByUserId(userId)
                .map(CommonResult::success)
                .orElseGet(CommonResult::notFound);
    }

    @PostMapping("")
    @Operation(summary = "创建代理")
    public CommonResult<Agent> create(@Valid @RequestBody AgentCreateRequest req) {
        try {
            Agent created = agentService.createAgent(req);
            return CommonResult.success("创建成功", created);
        } catch (IllegalArgumentException e) {
            return CommonResult.<Agent>error(com.example.common.constants.ErrorCode.UNPROCESSABLE_ENTITY.getHttpCode(), e.getMessage());
        } catch (Exception e) {
            return CommonResult.<Agent>error(com.example.common.constants.ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), e.getMessage());
        }
    }

    @PutMapping("/{userId}")
    @Operation(summary = "更新代理")
    public CommonResult<Agent> update(@PathVariable Long userId, @Valid @RequestBody AgentUpdateRequest req) {
        try {
            return agentService.updateAgent(userId, req)
                    .map(a -> CommonResult.success("更新成功", a))
                    .orElseGet(CommonResult::notFound);
        } catch (IllegalArgumentException e) {
            return CommonResult.<Agent>error(com.example.common.constants.ErrorCode.UNPROCESSABLE_ENTITY.getHttpCode(), e.getMessage());
        } catch (Exception e) {
            return CommonResult.<Agent>error(com.example.common.constants.ErrorCode.INTERNAL_SERVER_ERROR.getHttpCode(), e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除代理(软删除)")
    public CommonResult<Map<String, Object>> delete(@PathVariable Long userId) {
        try {
            boolean ok = agentService.deleteAgent(userId);
            if (ok) {
                return CommonResult.success(Map.of("deletedId", userId));
            }
            return CommonResult.notFound();
        } catch (IllegalStateException e) {
            return CommonResult.error(com.example.common.constants.ErrorCode.UNPROCESSABLE_ENTITY, e.getMessage());
        } catch (Exception e) {
            return CommonResult.error(com.example.common.constants.ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除代理(软删除)")
    public CommonResult<Map<String, Object>> batchDelete(@Valid @RequestBody BatchDeleteRequest req) {
        try {
            Map<String, Object> stats = agentService.batchDeleteAgents(req.getIds());
            return CommonResult.success(stats);
        } catch (Exception e) {
            return CommonResult.error(com.example.common.constants.ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{userId}/performance")
    @Operation(summary = "代理业绩(最小占位)", description = "支持 includeTrend 与 granularity=day|week|month")
    public CommonResult<com.example.agent.dto.AgentPerformanceDto> performance(
            @PathVariable Long userId,
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "是否包含趋势") @RequestParam(required = false) Boolean includeTrend,
            @Parameter(description = "粒度 day|week|month") @RequestParam(required = false) String granularity
    ) {
        return CommonResult.success(performanceService.getPerformance(userId, startDate, endDate, includeTrend, granularity));
    }
}


