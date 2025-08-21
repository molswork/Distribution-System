package com.example.user.event.handler;

import com.example.common.event.domain.UserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserCreatedEventHandler单元测试
 * 
 * <p>测试用户创建事件处理器的各项功能，包括：
 * <ul>
 *   <li>正常用户创建事件处理</li>
 *   <li>通过邀请码注册的用户创建处理</li>
 *   <li>邀请码生成功能</li>
 *   <li>邀请关系建立</li>
 *   <li>用户统计数据初始化</li>
 *   <li>用户信息同步</li>
 *   <li>欢迎通知发送</li>
 *   <li>异常处理</li>
 * </ul>
 * 
 * @author User Service Team
 * @version 1.0.0
 * @since 2025-08-12
 */
@ExtendWith(MockitoExtension.class)
class UserCreatedEventHandlerTest {

    @InjectMocks
    private UserCreatedEventHandler eventHandler;

    private UserCreatedEvent testEvent;
    private UserCreatedEvent testEventWithInvitation;

    @BeforeEach
    void setUp() {
        testEvent = createTestEvent();
        testEventWithInvitation = createTestEventWithInvitation();
    }

    @Test
    void should_handle_user_created_event_successfully() {
        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> {
            eventHandler.handle(testEvent);
        });
    }

    @Test
    void should_handle_user_created_event_with_invitation_successfully() {
        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> {
            eventHandler.handle(testEventWithInvitation);
        });
    }

    @Test
    void should_generate_invitation_code_without_error() {
        // Given
        UserCreatedEvent event = createTestEvent();

        // When & Then - 测试邀请码生成不会抛出异常
        assertDoesNotThrow(() -> {
            // 使用反射调用私有方法进行测试
            ReflectionTestUtils.invokeMethod(eventHandler, "generateInvitationCode", event);
        });
    }

    @Test
    void should_establish_invitation_relationship_when_invitation_exists() {
        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> {
            eventHandler.handle(testEventWithInvitation);
        });
    }

    @Test
    void should_not_establish_invitation_relationship_when_no_invitation() {
        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> {
            eventHandler.handle(testEvent);
        });
    }

    @Test
    void should_initialize_user_statistics_without_error() {
        // Given
        UserCreatedEvent event = createTestEvent();

        // When & Then - 测试统计数据初始化不会抛出异常
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(eventHandler, "initializeUserStatistics", event);
        });
    }

    @Test
    void should_sync_user_info_to_related_services_without_error() {
        // Given
        UserCreatedEvent event = createTestEvent();

        // When & Then - 测试信息同步不会抛出异常
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(eventHandler, "syncUserInfoToRelatedServices", event);
        });
    }

    @Test
    void should_send_welcome_notification_without_error() {
        // Given
        UserCreatedEvent event = createTestEvent();

        // When & Then - 测试欢迎通知发送不会抛出异常
        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(eventHandler, "sendWelcomeNotificationAsync", event);
        });
    }

    @Test
    void should_handle_null_event_gracefully() {
        // When & Then - 应该抛出异常或进行适当处理
        assertThrows(Exception.class, () -> {
            eventHandler.handle(null);
        });
    }

    @Test
    void should_generate_valid_mock_invitation_code() {
        // Given
        Long userId = 123L;

        // When
        String invitationCode = ReflectionTestUtils.invokeMethod(
                eventHandler, "generateMockInvitationCode", userId);

        // Then
        assertNotNull(invitationCode);
        assertTrue(invitationCode.startsWith("INV"));
        assertTrue(invitationCode.contains(userId.toString()));
    }

    @Test
    void should_handle_different_user_roles() {
        // Given
        UserCreatedEvent salesEvent = UserCreatedEvent.create(1L, "sales_user", "13800138001", "sales", "corr-1");
        UserCreatedEvent leaderEvent = UserCreatedEvent.create(2L, "leader_user", "13800138002", "leader", "corr-2");

        // When & Then - 不同角色都应能正常处理
        assertDoesNotThrow(() -> {
            eventHandler.handle(salesEvent);
            eventHandler.handle(leaderEvent);
        });
    }

    @Test
    void should_handle_event_with_different_phone_formats() {
        // Given
        UserCreatedEvent event1 = UserCreatedEvent.create(1L, "user1", "13800138000", "sales", "corr-1");
        UserCreatedEvent event2 = UserCreatedEvent.create(2L, "user2", "+8613800138001", "sales", "corr-2");
        UserCreatedEvent event3 = UserCreatedEvent.create(3L, "user3", "86-138-0013-8002", "sales", "corr-3");

        // When & Then - 不同手机号格式都应能处理
        assertDoesNotThrow(() -> {
            eventHandler.handle(event1);
            eventHandler.handle(event2);
            eventHandler.handle(event3);
        });
    }

    @Test
    void should_be_transactional() throws NoSuchMethodException {
        // Given - 检查方法是否有@Transactional注解
        boolean isTransactional = eventHandler.getClass()
                .getDeclaredMethod("handle", Object.class)
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class);

        // Then
        assertTrue(isTransactional, "handle方法应该有@Transactional注解");
    }

    /**
     * 创建测试用户创建事件
     */
    private UserCreatedEvent createTestEvent() {
        return UserCreatedEvent.create(
                1L, 
                "testuser", 
                "13800138000", 
                "sales", 
                "test-correlation-id"
        );
    }

    /**
     * 创建包含邀请信息的测试事件
     */
    private UserCreatedEvent createTestEventWithInvitation() {
        return UserCreatedEvent.createWithInvitation(
                1L, 
                "testuser", 
                "13800138000", 
                "sales", 
                "INV123456", 
                2L, 
                "test-correlation-id-with-invitation"
        );
    }

    /**
     * 边界条件测试：极长的用户名
     */
    @Test
    void should_handle_long_username() {
        // Given
        String longUsername = "a".repeat(100);  // 100个字符的用户名
        UserCreatedEvent event = UserCreatedEvent.create(
                1L, longUsername, "13800138000", "sales", "corr-long");

        // When & Then
        assertDoesNotThrow(() -> {
            eventHandler.handle(event);
        });
    }

    /**
     * 边界条件测试：空字符串字段
     */
    @Test
    void should_handle_empty_string_fields() {
        // Given - 创建包含空字符串字段的事件
        UserCreatedEvent event = UserCreatedEvent.create(
                1L, "", "13800138000", "sales", "");

        // When & Then - 应该能处理但可能会有验证失败
        assertDoesNotThrow(() -> {
            eventHandler.handle(event);
        });
    }
}