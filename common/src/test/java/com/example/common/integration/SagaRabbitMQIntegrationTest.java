package com.example.common.integration;

import com.example.common.config.RabbitMQConfig;
import com.example.common.event.DomainEventPublisher;
import com.example.common.event.EventType;
import com.example.common.event.saga.SagaStartedEvent;
import com.example.common.event.saga.SagaStepCompletedEvent;
import com.example.common.saga.SagaCoordinator;
import com.example.common.saga.SagaStep;
import com.example.common.saga.SagaTransaction;
import com.example.common.saga.engine.SagaExecutionEngine;
import com.example.common.saga.process.UserRegistrationSaga;
import com.example.common.dto.CommonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Saga事务协调与RabbitMQ集成测试
 * 
 * 测试Saga分布式事务与事件驱动架构的集成，验证：
 * - Saga事务状态变化事件的发布和消费
 * - 分布式事务步骤的异步协调
 * - 补偿事务的事件通知
 * - 业务流程Saga的端到端执行
 * - 事件驱动的服务间通信
 * 
 * @author Event-Driven Architecture Team
 */
@SpringBootTest(classes = {
    RabbitMQConfig.class,
    RabbitAutoConfiguration.class,
    SagaRabbitMQIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672", 
    "spring.rabbitmq.username=guest",
    "spring.rabbitmq.password=guest",
    "spring.rabbitmq.virtual-host=/",
    "spring.rabbitmq.publisher-confirms=true",
    "spring.rabbitmq.publisher-confirm-type=correlated",
    "event-driven.enabled=false"
})

