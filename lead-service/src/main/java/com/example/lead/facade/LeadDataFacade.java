package com.example.lead.facade;

import com.example.data.entity.CustomerLead;
import com.example.data.mapper.CustomerLeadMapper;
import com.example.lead.converter.LeadDtoConverter;
import com.example.lead.dto.CreateLeadRequest;
import com.example.lead.dto.CustomerLeadDto;
import com.example.lead.dto.LeadDetailsDto;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.TimeUnit;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;
import com.example.lead.dto.UpdateLeadRequest;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class LeadDataFacade {

    private final CustomerLeadMapper leadMapper;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redis;

    public LeadDataFacade(CustomerLeadMapper leadMapper) {
        this.leadMapper = leadMapper;
    }

    public Optional<LeadDetailsDto> findDetailsById(Long id) {
        return leadMapper.findById(id).map(LeadDtoConverter::toDetails);
    }

    public boolean existsByPhone(String phone, Long excludeId) {
        // 先查缓存
        if (redis != null) {
            String key = "lead:exists:phone:" + phone;
            Object v = redis.opsForValue().get(key);
            if (v != null) {
                boolean exists = "1".equals(v.toString());
                if (!exists) return false; // 缓存明确为不存在时直接返回
                // 存在时如需排除ID，仍需DB核对
                if (excludeId == null) return true;
            }
        }
        Optional<CustomerLead> opt = leadMapper.findByPhone(phone);
        boolean present = opt.filter(e -> excludeId == null || !excludeId.equals(e.getId())).isPresent();
        if (redis != null) {
            String key = "lead:exists:phone:" + phone;
            redis.opsForValue().set(key, present ? "1" : "0", 5, TimeUnit.MINUTES);
        }
        return present;
    }

    public CustomerLeadDto create(CreateLeadRequest req) {
        CustomerLead e = LeadDtoConverter.toEntity(req);
        // 不再设置phoneNormalized字段，因为数据库表中没有这个字段
        leadMapper.insert(e);
        return LeadDtoConverter.toDto(e);
    }

    public boolean updateStatus(Long id, String statusCode) {
        return leadMapper.updateFollowUp(id, statusCode, LocalDateTime.now(), LocalDateTime.now()) > 0;
    }

    public boolean batchUpdateAuditStatus(List<Long> ids, String auditStatusCode) {
        return leadMapper.batchUpdateAuditStatus(ids, auditStatusCode, LocalDateTime.now()) > 0;
    }
    public boolean updateAuditStatus(Long id, String auditStatusCode) {
        return leadMapper.updateAuditStatus(id, auditStatusCode, java.time.LocalDateTime.now()) > 0;
    }


    // 向后兼容的重载：不含 keyword/source 参数
    public List<CustomerLeadDto> findPage(Integer page, Integer size, Long salespersonId, String status, String auditStatus) {
        return findPage(page, size, salespersonId, status, auditStatus, null, null, null, null, null, null);
    }
    public boolean updateLead(Long id, UpdateLeadRequest req) {
        return leadMapper.findById(id).map(e -> {
            if (StringUtils.hasText(req.getName())) e.setName(req.getName());
            if (StringUtils.hasText(req.getPhone())) e.setPhone(req.getPhone());
            if (StringUtils.hasText(req.getWechatId())) e.setWechatId(req.getWechatId());
            if (StringUtils.hasText(req.getNotes())) e.setNotes(req.getNotes());
            if (StringUtils.hasText(req.getStatus())) e.setStatus(CustomerLead.LeadStatus.fromCode(req.getStatus()));
            if (StringUtils.hasText(req.getSource())) e.setSource(req.getSource());
            if (StringUtils.hasText(req.getSourceDetail())) e.setSourceDetail(req.getSourceDetail());
            if (req.getSalespersonId() != null) e.setSalespersonId(req.getSalespersonId());
            // email 字段暂未入库，保留请求但不持久化
            e.setUpdatedAt(LocalDateTime.now());
            return leadMapper.update(e) > 0;
        }).orElse(false);
    }

    public boolean deleteLead(Long id) {
        // 保护性删除：存在关联交易则不允许删除（后续可改为软删除）
        try {
            // 如果有 DealMapper 可用于检测关联，这里可以注入并校验；当前仅执行物理删除
            return leadMapper.deleteById(id) > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean batchDeleteLeads(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        int deletedCount = 0;
        for (Long id : ids) {
            if (leadMapper.deleteById(id) > 0) {
                deletedCount++;
            }
        }

        // 如果至少删除了一个，就认为成功
        return deletedCount > 0;
    }

    public com.example.lead.dto.PageResult<CustomerLeadDto> findPageWithCount(Integer page, Integer size,
            Long salespersonId, String status, String auditStatus, String keyword, String source, String startDate, String endDate, String sortBy, String sortOrder) {
        // 轻量分页缓存（短TTL + 版本号）
        String ver = "1";
        String verKey = "lead:list:ver";
        List<CustomerLeadDto> data;
        long total;
        boolean fromCache = false;
        boolean isPending = "PENDING_AUDIT".equalsIgnoreCase(auditStatus);
        if (!isPending && redis != null) {
            Object v = redis.opsForValue().get(verKey);
            if (v != null) ver = v.toString();
            String raw = String.format("p=%s,s=%s,sp=%s,st=%s,as=%s,kw=%s,src=%s,sd=%s,ed=%s,sb=%s,so=%s",
                    page,size,salespersonId,status,auditStatus,keyword,source,startDate,endDate,sortBy,sortOrder);
            String k = "lead:list:v" + ver + ":" + org.springframework.util.DigestUtils.md5DigestAsHex(raw.getBytes());
            Object cached = redis.opsForValue().get(k);
            if (cached instanceof com.example.lead.dto.PageResult) {
                @SuppressWarnings("unchecked")
                com.example.lead.dto.PageResult<CustomerLeadDto> pr = (com.example.lead.dto.PageResult<CustomerLeadDto>) cached;
                return pr;
            }
        }
        // miss → DB
        data = findPage(page, size, salespersonId, status, auditStatus, keyword, source, startDate, endDate, sortBy, sortOrder);
        total = countByConditions(salespersonId, status, auditStatus, keyword, source, startDate, endDate);
        com.example.lead.dto.PageResult<CustomerLeadDto> pr = new com.example.lead.dto.PageResult<>(data, total, page == null ? 1 : page, size == null ? 10 : size);
        if (!isPending && redis != null) {
            String raw = String.format("p=%s,s=%s,sp=%s,st=%s,as=%s,kw=%s,src=%s,sd=%s,ed=%s,sb=%s,so=%s",
                    page,size,salespersonId,status,auditStatus,keyword,source,startDate,endDate,sortBy,sortOrder);
            String k = "lead:list:v" + ver + ":" + org.springframework.util.DigestUtils.md5DigestAsHex(raw.getBytes());
            redis.opsForValue().set(k, pr, 60, TimeUnit.SECONDS);
        }
        return pr;
    }

    public List<CustomerLeadDto> findPage(Integer page, Integer size, Long salespersonId, String status, String auditStatus,
                                          String keyword, String source, String startDate, String endDate, String sortBy, String sortOrder) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : size;
        int offset = (p - 1) * s;
        List<CustomerLead> list = leadMapper.findByConditions(salespersonId, status, auditStatus, keyword, source, startDate, endDate, sortBy, sortOrder, offset, s);
        return list.stream().map(LeadDtoConverter::toDto).collect(Collectors.toList());
    }

    public long countByConditions(Long salespersonId, String status, String auditStatus, String keyword, String source, String startDate, String endDate) {
        return leadMapper.countByConditions(salespersonId, status, auditStatus, keyword, source, startDate, endDate);
    }

}

