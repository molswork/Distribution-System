package com.example.agent.service;

import com.example.data.entity.Agent;
import com.example.agent.dto.AgentCreateRequest;
import com.example.agent.dto.AgentUpdateRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AgentService {
    Map<String, Object> heartbeat();

    Map<String, Object> getAgents(Integer page,
                                  Integer pageSize,
                                  String keyword,
                                  String status,
                                  String levelName,
                                  Long parentAgentId,
                                  LocalDate dateFrom,
                                  LocalDate dateTo,
                                  String sortBy,
                                  String sortOrder);

    Optional<Agent> getAgentByUserId(Long userId);

    Agent createAgent(AgentCreateRequest request);

    Optional<Agent> updateAgent(Long userId, AgentUpdateRequest request);

    boolean deleteAgent(Long userId);

    Map<String, Object> batchDeleteAgents(List<Long> userIds);
}