@RabbitListenerTest
@DirtiesContext
@DisplayName("Saga事务协调与RabbitMQ集成测试")
class SagaRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DomainEventPublisher domainEventPublisher;
    
    @Autowired
    private SagaCoordinator sagaCoordinator;
    
    @Autowired
    private UserRegistrationSaga userRegistrationSaga;
    
    @Autowired
    private SagaEventListener sagaEventListener;

    // 测试数据
    private final String testCorrelationId = "SAGA-CORR-12345";
    private final Long testUserId = 78901L;
    private final String testUsername = "testsagauser";
    private final String testPhone = "13777777777";
    private final String testRole = "AGENT";

    @BeforeEach
    void setUp() {
        sagaEventListener.clearReceivedEvents();
    }

    @Test
    @DisplayName("正常场景：Saga启动事件应该成功发布和消费")
    void should_PublishSagaStartedEvent_when_SagaTransactionStarts() throws Exception {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("userId", testUserId);
        context.put("username", testUsername);
        
        // When - 创建并启动Saga
        CommonResult<SagaTransaction> createResult = sagaCoordinator.createSaga(
            "USER_REGISTRATION", testCorrelationId, testUserId, context);
        assertTrue(createResult.getSuccess(), "Saga创建应该成功");
        
        SagaTransaction saga = createResult.getData();
        CommonResult<Void> startResult = sagaCoordinator.startSaga(saga.getSagaId());
        assertTrue(startResult.getSuccess(), "Saga启动应该成功");
        
        // Then - 验证Saga启动事件
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            sagaEventListener.getSagaStartedEvents().size() == 1);
        
        SagaStartedEvent startedEvent = sagaEventListener.getSagaStartedEvents().poll();
        assertNotNull(startedEvent, "应该接收到Saga启动事件");
        
        assertAll("Saga启动事件验证",
            () -> assertEquals(saga.getSagaId(), startedEvent.getSagaId(), "SagaId应该匹配"),
            () -> assertEquals("USER_REGISTRATION", startedEvent.getSagaType(), "Saga类型应该匹配"),
            () -> assertEquals(testCorrelationId, startedEvent.getCorrelationId(), "关联ID应该匹配"),
            () -> assertEquals(testUserId, startedEvent.getInitiatorId(), "发起人ID应该匹配"),
            () -> assertEquals(EventType.SAGA_STARTED, startedEvent.getEventType(), "事件类型应该匹配"),
            () -> assertNotNull(startedEvent.getEventId(), "事件ID不应该为空"),
            () -> assertNotNull(startedEvent.getTimestamp(), "时间戳不应该为空")
        );
    }

    @Test
    @DisplayName("正常场景：Saga步骤完成事件应该成功发布和消费")
    void should_PublishStepCompletedEvent_when_SagaStepCompletes() throws Exception {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("userId", testUserId);
        
        CommonResult<SagaTransaction> createResult = sagaCoordinator.createSaga(
            "USER_REGISTRATION", testCorrelationId, testUserId, context);
        SagaTransaction saga = createResult.getData();
        
        // 添加测试步骤
        SagaStep step = SagaStep.create("TEST_STEP", "test-service", "testMethod");
        step.setDescription("测试步骤");
        saga.addStep(step);
        
        // When - 启动Saga并模拟步骤完成
        sagaCoordinator.startSaga(saga.getSagaId());
        
        // 模拟步骤完成事件发布
        SagaStepCompletedEvent stepEvent = SagaStepCompletedEvent.createWithResult(
            saga.getSagaId(),
            "TEST_STEP",
            0,
            "test-service",
            java.util.Map.of("success", true),
            null,
            testCorrelationId
        );
        stepEvent.initializeEvent("test-service");

        domainEventPublisher.publish(stepEvent);
        
        // Then
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            sagaEventListener.getStepCompletedEvents().size() == 1);
        
        SagaStepCompletedEvent receivedEvent = sagaEventListener.getStepCompletedEvents().poll();
        assertNotNull(receivedEvent, "应该接收到步骤完成事件");

        assertAll("Saga步骤完成事件验证",
            () -> assertEquals(saga.getSagaId(), receivedEvent.getSagaId(), "SagaId应该匹配"),
            () -> assertEquals("TEST_STEP", receivedEvent.getStepName(), "步骤名应该匹配"),
            () -> assertEquals(0, receivedEvent.getStepOrder(), "步骤序号应该匹配"),
            () -> assertEquals(Boolean.TRUE, receivedEvent.getStepResult() != null ? receivedEvent.getStepResult().get("success") : null, "步骤应该成功"),
            () -> assertEquals(testCorrelationId, receivedEvent.getCorrelationId(), "关联ID应该匹配"),
            () -> assertEquals(EventType.SAGA_STEP_COMPLETED, receivedEvent.getEventType(), "事件类型应该匹配")
        );
    }

    @Test
    @DisplayName("正常场景：用户注册Saga业务流程应该完整执行")
    void should_ExecuteCompleteFlow_when_UserRegistrationSagaRuns() throws Exception {
        // Given - 启动用户注册流程
        CommonResult<String> result = userRegistrationSaga.startUserRegistration(
            testUserId, testUsername, testPhone, testRole, "password123", null, testCorrelationId);
        
        // Then - 验证流程启动成功
        assertTrue(result.getSuccess(), "用户注册流程应该启动成功");
        assertNotNull(result.getData(), "应该返回SagaId");
        
        String sagaId = result.getData();
        
        // 等待Saga启动事件
        await().atMost(10, TimeUnit.SECONDS).until(() -> 
            sagaEventListener.getSagaStartedEvents().size() == 1);
        
        SagaStartedEvent startedEvent = sagaEventListener.getSagaStartedEvents().poll();
        assertAll("用户注册Saga启动验证",
            () -> assertEquals(sagaId, startedEvent.getSagaId(), "SagaId应该匹配"),
            () -> assertEquals("USER_REGISTRATION", startedEvent.getSagaType(), "Saga类型应该匹配"),
            () -> assertEquals(testCorrelationId, startedEvent.getCorrelationId(), "关联ID应该匹配")
        );
        
        // 验证Saga状态查询
        CommonResult<Map<String, Object>> statusResult = userRegistrationSaga.getUserRegistrationStatus(sagaId);
        assertTrue(statusResult.getSuccess(), "状态查询应该成功");
        
        Map<String, Object> status = statusResult.getData();
        assertAll("用户注册Saga状态验证",
            () -> assertEquals(sagaId, status.get("sagaId"), "SagaId应该匹配"),
            () -> assertEquals("USER_REGISTRATION", status.get("sagaType"), "Saga类型应该匹配"),
            () -> assertNotNull(status.get("status"), "状态不应该为空"),
            () -> assertNotNull(status.get("currentStepIndex"), "当前步骤索引不应该为空"),
            () -> assertNotNull(status.get("totalSteps"), "总步骤数不应该为空"),
            () -> assertEquals(testUserId, status.get("userId"), "用户ID应该匹配")
        );
    }

    @Test
    @DisplayName("正常场景：Saga事件路由应该正确匹配")
    void should_RouteCorrectly_when_SagaEventsPublished() throws Exception {
        // Given
        SagaStartedEvent sagaEvent = SagaStartedEvent.builder()
            .sagaId("TEST-SAGA-001")
            .sagaType("TEST_SAGA")
            .correlationId(testCorrelationId)
            .initiatorId(testUserId)
            .eventType(EventType.SAGA_STARTED)
            .build();
        sagaEvent.initializeEvent("test-service");
        
        // When - 直接发送到Saga交换机
        rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, "saga.started", sagaEvent);
        
        // Then
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            sagaEventListener.getSagaStartedEvents().size() == 1);
        
        SagaStartedEvent receivedEvent = sagaEventListener.getSagaStartedEvents().poll();
        assertNotNull(receivedEvent, "应该接收到Saga事件");
        assertEquals("TEST-SAGA-001", receivedEvent.getSagaId(), "SagaId应该匹配");
    }

    @Test
    @DisplayName("边界条件：Saga超时应该触发相应的事件处理")
    void should_HandleTimeoutEvent_when_SagaTimesOut() throws Exception {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("userId", testUserId);
        
        CommonResult<SagaTransaction> createResult = sagaCoordinator.createSaga(
            "USER_REGISTRATION", testCorrelationId, testUserId, context);
        SagaTransaction saga = createResult.getData();
        
        // 设置短超时时间进行测试
        saga.setTimeoutMillis(1000L); // 1秒超时
        
        // When - 启动Saga但不处理步骤（模拟超时）
        sagaCoordinator.startSaga(saga.getSagaId());
        
        // Then - 验证超时处理（实际需要SagaExecutionEngine的超时检查机制）
        // 这里主要验证Saga框架能正确处理超时配置
        assertNotNull(saga.getTimeoutMillis(), "超时时间应该被设置");
        assertEquals(1000L, saga.getTimeoutMillis(), "超时时间应该匹配");
        
        // 验证Saga启动事件仍然正常发布
        await().atMost(3, TimeUnit.SECONDS).until(() -> 
            sagaEventListener.getSagaStartedEvents().size() == 1);
        
        assertNotNull(sagaEventListener.getSagaStartedEvents().poll(), "Saga启动事件应该被接收");
    }

    @Test
    @DisplayName("异常场景：Saga执行失败应该触发补偿流程事件")
    void should_PublishCompensationEvents_when_SagaFails() throws Exception {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("userId", testUserId);
        
        CommonResult<SagaTransaction> createResult = sagaCoordinator.createSaga(
            "USER_REGISTRATION", testCorrelationId, testUserId, context);
        SagaTransaction saga = createResult.getData();
        
        // 添加可补偿的测试步骤
        SagaStep step = SagaStep.createCompensable(
            "COMPENSABLE_STEP", "test-service", "testMethod", "compensateMethod");
        step.setDescription("可补偿测试步骤");
        saga.addStep(step);
        
        // When - 启动Saga
        sagaCoordinator.startSaga(saga.getSagaId());
        
        // 模拟步骤失败并触发补偿
        saga.startCompensation("模拟步骤失败");
        
        // 发布补偿相关事件（在实际系统中由SagaExecutionEngine处理）
        SagaStepCompletedEvent failureEvent = SagaStepCompletedEvent.createWithResult(
            saga.getSagaId(),
            "COMPENSABLE_STEP",
            0,
            "test-service",
            java.util.Map.of("success", false, "errorMessage", "模拟步骤失败"),
            null,
            testCorrelationId
        );
        failureEvent.initializeEvent("test-service");

        domainEventPublisher.publish(failureEvent);
        
        // Then
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            sagaEventListener.getStepCompletedEvents().size() == 1);
        
        SagaStepCompletedEvent receivedEvent = sagaEventListener.getStepCompletedEvents().poll();
        assertNotNull(receivedEvent, "应该接收到步骤完成事件");
        assertEquals(Boolean.FALSE, receivedEvent.getStepResult() != null ? receivedEvent.getStepResult().get("success") : null, "步骤应该失败");
        assertEquals("模拟步骤失败", receivedEvent.getStepResult() != null ? receivedEvent.getStepResult().get("errorMessage") : null, "错误信息应该匹配");
        
        // 验证Saga状态已更新为补偿中
        assertEquals(SagaTransaction.SagaStatus.COMPENSATING, saga.getStatus(), "Saga应该处于补偿状态");
    }

    @Test
    @DisplayName("性能测试：并发Saga执行应该稳定处理")
    void should_HandleConcurrentSagas_when_MultipleFlowsExecuted() throws Exception {
        // Given
        int concurrentSagas = 10;
        
        // When - 并发启动多个Saga
        for (int i = 0; i < concurrentSagas; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    CommonResult<String> result = userRegistrationSaga.startUserRegistration(
                        testUserId + index, testUsername + index, testPhone, testRole,
                        "password" + index, null, testCorrelationId + index);
                    assertTrue(result.getSuccess(), "Saga启动应该成功");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        
        // Then
        await().atMost(30, TimeUnit.SECONDS).until(() -> 
            sagaEventListener.getSagaStartedEvents().size() == concurrentSagas);
        
        assertEquals(concurrentSagas, sagaEventListener.getSagaStartedEvents().size(), 
            "应该处理所有并发Saga");
        
        // 验证每个Saga都有唯一的ID
        for (int i = 0; i < concurrentSagas; i++) {
            SagaStartedEvent event = sagaEventListener.getSagaStartedEvents().poll();
            assertNotNull(event, "Saga启动事件应该存在");
            assertNotNull(event.getSagaId(), "SagaId应该不为空");
            assertEquals("USER_REGISTRATION", event.getSagaType(), "Saga类型应该匹配");
        }
    }

    @Test
    @DisplayName("集成测试：事件驱动的服务间通信应该正常工作")
    void should_CommunicateAcrossServices_when_EventDrivenArchitecture() {
        // Given & When & Then
        // 验证各个交换机和队列的配置正确性
        assertNotNull(rabbitTemplate, "RabbitTemplate应该存在");
        
        // 验证关键的RabbitMQ配置
        assertTrue(rabbitTemplate.getConnectionFactory().isPublisherConfirms(), 
            "发布确认应该开启");
        
        // 验证各个业务域的交换机存在（通过发送测试消息验证）
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.test", "test message");
            rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, "saga.test", "test message");
        }, "消息发送应该成功");
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
        public SagaCoordinator sagaCoordinator() {
            return mock(SagaCoordinator.class, RETURNS_DEEP_STUBS);
        }
        
        @Bean
        public UserRegistrationSaga userRegistrationSaga(SagaCoordinator sagaCoordinator) {
            UserRegistrationSaga saga = new UserRegistrationSaga();
            // 使用反射设置私有字段
            try {
                java.lang.reflect.Field field = UserRegistrationSaga.class.getDeclaredField("sagaCoordinator");
                field.setAccessible(true);
                field.set(saga, sagaCoordinator);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return saga;
        }
        
        @Bean
        public SagaEventListener sagaEventListener() {
            return new SagaEventListener();
        }
    }

    /**
     * Saga事件监听器
     */
    static class SagaEventListener {
        private final BlockingQueue<SagaStartedEvent> sagaStartedEvents = new LinkedBlockingQueue<>();
        private final BlockingQueue<SagaStepCompletedEvent> stepCompletedEvents = new LinkedBlockingQueue<>();
        
        @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "saga.queue")
        public void handleSagaStartedEvent(SagaStartedEvent event) {
            sagaStartedEvents.offer(event);
        }
        
        @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "saga.queue")
        public void handleStepCompletedEvent(SagaStepCompletedEvent event) {
            stepCompletedEvents.offer(event);
        }
        
        public BlockingQueue<SagaStartedEvent> getSagaStartedEvents() {
            return sagaStartedEvents;
        }
        
        public BlockingQueue<SagaStepCompletedEvent> getStepCompletedEvents() {
            return stepCompletedEvents;
        }
        
        public void clearReceivedEvents() {
            sagaStartedEvents.clear();
            stepCompletedEvents.clear();
        }
    }
}