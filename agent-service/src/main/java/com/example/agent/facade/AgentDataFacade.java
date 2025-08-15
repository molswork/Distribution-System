package com.example.agent.facade;

import com.example.agent.dto.AgentCreateRequest;
import com.example.agent.dto.AgentUpdateRequest;
import com.example.common.constants.ErrorCode;
import com.example.common.dto.CommonResult;
import com.example.common.utils.SecurityUtils;
import com.example.data.entity.Agent;
import com.example.data.entity.User;
import com.example.data.mapper.AgentLevelMapper;
import com.example.data.mapper.AgentMapper;
import com.example.data.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class AgentDataFacade {

    private final AgentMapper agentMapper;
    private final UserMapper userMapper;
    private final AgentLevelMapper agentLevelMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${agent.cache.list-ttl-seconds:60}")
    private long listTtlSeconds;

    @Value("${agent.cache.list-version-key:agent:list:ver}")
    private String listVersionKey;

    @Autowired
    public AgentDataFacade(AgentMapper agentMapper,
                           UserMapper userMapper,
                           AgentLevelMapper agentLevelMapper,
                           RedisTemplate<String, Object> redisTemplate) {
        this.agentMapper = agentMapper;
        this.userMapper = userMapper;
        this.agentLevelMapper = agentLevelMapper;
        this.redisTemplate = redisTemplate;
    }

    public Optional<Agent> findBasicByUserId(Long userId) {
        return agentMapper.findBasicByUserId(userId);
    }

    public Map<String, Object> findPageWithCount(Integer page,
                                                 Integer pageSize,
                                                 String keyword,
                                                 String status,
                                                 String levelName,
                                                 Long parentAgentId,
                                                 LocalDate dateFrom,
                                                 LocalDate dateTo,
                                                 String sortBy,
                                                 String sortOrder) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (pageSize == null || pageSize < 1 || pageSize > 100) ? 20 : pageSize;
        Integer offset = (p - 1) * s;

        LocalDateTime start = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime end = dateTo == null ? null : dateTo.atTime(23, 59, 59);

        // 读取列表缓存
        String ver = String.valueOf(getListVersion());
        String rawKey = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                p, s, nullToEmpty(keyword), nullToEmpty(status), nullToEmpty(levelName),
                parentAgentId == null ? "" : parentAgentId,
                start == null ? "" : start,
                end == null ? "" : end,
                nullToEmpty(sortBy), nullToEmpty(sortOrder));
        String hash = DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
        String cacheKey = "agent:list:v" + ver + ":" + hash;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Map) {
            //noinspection unchecked
            return (Map<String, Object>) cached;
        }

        List<Agent> data = agentMapper.findByConditions(
                offset, s, keyword, status, levelName, parentAgentId, start, end, sortBy, sortOrder
        );
        long total = agentMapper.countByConditions(
                keyword, status, levelName, parentAgentId, start, end
        );

        Map<String, Object> result = new HashMap<>();
        result.put("list", data);
        result.put("total", total);
        result.put("page", p);
        result.put("pageSize", s);
        long totalPages = (total + s - 1) / s;
        result.put("totalPages", totalPages);

        redisTemplate.opsForValue().set(cacheKey, result, java.time.Duration.ofSeconds(listTtlSeconds));
        return result;
    }

    // ========== 写操作 ==========
    public Optional<Agent> createAgent(AgentCreateRequest req) {
        // 唯一性校验
        if (userMapper.existsByEmail(req.getEmail(), null)) {
            throw new IllegalArgumentException(ErrorCode.AGENT_002.getMessage());
        }
        if (userMapper.existsByPhone(req.getPhone(), null)) {
            throw new IllegalArgumentException(ErrorCode.AGENT_003.getMessage());
        }
        // 等级校验
        var levelOpt = agentLevelMapper.findByLevelName(req.getLevel());
        if (levelOpt.isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.AGENT_004.getMessage());
        }
        // 上级校验（可选）
        if (req.getParentAgentId() != null && userMapper.findById(req.getParentAgentId()).isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.AGENT_005.getMessage());
        }

        // 构造用户
        User user = new User();
        user.setUsername(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        String rawPwd = StringUtils.hasText(req.getPassword()) ? req.getPassword() : ("Init@" + UUID.randomUUID().toString().substring(0, 8));
        user.setPassword(SecurityUtils.encodePassword(rawPwd));
        user.setRole("agent");
        user.setStatus("active");
        user.setParentId(req.getParentAgentId());
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        int n = userMapper.insert(user);
        if (n <= 0 || user.getId() == null) {
            throw new RuntimeException("创建用户失败");
        }
        // 绑定等级
        agentMapper.upsertUserAgentLevel(user.getId(), levelOpt.get().getId().longValue());

        bumpListVersion();
        return findBasicByUserId(user.getId());
    }

    public Optional<Agent> updateAgent(Long userId, AgentUpdateRequest req) {
        var userOpt = userMapper.findById(userId);
        if (userOpt.isEmpty()) return Optional.empty();
        User user = userOpt.get();

        if (StringUtils.hasText(req.getEmail()) && userMapper.existsByEmail(req.getEmail(), userId)) {
            throw new IllegalArgumentException(ErrorCode.AGENT_002.getMessage());
        }
        if (StringUtils.hasText(req.getPhone()) && userMapper.existsByPhone(req.getPhone(), userId)) {
            throw new IllegalArgumentException(ErrorCode.AGENT_003.getMessage());
        }
        if (req.getParentAgentId() != null && userMapper.findById(req.getParentAgentId()).isEmpty()) {
            throw new IllegalArgumentException(ErrorCode.AGENT_005.getMessage());
        }
        if (StringUtils.hasText(req.getLevel())) {
            var levelOpt = agentLevelMapper.findByLevelName(req.getLevel());
            if (levelOpt.isEmpty()) {
                throw new IllegalArgumentException(ErrorCode.AGENT_004.getMessage());
            }
            agentMapper.upsertUserAgentLevel(userId, levelOpt.get().getId().longValue());
        }
        if (StringUtils.hasText(req.getName())) user.setUsername(req.getName());
        if (StringUtils.hasText(req.getEmail())) user.setEmail(req.getEmail());
        if (StringUtils.hasText(req.getPhone())) user.setPhone(req.getPhone());
        if (StringUtils.hasText(req.getStatus())) user.setStatus(req.getStatus());
        if (req.getParentAgentId() != null) user.setParentId(req.getParentAgentId());
        if (req.getCommissionRate() != null) user.setCommissionRate(req.getCommissionRate());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.update(user);

        bumpListVersion();
        return findBasicByUserId(userId);
    }

    public boolean deleteAgent(Long userId) {
        // 检查下级
        var children = userMapper.findByParentId(userId);
        if (children != null && !children.isEmpty()) {
            throw new IllegalStateException(ErrorCode.AGENT_006.getMessage());
        }
        int n = userMapper.updateStatus(userId, "deleted", java.time.LocalDateTime.now());
        if (n > 0) {
            bumpListVersion();
            return true;
        }
        return false;
    }

    public Map<String, Object> batchDeleteAgents(List<Long> userIds) {
        int success = 0;
        List<Long> failed = new ArrayList<>();
        for (Long id : userIds) {
            try {
                boolean ok = deleteAgent(id);
                if (ok) success++; else failed.add(id);
            } catch (Exception e) {
                failed.add(id);
            }
        }
        if (success > 0) bumpListVersion();
        Map<String, Object> r = new HashMap<>();
        r.put("successCount", success);
        r.put("failedCount", failed.size());
        r.put("failedIds", failed);
        r.put("affectedSubAgents", 0);
        return r;
    }

    public void bumpListVersion() {
        try {
            redisTemplate.opsForValue().increment(listVersionKey);
        } catch (Exception e) {
            // 降级：直接设置为1，避免缓存永不刷新
            redisTemplate.opsForValue().setIfAbsent(listVersionKey, 1L);
        }
    }

    private long getListVersion() {
        Object v = redisTemplate.opsForValue().get(listVersionKey);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        // 初始化为1
        redisTemplate.opsForValue().set(listVersionKey, 1L);
        return 1L;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

