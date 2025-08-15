package com.example.agent.service.impl;

import com.example.agent.dto.AgentCreateRequest;
import com.example.agent.dto.AgentUpdateRequest;
import com.example.agent.facade.AgentDataFacade;
import com.example.agent.service.AgentService;
import com.example.data.entity.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AgentServiceImpl implements AgentService {

    private final AgentDataFacade facade;

    @Autowired
    public AgentServiceImpl(AgentDataFacade facade) {
        this.facade = facade;
    }

    @Override
    public Map<String, Object> heartbeat() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "agent-service");
        data.put("status", "OK");
        return data;
    }

    @Override
    public Map<String, Object> getAgents(Integer page,
                                         Integer pageSize,
                                         String keyword,
                                         String status,
                                         String levelName,
                                         Long parentAgentId,
                                         LocalDate dateFrom,
                                         LocalDate dateTo,
                                         String sortBy,
                                         String sortOrder) {
        return facade.findPageWithCount(page, pageSize, keyword, status, levelName, parentAgentId, dateFrom, dateTo, sortBy, sortOrder);
    }

    @Override
    public Optional<Agent> getAgentByUserId(Long userId) {
        return facade.findBasicByUserId(userId);
    }

    @Override
    public Agent createAgent(AgentCreateRequest request) {
        return facade.createAgent(request).orElse(null);
    }

    @Override
    public Optional<Agent> updateAgent(Long userId, AgentUpdateRequest request) {
        return facade.updateAgent(userId, request);
    }

    @Override
    public boolean deleteAgent(Long userId) {
        return facade.deleteAgent(userId);
    }

    @Override
    public Map<String, Object> batchDeleteAgents(List<Long> userIds) {
        return facade.batchDeleteAgents(userIds);
    }
}

