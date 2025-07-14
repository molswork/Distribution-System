package com.example.auth.mapper;

import com.example.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户数据访问层接口
 * 
 * <p>负责用户表（users）的数据访问操作，提供用户信息的增删改查功能。
 * 使用 MyBatis 框架实现数据持久化，对应的 SQL 映射文件为 UserMapper.xml。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>用户信息查询（按手机号、ID、邀请码）</li>
 *   <li>用户信息的新增和更新</li>
 *   <li>手机号唯一性校验</li>
 *   <li>邀请码生成</li>
 * </ul>
 * 
 * <p>使用场景：
 * <ul>
 *   <li>用户注册时的手机号重复检查</li>
 *   <li>登录时的用户信息查询</li>
 *   <li>邀请关系的建立和查询</li>
 * </ul>
 * 
 * @author mols
 * @date 2025-07-12
 * @since 1.0.0
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    User selectByPhone(@Param("phone") String phone);
    
    /**
     * 根据用户ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User selectById(@Param("id") Long id);
    
    /**
     * 根据邀请码查询用户
     *
     * @param inviteCode 邀请码
     * @return 用户信息
     */
    User selectByInviteCode(@Param("inviteCode") String inviteCode);
    
    /**
     * 插入用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int insert(User user);
    
    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int update(User user);
    
    /**
     * 检查手机号是否已存在
     *
     * @param phone 手机号
     * @return 是否存在
     */
    boolean existsByPhone(@Param("phone") String phone);
    
    /**
     * 生成新的邀请码
     *
     * @return 未被使用的邀请码
     */
    String generateUniqueInviteCode();
}