package com.example.agent.controller;

import com.example.agent.dto.AgentCreateRequest;
import com.example.agent.dto.AgentUpdateRequest;
import com.example.agent.service.AgentPerformanceService;
import com.example.agent.service.AgentService;
import com.example.common.dto.CommonResult;
import com.example.data.entity.Agent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentService agentService;

    @MockBean
    private AgentPerformanceService performanceService;

    @Test
    @DisplayName("GET /api/agents/all 返回列表成功")
    void testListAgents() throws Exception {
        Map<String, Object> page = new HashMap<>();
        page.put("list", java.util.List.of());
        page.put("total", 0);
        page.put("page", 1);
        page.put("pageSize", 20);
        Mockito.when(agentService.getAgents(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/agents/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.total", is(0)));
    }

    @Test
    @DisplayName("GET /api/agents/{userId} 未找到")
    void testGetAgentNotFound() throws Exception {
        Mockito.when(agentService.getAgentByUserId(100L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/agents/100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)));
    }

    @Test
    @DisplayName("POST /api/agents 创建成功")
    void testCreateAgent() throws Exception {
        Agent a = new Agent();
        a.setUserId(1L); a.setUsername("张三"); a.setEmail("zhangsan@example.com"); a.setPhone("13800138000");
        Mockito.when(agentService.createAgent(Mockito.any(AgentCreateRequest.class))).thenReturn(a);

        String body = "{\n" +
                "  \"name\": \"张三\",\n" +
                "  \"email\": \"zhangsan@example.com\",\n" +
                "  \"phone\": \"13800138000\",\n" +
                "  \"level\": \"standard\"\n" +
                "}";

        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.userId", is(1)));
    }

    @Test
    @DisplayName("PUT /api/agents/{userId} 更新成功/未找到")
    void testUpdateAgent() throws Exception {
        Agent a = new Agent(); a.setUserId(2L); a.setUsername("李四");
        Mockito.when(agentService.updateAgent(Mockito.eq(2L), Mockito.any(AgentUpdateRequest.class)))
                .thenReturn(Optional.of(a));
        String body = "{\"name\":\"李四\"}";
        mockMvc.perform(put("/api/agents/2").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(2)));

        Mockito.when(agentService.updateAgent(Mockito.eq(3L), Mockito.any(AgentUpdateRequest.class)))
                .thenReturn(Optional.empty());
        mockMvc.perform(put("/api/agents/3").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/agents/{userId} 删除成功/未找到")
    void testDeleteAgent() throws Exception {
        Mockito.when(agentService.deleteAgent(5L)).thenReturn(true);
        mockMvc.perform(delete("/api/agents/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedId", is(5)));

        Mockito.when(agentService.deleteAgent(6L)).thenReturn(false);
        mockMvc.perform(delete("/api/agents/6"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/agents/batch-delete 批量删除成功")
    void testBatchDelete() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("successCount", 2);
        stats.put("failedCount", 1);
        stats.put("failedIds", java.util.List.of(7));
        Mockito.when(agentService.batchDeleteAgents(Mockito.anyList())).thenReturn(stats);

        String body = "{\"ids\":[1,2,7]}";
        mockMvc.perform(post("/api/agents/batch-delete").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount", is(2)))
                .andExpect(jsonPath("$.data.failedCount", is(1)));
    }
}

