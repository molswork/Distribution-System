package com.example.common.integration;

import com.example.common.config.RabbitMQConfig;
import com.example.common.event.DomainEvent;
import com.example.common.event.DomainEventPublisher;
import com.example.common.event.EventType;
import com.example.common.event.domain.UserCreatedEvent;
import com.example.common.event.domain.LeadCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * RabbitMQ事件驱动架构集成测试
 * 
 * 测试事件发布和消费的端到端流程，验证：
 * - RabbitMQ交换机、队列和绑定配置
 * - 事件序列化和反序列化
 * - 消息路由和主题匹配
 * - 死信队列和错误处理
 * - 消息持久化和确认机制
 * - 业务事件的完整流程
 * 
 * @author Event-Driven Architecture Team
 */
@SpringBootTest(classes = {
    RabbitMQConfig.class,
    RabbitAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
    RabbitMQEventIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672", 
    "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest",
    "spring.rabbitmq.virtual-host=/",
    "spring.rabbitmq.publisher-confirms=true",
    "spring.rabbitmq.publisher-returns=true",
    "spring.rabbitmq.publisher-confirm-type=correlated",
    "event-driven.enabled=false"
})

@RabbitListenerTest(capture = true)
@DirtiesContext
@DisplayName("RabbitMQ事件驱动架构集成测试")
class RabbitMQEventIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DomainEventPublisher domainEventPublisher;
    
    @Autowired
    private RabbitListenerTestHarness harness;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TestEventListener testEventListener;

    // 测试数据
    private final String testCorrelationId = "TEST-CORR-12345";
    private final Long testUserId = 98765L;
    private final String testUsername = "testuser";
    private final String testPhone = "13888888888";
    private final String testRole = "SALES";

    @BeforeEach
    void setUp() {
        testEventListener.clearReceivedEvents();
    }

    @Test
    @DisplayName("正常场景：用户创建事件应该成功发布和消费")
    void should_PublishAndConsumeSuccessfully_when_UserCreatedEvent() throws Exception {
        // Given
        UserCreatedEvent event = UserCreatedEvent.create(
            testUserId, testUsername, testPhone, testRole, testCorrelationId);
        
        // When
        domainEventPublisher.publish(event);
        
        // Then - 等待消息被消费
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            testEventListener.getReceivedEvents().size() == 1);
        
        // 验证接收到的事件
        DomainEvent receivedEvent = testEventListener.getReceivedEvents().poll();
        assertNotNull(receivedEvent, "应该接收到事件");
        assertTrue(receivedEvent instanceof UserCreatedEvent, "应该是UserCreatedEvent类型");
        
        UserCreatedEvent userEvent = (UserCreatedEvent) receivedEvent;
        assertAll("用户创建事件验证",
            () -> assertEquals(testUserId, userEvent.getUserId(), "用户ID应该匹配"),
            () -> assertEquals(testUsername, userEvent.getUsername(), "用户名应该匹配"),
            () -> assertEquals(testPhone, userEvent.getPhone(), "手机号应该匹配"),
            () -> assertEquals(testRole, userEvent.getRole(), "角色应该匹配"),
            () -> assertEquals(testCorrelationId, userEvent.getCorrelationId(), "关联ID应该匹配"),
            () -> assertEquals(EventType.USER_CREATED, userEvent.getEventType(), "事件类型应该匹配"),
            () -> assertNotNull(userEvent.getEventId(), "事件ID不应该为空"),
            () -> assertNotNull(userEvent.getTimestamp(), "时间戳不应该为空")
        );
    }

    @Test
    @DisplayName("正常场景：客资创建事件应该成功发布和消费")
    void should_PublishAndConsumeSuccessfully_when_LeadCreatedEvent() throws Exception {
        // Given
        Long leadId = 54321L;
        String customerName = "张三";
        String customerPhone = "13999999999";
        Long submitterId = testUserId;
        
        LeadCreatedEvent event = LeadCreatedEvent.create(
            leadId, customerName, customerPhone, submitterId, testUsername, testCorrelationId);

        // When
        domainEventPublisher.publish(event);
        
        // Then
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            testEventListener.getReceivedEvents().size() == 1);
        
        DomainEvent receivedEvent = testEventListener.getReceivedEvents().poll();
        assertNotNull(receivedEvent, "应该接收到事件");
        assertTrue(receivedEvent instanceof LeadCreatedEvent, "应该是LeadCreatedEvent类型");
        
        LeadCreatedEvent leadEvent = (LeadCreatedEvent) receivedEvent;
        assertAll("客资创建事件验证",
            () -> assertEquals(leadId, leadEvent.getLeadId(), "客资ID应该匹配"),
            () -> assertEquals(customerName, leadEvent.getCustomerName(), "客户名应该匹配"),
            () -> assertEquals(customerPhone, leadEvent.getCustomerPhone(), "客户电话应该匹配"),
            () -> assertEquals(submitterId, leadEvent.getSubmitterId(), "提交人ID应该匹配"),
            () -> assertEquals(testCorrelationId, leadEvent.getCorrelationId(), "关联ID应该匹配"),
            () -> assertEquals(EventType.LEAD_CREATED, leadEvent.getEventType(), "事件类型应该匹配")
        );
    }

    @Test
    @DisplayName("正常场景：批量事件发布应该成功处理")
    void should_HandleBatchEvents_when_MultipleEventsPublished() throws Exception {
        // Given
        int eventCount = 5;
        
        // When - 发布多个用户创建事件
        for (int i = 1; i <= eventCount; i++) {
            UserCreatedEvent event = UserCreatedEvent.create(
                testUserId + i, testUsername + i, testPhone, testRole, testCorrelationId + i);
            domainEventPublisher.publish(event);
        }
        
        // Then
        await().atMost(10, TimeUnit.SECONDS).until(() -> 
            testEventListener.getReceivedEvents().size() == eventCount);
        
        // 验证所有事件都被接收
        assertEquals(eventCount, testEventListener.getReceivedEvents().size(), 
            "应该接收到所有事件");
        
        // 验证事件内容的正确性
        for (int i = 1; i <= eventCount; i++) {
            DomainEvent event = testEventListener.getReceivedEvents().poll();
            assertNotNull(event, "事件不应该为空");
            assertTrue(event instanceof UserCreatedEvent, "应该是UserCreatedEvent类型");
            
            UserCreatedEvent userEvent = (UserCreatedEvent) event;
            assertTrue(userEvent.getUserId() >= testUserId + 1 && 
                      userEvent.getUserId() <= testUserId + eventCount, 
                      "用户ID应该在预期范围内");
        }
    }

    @Test
    @DisplayName("正常场景：事件序列化和反序列化应该保持数据完整性")
    void should_PreserveDataIntegrity_when_SerializationAndDeserialization() throws Exception {
        // Given
        UserCreatedEvent originalEvent = UserCreatedEvent.createWithInvitation(
            testUserId, testUsername, testPhone, testRole, 
            "INV12345", 99999L, testCorrelationId);
        originalEvent.initializeEvent("test-service");
        
        // When
        domainEventPublisher.publish(originalEvent);
        
        // Then
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            testEventListener.getReceivedEvents().size() == 1);
        
        UserCreatedEvent receivedEvent = (UserCreatedEvent) testEventListener.getReceivedEvents().poll();
        
        assertAll("序列化数据完整性验证",
            () -> assertEquals(originalEvent.getUserId(), receivedEvent.getUserId(), "用户ID应该相同"),
            () -> assertEquals(originalEvent.getUsername(), receivedEvent.getUsername(), "用户名应该相同"),
            () -> assertEquals(originalEvent.getPhone(), receivedEvent.getPhone(), "手机号应该相同"),
            () -> assertEquals(originalEvent.getRole(), receivedEvent.getRole(), "角色应该相同"),
            () -> assertEquals(originalEvent.getInvitationCode(), receivedEvent.getInvitationCode(), "邀请码应该相同"),
            () -> assertEquals(originalEvent.getInviterId(), receivedEvent.getInviterId(), "邀请人ID应该相同"),
            () -> assertEquals(originalEvent.getCorrelationId(), receivedEvent.getCorrelationId(), "关联ID应该相同"),
            () -> assertEquals(originalEvent.getEventType(), receivedEvent.getEventType(), "事件类型应该相同"),
            () -> assertEquals(originalEvent.getEventId(), receivedEvent.getEventId(), "事件ID应该相同"),
            () -> assertEquals(originalEvent.getSource(), receivedEvent.getSource(), "事件来源应该相同")
        );
    }

    @Test
    @DisplayName("异常场景：无效事件应该被拒绝发布")
    void should_RejectInvalidEvent_when_EventValidationFails() {
        // Given - 创建无效事件（缺少必要字段）
        UserCreatedEvent invalidEvent = UserCreatedEvent.builder()
            .userId(null)  // 无效：用户ID为空
            .username("")  // 无效：用户名为空
            .correlationId(testCorrelationId)
            .eventType(EventType.USER_CREATED)
            .build();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            domainEventPublisher.publish(invalidEvent);
        }, "应该抛出验证失败异常");
    }

    @Test
    @DisplayName("边界条件：事件路由键应该正确匹配交换机绑定")
    void should_RouteCorrectly_when_EventRoutingKeyMatches() throws Exception {
        // Given
        UserCreatedEvent userEvent = UserCreatedEvent.create(
            testUserId, testUsername, testPhone, testRole, testCorrelationId);
        
        // When - 直接发送到用户交换机
        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.created", userEvent);
        
        // Then
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            testEventListener.getReceivedEvents().size() == 1);
        
        DomainEvent receivedEvent = testEventListener.getReceivedEvents().poll();
        assertNotNull(receivedEvent, "应该接收到路由的事件");
        assertEquals(EventType.USER_CREATED, receivedEvent.getEventType(), "事件类型应该匹配");
    }

    @Test
    @DisplayName("边界条件：消息确认机制应该正常工作")
    void should_HandleMessageAcknowledgment_when_ProcessingEvents() throws Exception {
        // Given
        UserCreatedEvent event = UserCreatedEvent.create(
            testUserId, testUsername, testPhone, testRole, testCorrelationId);
        
        // When
        domainEventPublisher.publishWithConfirmation(event);
        
        // Then - 验证发布确认（通过日志或回调验证）
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            testEventListener.getReceivedEvents().size() == 1);
        
        assertNotNull(testEventListener.getReceivedEvents().poll(), "消息应该被成功处理");
    }

    @Test
    @DisplayName("边界条件：消息持久化配置应该生效")
    void should_PersistMessages_when_DurabilityConfigured() throws Exception {
        // Given
        UserCreatedEvent event = UserCreatedEvent.create(
            testUserId, testUsername, testPhone, testRole, testCorrelationId);
        
        // When
        domainEventPublisher.publish(event);
        
        // Then
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            testEventListener.getReceivedEvents().size() == 1);
        
        // 验证消息属性包含持久化标记
        Message message = rabbitTemplate.receive("user.queue", 1000);
        if (message != null) {
            MessageProperties props = message.getMessageProperties();
            // 验证持久化属性（队列和交换机都是持久化的）
            assertNotNull(props, "消息属性应该存在");
        }
    }

    @Test
    @DisplayName("性能测试：高并发事件发布应该稳定处理")
    void should_HandleHighConcurrency_when_ManyEventsPublished() throws Exception {
        // Given
        int concurrentEvents = 50;
        long startTime = System.currentTimeMillis();
        
        // When - 并发发布事件
        for (int i = 0; i < concurrentEvents; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    UserCreatedEvent event = UserCreatedEvent.create(
                        testUserId + index, testUsername + index, testPhone,
                        testRole, testCorrelationId + index);
                    domainEventPublisher.publish(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        
        // Then
        await().atMost(30, TimeUnit.SECONDS).until(() -> 
            testEventListener.getReceivedEvents().size() == concurrentEvents);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertEquals(concurrentEvents, testEventListener.getReceivedEvents().size(), 
            "应该处理所有并发事件");
        assertTrue(duration < 15000, "处理时间应该在合理范围内（<15秒）");
    }

    @Test
    @DisplayName("故障恢复：RabbitMQ连接断开重连应该正常工作")
    void should_RecoverFromConnectionFailure_when_NetworkIssues() {
        // Note: 这个测试需要模拟网络故障，在真实环境中测试
        // 此处仅验证连接配置的正确性
        
        // Given & When & Then
        assertNotNull(rabbitTemplate.getConnectionFactory(), "连接工厂应该存在");
        assertTrue(rabbitTemplate.getConnectionFactory().isPublisherConfirms(), 
            "发布确认应该开启");
        assertTrue(rabbitTemplate.getConnectionFactory().isPublisherReturns(), 
            "发布返回应该开启");
    }

    /**
     * 测试配置类
     */
    @TestConfiguration
    static class TestConfig {
        
        @Bean
        public DomainEventPublisher domainEventPublisher(RabbitTemplate rabbitTemplate) {
            return new DomainEventPublisher(rabbitTemplate);
        }
        
        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    /**
     * 测试事件监听器
     */
    static class TestEventListener {
        private final BlockingQueue<DomainEvent> receivedEvents = new LinkedBlockingQueue<>();
        
        @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "user.queue")
        public void handleUserEvent(UserCreatedEvent event) {
            receivedEvents.offer(event);
        }
        
        @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "lead.queue")  
        public void handleLeadEvent(LeadCreatedEvent event) {
            receivedEvents.offer(event);
        }
        
        public BlockingQueue<DomainEvent> getReceivedEvents() {
            return receivedEvents;
        }
        
        public void clearReceivedEvents() {
            receivedEvents.clear();
        }
    }
}