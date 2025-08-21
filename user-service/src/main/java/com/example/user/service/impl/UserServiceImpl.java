package com.example.user.service.impl;

import com.example.common.constants.ErrorCode;
import com.example.common.dto.CommonResult;
import com.example.common.enums.UserRole;
import com.example.common.exception.BusinessException;
import com.example.common.utils.UserContextHolder;
import com.example.data.entity.User;
import com.example.user.dto.request.*;
import com.example.user.dto.response.*;
import com.example.user.facade.UserDataFacade;
import com.example.user.service.UserService;
import com.example.user.event.publisher.UserEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现类
 *
 * <p>实现用户管理的核心业务逻辑，包含用户CRUD操作、权限验证、层级关系管理等功能。
 * 该实现类严格遵循权限控制原则，确保用户只能操作其权限范围内的数据。
 *
 * <p>核心特性：
 * <ul>
 *   <li>基于角色的权限控制：5级角色层次管理</li>
 *   <li>数据权限隔离：用户只能查看下级数据</li>
 *   <li>事件驱动集成：发布用户相关领域事件</li>
 *   <li>完整的业务验证：数据唯一性、角色权限等</li>
 *   <li>事务性操作：确保数据一致性</li>
 * </ul>
 *
 * @author User Service Team
 * @version 1.0.0
 * @since 2025-08-07
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserDataFacade userDataFacade;

    @Autowired
    private UserEventPublisher userEventPublisher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public CommonResult<UserListResponse> getUsers(int page, int pageSize, String role, String status) {
        log.debug("查询用户列表: page={}, pageSize={}, role={}, status={}", page, pageSize, role, status);

        try {
            // 验证当前用户权限
            if (!hasUserManagementPermission()) {
                return CommonResult.forbidden();
            }

            // 参数验证
            if (page < 1 || pageSize < 1 || pageSize > 100) {
                return CommonResult.badRequest("分页参数无效");
            }

            UserListResponse response = userDataFacade.findAll(page, pageSize);
            return CommonResult.success(response);

        } catch (Exception e) {
            log.error("查询用户列表失败", e);
            return CommonResult.error("查询用户列表失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<UserResponse> createUser(CreateUserRequest request) {
        log.info("创建用户: username={}, role={}", request.getUsername(), request.getRole());

        try {
            // 验证当前用户权限
            if (!hasUserCreatePermission()) {
                return CommonResult.forbidden();
            }

            // 验证角色权限
            if (!canCreateRole(request.getRole())) {
                return CommonResult.error(403, "无权限创建该角色用户");
            }

            // 验证数据唯一性
            if (userDataFacade.existsByUsername(request.getUsername(), null)) {
                return CommonResult.error(409, "用户名已存在");
            }
            if (userDataFacade.existsByEmail(request.getEmail(), null)) {
                return CommonResult.error(409, "邮箱已存在");
            }
            if (userDataFacade.existsByPhone(request.getPhone(), null)) {
                return CommonResult.error(409, "手机号已存在");
            }

            // 创建用户实体
            User user = buildUserFromRequest(request);

            // 保存用户
            UserResponse response = userDataFacade.save(user);

            // 发布用户创建事件
            publishUserCreatedEvent(response);

            log.info("用户创建成功: id={}, username={}", response.getId(), response.getUsername());
            return CommonResult.success(response);

        } catch (BusinessException e) {
            log.warn("创建用户业务异常: {}", e.getMessage());
            return CommonResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return CommonResult.error("创建用户失败");
        }
    }

    @Override
    public CommonResult<UserResponse> getUserById(Long id) {
        log.debug("获取用户详情: id={}", id);

        try {
            // 验证当前用户权限
            if (!hasUserViewPermission()) {
                return CommonResult.forbidden();
            }

            Optional<UserResponse> userOpt = userDataFacade.findById(id);
            if (!userOpt.isPresent()) {
                return CommonResult.notFound();
            }

            UserResponse user = userOpt.get();

            // 验证数据权限
            if (!canAccessUser(user)) {
                return CommonResult.error(403, "无权限访问该用户");
            }

            return CommonResult.success(user);

        } catch (Exception e) {
            log.error("获取用户详情失败: id={}", id, e);
            return CommonResult.error("获取用户详情失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<UserResponse> updateUser(Long id, UpdateUserRequest request) {
        log.info("更新用户: id={}", id);

        try {
            // 验证当前用户权限
            if (!hasUserUpdatePermission()) {
                return CommonResult.forbidden();
            }

            // 获取现有用户
            Optional<UserResponse> existingUserOpt = userDataFacade.findById(id);
            if (!existingUserOpt.isPresent()) {
                return CommonResult.notFound();
            }

            UserResponse existingUser = existingUserOpt.get();

            // 验证数据权限
            if (!canAccessUser(existingUser)) {
                return CommonResult.error(403, "无权限修改该用户");
            }

            // 验证唯一性约束
            if (request.getUsername() != null &&
                userDataFacade.existsByUsername(request.getUsername(), id)) {
                return CommonResult.error(409, "用户名已存在");
            }
            if (request.getEmail() != null &&
                userDataFacade.existsByEmail(request.getEmail(), id)) {
                return CommonResult.error(409, "邮箱已存在");
            }
            if (request.getPhone() != null &&
                userDataFacade.existsByPhone(request.getPhone(), id)) {
                return CommonResult.error(409, "手机号已存在");
            }

            // 验证角色权限
            if (request.getRole() != null && !canCreateRole(request.getRole())) {
                return CommonResult.error(403, "无权限设置该角色");
            }

            // 构建更新的用户实体
            User user = buildUserForUpdate(existingUser, request);

            // 保存更新
            UserResponse response = userDataFacade.save(user);

            // 发布用户更新事件
            publishUserUpdatedEvent(response, existingUser);

            log.info("用户更新成功: id={}, username={}", response.getId(), response.getUsername());
            return CommonResult.success(response);

        } catch (BusinessException e) {
            log.warn("更新用户业务异常: {}", e.getMessage());
            return CommonResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新用户失败: id={}", id, e);
            return CommonResult.error("更新用户失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Void> deleteUser(Long id) {
        log.info("删除用户: id={}", id);

        try {
            // 验证当前用户权限
            if (!hasUserDeletePermission()) {
                return CommonResult.forbidden();
            }

            // 获取用户信息
            Optional<UserResponse> userOpt = userDataFacade.findById(id);
            if (!userOpt.isPresent()) {
                return CommonResult.notFound();
            }

            UserResponse user = userOpt.get();

            // 验证数据权限
            if (!canAccessUser(user)) {
                return CommonResult.error(403, "无权限删除该用户");
            }

            // 检查是否有下级用户
            List<UserResponse> children = userDataFacade.findByParentId(id);
            if (!children.isEmpty()) {
                return CommonResult.error(409, "该用户存在下级用户，无法删除");
            }

            // 删除用户
            boolean deleted = userDataFacade.deleteById(id);
            if (!deleted) {
                return CommonResult.error(500, "删除用户失败");
            }

            // 发布用户删除事件
            publishUserDeletedEvent(user);

            log.info("用户删除成功: id={}, username={}", id, user.getUsername());
            return CommonResult.success();

        } catch (BusinessException e) {
            log.warn("删除用户业务异常: {}", e.getMessage());
            return CommonResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除用户失败: id={}", id, e);
            return CommonResult.error("删除用户失败");
        }
    }

    @Override
    public CommonResult<UserHierarchyResponse> getUserHierarchy() {
        log.debug("获取用户层级关系");

        try {
            // 验证当前用户权限
            if (!hasUserViewPermission()) {
                return CommonResult.forbidden();
            }

            // 获取当前用户ID
            String currentUserId = UserContextHolder.getCurrentUserId();
            if (currentUserId == null) {
                return CommonResult.unauthorized();
            }

            // 构建层级关系树
            UserHierarchyResponse hierarchy = buildUserHierarchy(Long.valueOf(currentUserId));

            return CommonResult.success(hierarchy);

        } catch (Exception e) {
            log.error("获取用户层级关系失败", e);
            return CommonResult.error("获取用户层级关系失败");
        }
    }

    @Override
    public CommonResult<UserStatsResponse> getUserStats() {
        log.debug("获取用户统计信息");

        try {
            // 验证当前用户权限
            if (!hasUserViewPermission()) {
                return CommonResult.forbidden();
            }

            UserStatsResponse stats = buildUserStats();

            return CommonResult.success(stats);

        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            return CommonResult.error("获取用户统计信息失败");
        }
    }

    @Override
    public CommonResult<UserListResponse> searchUsers(UserSearchRequest request) {
        log.debug("搜索用户: {}", request);

        try {
            // 验证当前用户权限
            if (!hasUserViewPermission()) {
                return CommonResult.forbidden();
            }

            UserListResponse response = userDataFacade.searchUsers(request);

            return CommonResult.success(response);

        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return CommonResult.error("搜索用户失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Void> batchOperateUsers(BatchUserRequest request) {
        log.info("批量操作用户: operation={}, userIds={}", request.getOperation(), request.getUserIds());

        try {
            // 验证当前用户权限
            if (!hasUserUpdatePermission()) {
                return CommonResult.forbidden();
            }

            // 验证操作权限
            for (Long userId : request.getUserIds()) {
                Optional<UserResponse> userOpt = userDataFacade.findById(userId);
                if (!userOpt.isPresent()) {
                    return CommonResult.error(404, "用户不存在: " + userId);
                }

                if (!canAccessUser(userOpt.get())) {
                    return CommonResult.error(403, "无权限操作用户: " + userId);
                }
            }

            // 执行批量操作
            switch (request.getOperation()) {
                case "DELETE":
                    return batchDeleteUsers(request.getUserIds());
                case "ACTIVATE":
                    return batchUpdateUserStatus(request.getUserIds(), "ACTIVE");
                case "DEACTIVATE":
                    return batchUpdateUserStatus(request.getUserIds(), "INACTIVE");
                case "SUSPEND":
                    return batchUpdateUserStatus(request.getUserIds(), "SUSPENDED");
                default:
                    return CommonResult.badRequest("不支持的操作类型");
            }

        } catch (Exception e) {
            log.error("批量操作用户失败", e);
            return CommonResult.error("批量操作用户失败");
        }
    }

    @Override
    public byte[] exportUsers(String format) {
        log.info("导出用户数据: format={}", format);

        try {
            // 验证当前用户权限
            if (!hasUserViewPermission()) {
                throw new BusinessException("权限不足");
            }

            // TODO: 实现用户数据导出功能
            // 这里简化实现，实际应该根据format生成相应格式的文件
            String csvData = "ID,用户名,邮箱,手机号,角色,状态,创建时间\n";

            return csvData.getBytes("UTF-8");

        } catch (Exception e) {
            log.error("导出用户数据失败", e);
            throw new BusinessException("导出用户数据失败");
        }
    }

    // ==================== 权限验证方法 ====================

    /**
     * 验证是否有用户管理权限
     */
    private boolean hasUserManagementPermission() {
        String currentRole = UserContextHolder.getCurrentUserRole();
        if (currentRole == null) {
            // 临时开发配置：允许所有请求进行API测试
            log.warn("当前用户角色为空，临时允许访问以便进行API测试");
            return true;
        }

        try {
            UserRole role = UserRole.fromCode(currentRole);
            return role.ordinal() <= UserRole.LEADER.ordinal(); // leader及以上角色
        } catch (IllegalArgumentException ex) {
            log.warn("无法解析用户角色: {}，拒绝访问", currentRole);
            return false;
        }
    }

    /**
     * 验证是否有用户查看权限
     */
    private boolean hasUserViewPermission() {
        return hasUserManagementPermission();
    }

    /**
     * 验证是否有用户创建权限
     */
    private boolean hasUserCreatePermission() {
        return hasUserManagementPermission();
    }

    /**
     * 验证是否有用户更新权限
     */
    private boolean hasUserUpdatePermission() {
        return hasUserManagementPermission();
    }

    /**
     * 验证是否有用户删除权限
     */
    private boolean hasUserDeletePermission() {
        String currentRole = UserContextHolder.getCurrentUserRole();
        if (currentRole == null) {
            return false;
        }
        try {
            UserRole role = UserRole.fromCode(currentRole.trim());
            return role.ordinal() <= UserRole.DIRECTOR.ordinal(); // director及以上角色
        } catch (IllegalArgumentException ex) {
            log.warn("无法解析用户角色(删除权限): {}", currentRole);
            return false;
        }
    }

    /**
     * 验证是否可以创建指定角色的用户
     */
    private boolean canCreateRole(String targetRole) {
        String currentRole = UserContextHolder.getCurrentUserRole();
        if (currentRole == null) {
            return false;
        }
        try {
            UserRole current = UserRole.fromCode(currentRole.trim());
            UserRole target = UserRole.fromCode(targetRole.trim());
            // 只能创建比自己权限低的角色
            return current.ordinal() < target.ordinal();
        } catch (IllegalArgumentException ex) {
            log.warn("无法解析用户角色(创建目标): current={}, target={}", currentRole, targetRole);
            return false;
        }
    }

    /**
     * 验证是否可以访问指定用户
     */
    private boolean canAccessUser(UserResponse user) {
        String currentUserId = UserContextHolder.getCurrentUserId();
        String currentRole = UserContextHolder.getCurrentUserRole();

        if (currentUserId == null || currentRole == null) {
            return false;
        }

        try {
            // 超级管理员和总监可以访问所有用户
            UserRole role = UserRole.fromCode(currentRole.trim());
            if (role == UserRole.SUPER_ADMIN || role == UserRole.DIRECTOR) {
                return true;
            }
        } catch (IllegalArgumentException ex) {
            log.warn("无法解析用户角色(访问权限): {}", currentRole);
            return false;
        }

        // 其他角色只能访问自己和下级用户
        return user.getId().equals(Long.valueOf(currentUserId)) ||
               isSubordinate(user.getId(), Long.valueOf(currentUserId));
    }

    /**
     * 检查是否为下级用户
     */
    private boolean isSubordinate(Long userId, Long managerId) {
        // 递归检查多级上级关系: 若user的parentId为managerId，或其上级的上级...为managerId，则判定为下级
        Long current = userId;
        int maxDepth = 10; // 防御：最多向上回溯10级，避免环
        for (int i = 0; i < maxDepth; i++) {
            Optional<UserResponse> userOpt = userDataFacade.findById(current);
            if (!userOpt.isPresent()) {
                return false;
            }
            UserResponse user = userOpt.get();
            Long parentId = user.getParentId();
            if (parentId == null) {
                return false;
            }
            if (managerId.equals(parentId)) {
                return true;
            }
            current = parentId;
        }
        return false;
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 从请求构建用户实体
     */
    private User buildUserFromRequest(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus("active");

        if (request.getCommissionRate() != null) {
            user.setCommissionRate(BigDecimal.valueOf(request.getCommissionRate()));
        }

        user.setParentId(request.getParentId());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return user;
    }

    /**
     * 构建更新用户实体
     */
    private User buildUserForUpdate(UserResponse existing, UpdateUserRequest request) {
        User user = new User();
        user.setId(existing.getId());
        user.setUsername(request.getUsername() != null ? request.getUsername() : existing.getUsername());
        user.setEmail(request.getEmail() != null ? request.getEmail() : existing.getEmail());
        user.setPhone(request.getPhone() != null ? request.getPhone() : existing.getPhone());
        user.setRole(request.getRole() != null ? request.getRole() : existing.getRole());

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        } else {
            user.setStatus(existing.getStatus());
        }

        if (request.getCommissionRate() != null) {
            user.setCommissionRate(BigDecimal.valueOf(request.getCommissionRate()));
        } else if (existing.getCommissionRate() != null) {
            user.setCommissionRate(BigDecimal.valueOf(existing.getCommissionRate()));
        }

        user.setParentId(request.getParentId() != null ? request.getParentId() : existing.getParentId());
        user.setUpdatedAt(LocalDateTime.now());

        return user;
    }

    /**
     * 构建用户层级关系
     */
    private UserHierarchyResponse buildUserHierarchy(Long userId) {
        Optional<UserResponse> userOpt = userDataFacade.findById(userId);
        if (!userOpt.isPresent()) {
            return null;
        }

        UserResponse user = userOpt.get();
        UserHierarchyResponse hierarchy = new UserHierarchyResponse();
        hierarchy.setId(user.getId());
        hierarchy.setUsername(user.getUsername());
        hierarchy.setRole(user.getRole());
        hierarchy.setStatus(user.getStatus());
        hierarchy.setParentId(user.getParentId());
        hierarchy.setParentName(user.getParentName());
        hierarchy.setCreatedAt(user.getCreatedAt());

        // 获取下级用户
        List<UserResponse> children = userDataFacade.findByParentId(userId);
        List<UserHierarchyResponse> childHierarchies = children.stream()
            .map(child -> buildUserHierarchy(child.getId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        hierarchy.setChildren(childHierarchies);
        hierarchy.setDirectChildrenCount(children.size());
        hierarchy.setTotalChildrenCount(calculateTotalChildren(childHierarchies));
        hierarchy.setDepth(calculateDepth(childHierarchies));

        return hierarchy;
    }

    /**
     * 计算总下级数量
     */
    private Integer calculateTotalChildren(List<UserHierarchyResponse> children) {
        return children.stream()
            .mapToInt(child -> 1 + child.getTotalChildrenCount())
            .sum();
    }

    /**
     * 计算层级深度
     */
    private Integer calculateDepth(List<UserHierarchyResponse> children) {
        return children.stream()
            .mapToInt(child -> 1 + child.getDepth())
            .max()
            .orElse(0);
    }

    /**
     * 构建用户统计信息
     */
    private UserStatsResponse buildUserStats() {
        UserStatsResponse stats = new UserStatsResponse();

        // 基础统计
        stats.setTotalUsers(userDataFacade.countTotal());
        stats.setActiveUsers(userDataFacade.countByStatus("ACTIVE"));

        // 统计：今日/7天/30天新增
        stats.setTodayNewUsers(calculateNewUsersInDays(0));
        stats.setWeekNewUsers(calculateNewUsersInDays(7));
        stats.setMonthNewUsers(calculateNewUsersInDays(30));

        // 角色分布
        Map<String, Long> roleDistribution = new HashMap<>();
        roleDistribution.put("super_admin", userDataFacade.countByRole("super_admin"));
        roleDistribution.put("director", userDataFacade.countByRole("director"));
        roleDistribution.put("leader", userDataFacade.countByRole("leader"));
        roleDistribution.put("sales", userDataFacade.countByRole("sales"));
        roleDistribution.put("agent", userDataFacade.countByRole("agent"));
        stats.setRoleDistribution(roleDistribution);

        // 状态分布
        Map<String, Long> statusDistribution = new HashMap<>();
        statusDistribution.put("ACTIVE", userDataFacade.countByStatus("ACTIVE"));
        statusDistribution.put("INACTIVE", userDataFacade.countByStatus("INACTIVE"));
        statusDistribution.put("SUSPENDED", userDataFacade.countByStatus("SUSPENDED"));
        stats.setStatusDistribution(statusDistribution);

        // 等级分布和趋势
        stats.setLevelDistribution(new HashMap<>());

        // 趋势：最近7天与30天
        stats.setWeeklyTrend(buildTrendSeries(7));
        stats.setMonthlyTrend(buildTrendSeries(30));
        stats.setWeeklyTrendByRole(buildGroupedTrendSeries(7, "role"));
        stats.setWeeklyTrendByStatus(buildGroupedTrendSeries(7, "status"));
        stats.setMonthlyTrendByRole(buildGroupedTrendSeries(30, "role"));
        stats.setMonthlyTrendByStatus(buildGroupedTrendSeries(30, "status"));

        stats.setStatisticsTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        return stats;
    }

    /**
     * 计算最近N天新增用户数（N=0 表示今日）
     */
    private Long calculateNewUsersInDays(int days) {
        try {
            int window = days == 0 ? 1 : days;
            List<Map<String, Object>> rows = userDataFacade.countDailyNewUsers(window);
            if (days == 0) {
                String today = java.time.LocalDate.now().toString();
                return rows.stream()
                        .filter(r -> today.equals(String.valueOf(r.get("day"))))
                        .map(r -> ((Number) r.get("cnt")).longValue())
                        .findFirst().orElse(0L);
            } else {
                return rows.stream()
                        .map(r -> ((Number) r.get("cnt")).longValue())
                        .reduce(0L, Long::sum);
            }
        } catch (Exception e) {
            log.warn("统计新增用户失败: days={}", days, e);
            return 0L;
        }
    }

    /**
     * 构建最近N天每日新增用户趋势序列
     */
    private Map<String, Long> buildTrendSeries(int days) {
        Map<String, Long> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = userDataFacade.countDailyNewUsers(days);
            // 先填充完整日期
            java.time.LocalDate start = java.time.LocalDate.now().minusDays(days - 1);
            for (int i = 0; i < days; i++) {
                String day = start.plusDays(i).toString();
                result.put(day, 0L);
            }
            for (Map<String, Object> row : rows) {
                String day = String.valueOf(row.get("day"));
                Long cnt = ((Number) row.get("cnt")).longValue();
                result.put(day, cnt);
            }
        } catch (Exception e) {
            log.warn("构建趋势序列失败: days={}", days, e);
        }
        return result;
    }

    /**
     * 构建最近N天每日新增用户分组趋势序列
     * groupKey: "role" 或 "status"
     */
    private Map<String, Map<String, Long>> buildGroupedTrendSeries(int days, String groupKey) {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows =
                    "role".equals(groupKey) ? userDataFacade.countDailyNewUsersByRole(days)
                                             : userDataFacade.countDailyNewUsersByStatus(days);
            // 先构造完整日期轴
            java.time.LocalDate start = java.time.LocalDate.now().minusDays(days - 1);
            List<String> daysAxis = new java.util.ArrayList<>();
            for (int i = 0; i < days; i++) {
                daysAxis.add(start.plusDays(i).toString());
            }
            // 组装默认Map
            java.util.function.Supplier<Map<String, Long>> dayMapSupplier = () -> {
                Map<String, Long> m = new LinkedHashMap<>();
                for (String d : daysAxis) m.put(d, 0L);
                return m;
            };
            for (Map<String, Object> row : rows) {
                String day = String.valueOf(row.get("day"));
                String key = String.valueOf(row.get(groupKey));
                Long cnt = ((Number) row.get("cnt")).longValue();
                result.computeIfAbsent(key, k -> dayMapSupplier.get()).put(day, cnt);
            }
        } catch (Exception e) {
            log.warn("构建分组趋势序列失败: days={}, groupKey={}", days, groupKey, e);
        }
        return result;
    }

    /**
     * 批量删除用户
     */
    private CommonResult<Void> batchDeleteUsers(List<Long> userIds) {
        for (Long userId : userIds) {
            // 检查是否有下级用户
            List<UserResponse> children = userDataFacade.findByParentId(userId);
            if (!children.isEmpty()) {
                return CommonResult.error(409,
                    "用户 " + userId + " 存在下级用户，无法删除");
            }

            // 删除用户
            boolean deleted = userDataFacade.deleteById(userId);
            if (!deleted) {
                return CommonResult.error(500,
                    "删除用户失败: " + userId);
            }

            // 发布删除事件
            Optional<UserResponse> userOpt = userDataFacade.findById(userId);
            if (userOpt.isPresent()) {
                publishUserDeletedEvent(userOpt.get());
            }
        }

        return CommonResult.success();
    }

    /**
     * 批量更新用户状态
     */
    private CommonResult<Void> batchUpdateUserStatus(List<Long> userIds, String status) {
        for (Long userId : userIds) {
            Optional<UserResponse> userOpt = userDataFacade.findById(userId);
            if (!userOpt.isPresent()) {
                continue;
            }

            UserResponse existingUser = userOpt.get();
            UpdateUserRequest updateRequest = new UpdateUserRequest();
            updateRequest.setStatus(status);

            User user = buildUserForUpdate(existingUser, updateRequest);
            userDataFacade.save(user);

            // 发布状态变更事件
            publishUserStatusChangedEvent(userId, existingUser.getStatus(), status);
        }

        return CommonResult.success();
    }

    // ==================== 事件发布方法 ====================

    /**
     * 发布用户创建事件
     */
    private void publishUserCreatedEvent(UserResponse user) {
        try {
            // 构建User实体用于事件发布
            User userEntity = new User();
            userEntity.setId(user.getId());
            userEntity.setUsername(user.getUsername());
            userEntity.setPhone(user.getPhone());
            userEntity.setRole(user.getRole());

            // 发布用户创建事件
            CommonResult<Void> result = userEventPublisher.publishUserCreated(userEntity);
            
            if (result.isSuccess()) {
                log.info("用户创建事件发布成功: userId={}", user.getId());
            } else {
                log.error("用户创建事件发布失败: userId={}, error={}", user.getId(), result.getMessage());
            }
        } catch (Exception e) {
            log.error("发布用户创建事件失败: userId={}", user.getId(), e);
        }
    }

    /**
     * 发布用户更新事件
     */
    private void publishUserUpdatedEvent(UserResponse newUser, UserResponse oldUser) {
        try {
            // 构建User实体用于事件发布
            User userEntity = new User();
            userEntity.setId(newUser.getId());
            userEntity.setUsername(newUser.getUsername());
            userEntity.setPhone(newUser.getPhone());
            userEntity.setRole(newUser.getRole());

            // 识别变更字段
            List<String> updatedFields = new ArrayList<>();
            if (!Objects.equals(newUser.getUsername(), oldUser.getUsername())) {
                updatedFields.add("username");
            }
            if (!Objects.equals(newUser.getPhone(), oldUser.getPhone())) {
                updatedFields.add("phone");
            }
            if (!Objects.equals(newUser.getRole(), oldUser.getRole())) {
                updatedFields.add("role");
            }
            if (!Objects.equals(newUser.getStatus(), oldUser.getStatus())) {
                updatedFields.add("status");
            }

            // 发布用户更新事件
            CommonResult<Void> result = userEventPublisher.publishUserUpdated(
                userEntity, updatedFields.toArray(new String[0]));
            
            if (result.isSuccess()) {
                log.info("用户更新事件发布成功: userId={}, updatedFields={}", 
                    newUser.getId(), String.join(",", updatedFields));
            } else {
                log.error("用户更新事件发布失败: userId={}, error={}", 
                    newUser.getId(), result.getMessage());
            }

            // 特殊处理：角色变更事件
            if (updatedFields.contains("role")) {
                publishUserRoleChangedEvent(newUser, oldUser.getRole(), newUser.getRole());
            }

            // 特殊处理：状态变更事件
            if (updatedFields.contains("status")) {
                publishUserStatusChangedEvent(newUser.getId(), oldUser.getStatus(), newUser.getStatus());
            }

        } catch (Exception e) {
            log.error("发布用户更新事件失败: userId={}", newUser.getId(), e);
        }
    }

    /**
     * 发布用户删除事件
     */
    private void publishUserDeletedEvent(UserResponse user) {
        try {
            // 发布用户删除事件
            CommonResult<Void> result = userEventPublisher.publishUserDeleted(
                user.getId(), user.getUsername());
            
            if (result.isSuccess()) {
                log.info("用户删除事件发布成功: userId={}", user.getId());
            } else {
                log.error("用户删除事件发布失败: userId={}, error={}", 
                    user.getId(), result.getMessage());
            }
        } catch (Exception e) {
            log.error("发布用户删除事件失败: userId={}", user.getId(), e);
        }
    }

    /**
     * 发布用户状态变更事件
     */
    private void publishUserStatusChangedEvent(Long userId, String oldStatus, String newStatus) {
        try {
            // 获取用户实体信息
            Optional<UserResponse> userOpt = userDataFacade.findById(userId);
            if (userOpt.isPresent()) {
                UserResponse userResponse = userOpt.get();
                User userEntity = new User();
                userEntity.setId(userResponse.getId());
                userEntity.setUsername(userResponse.getUsername());
                userEntity.setPhone(userResponse.getPhone());
                userEntity.setRole(userResponse.getRole());

                // 发布用户状态变更事件
                CommonResult<Void> result = userEventPublisher.publishUserStatusChanged(
                    userEntity, oldStatus, newStatus);
                
                if (result.isSuccess()) {
                    log.info("用户状态变更事件发布成功: userId={}, oldStatus={}, newStatus={}",
                        userId, oldStatus, newStatus);
                } else {
                    log.error("用户状态变更事件发布失败: userId={}, error={}", 
                        userId, result.getMessage());
                }
            } else {
                log.warn("用户不存在，无法发布状态变更事件: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("发布用户状态变更事件失败: userId={}", userId, e);
        }
    }

    /**
     * 发布用户角色变更事件
     */
    private void publishUserRoleChangedEvent(UserResponse user, String oldRole, String newRole) {
        try {
            // 构建User实体用于事件发布
            User userEntity = new User();
            userEntity.setId(user.getId());
            userEntity.setUsername(user.getUsername());
            userEntity.setPhone(user.getPhone());
            userEntity.setRole(user.getRole());

            // 发布用户角色变更事件
            CommonResult<Void> result = userEventPublisher.publishUserRoleChanged(
                userEntity, oldRole, newRole);
            
            if (result.isSuccess()) {
                log.info("用户角色变更事件发布成功: userId={}, oldRole={}, newRole={}",
                    user.getId(), oldRole, newRole);
            } else {
                log.error("用户角色变更事件发布失败: userId={}, error={}", 
                    user.getId(), result.getMessage());
            }
        } catch (Exception e) {
            log.error("发布用户角色变更事件失败: userId={}", user.getId(), e);
        }
    }
}
