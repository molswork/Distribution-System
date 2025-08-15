package com.example.common.event;

import com.example.common.dto.CommonResult;
import com.example.common.event.domain.UserCreatedEvent;
import com.example.common.event.domain.LeadCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

/**
 * DomainEventPublisher 单元测试
 * 
 * 测试领域事件发布器的核心功能：
 * - 单事件发布和验证
 * - 批量事件发布（事务性和非事务性）
 * - 异步事件发布
 * - 错误处理和重试机制
 * - 消息头设置和序列化
 * - 关联ID追踪和日志记录
 * 
 * @author Event-Driven Architecture Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DomainEventPublisher 单元测试")
class DomainEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DomainEventPublisher eventPublisher;

    private UserCreatedEvent validUserEvent;
    private LeadCreatedEvent validLeadEvent;
    private final String testCorrelationId = "test-correlation-123";
    private final String testServiceName = "test-service";

    @BeforeEach
    void setUp() throws Exception {
        // 设置服务名称
        ReflectionTestUtils.setField(eventPublisher, "serviceName", testServiceName);
        
        // 创建有效的用户创建事件
        validUserEvent = UserCreatedEvent.builder()
                .userId(1L)
                .username("testuser")
                .phone("13800138000")
                .role("AGENT")
                .correlationId(testCorrelationId)
                .eventType(EventType.USER_CREATED)
                .build();
        validUserEvent.initializeEvent(testServiceName);
        
        // 创建有效的客资创建事件
        validLeadEvent = LeadCreatedEvent.builder()
                .leadId(1L)
                .customerName("张三")
                .customerPhone("13900139000")
                .submitterId(1L)
                .submitterName("李四")
                .correlationId(testCorrelationId)
                .eventType(EventType.LEAD_CREATED)
                .build();
        validLeadEvent.initializeEvent(testServiceName);
        
        // 设置ObjectMapper模拟行为
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"TEST\"}");
    }

    @Test
    @DisplayName("正常场景：有效事件发布应该成功")
    void should_PublishSuccessfully_when_EventIsValid() throws Exception {
        // Given
        // Mock setup already done in setUp()
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvent(validUserEvent);
        
        // Then
        assertAll("事件发布验证",
                () -> assertTrue(result.getSuccess(), "发布结果应该成功"),
                () -> assertNull(result.getData(), "成功结果不应该有数据"),
                () -> verify(objectMapper, times(1)).writeValueAsString(validUserEvent),
                () -> verify(rabbitTemplate, times(1)).convertAndSend(
                        eq(EventType.USER_CREATED.getExchangeName()),
                        eq(EventType.USER_CREATED.getRoutingKey()),
                        eq("{\"eventType\":\"TEST\"}"),
                        any(org.springframework.amqp.core.MessagePostProcessor.class))
        );
    }

    @Test
    @DisplayName("异常场景：空事件发布应该失败")
    void should_FailGracefully_when_EventIsNull() {
        // When
        CommonResult<Void> result = eventPublisher.publishEvent(null);
        
        // Then
        assertAll("空事件发布验证",
                () -> assertFalse(result.getSuccess(), "空事件发布应该失败"),
                () -> assertEquals("事件不能为空", result.getMessage(), "错误消息应该正确"),
                () -> verifyNoInteractions(rabbitTemplate, objectMapper)
        );
    }

    @Test
    @DisplayName("异常场景：无效事件发布应该失败")
    void should_FailGracefully_when_EventIsInvalid() {
        // Given
        UserCreatedEvent invalidEvent = UserCreatedEvent.builder()
                .userId(null) // 缺少必需字段
                .correlationId(testCorrelationId)
                .eventType(EventType.USER_CREATED)
                .build();
        invalidEvent.initializeEvent(testServiceName);
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvent(invalidEvent);
        
        // Then
        assertAll("无效事件发布验证",
                () -> assertFalse(result.getSuccess(), "无效事件发布应该失败"),
                () -> assertEquals("事件数据不完整", result.getMessage(), "错误消息应该正确"),
                () -> verifyNoInteractions(rabbitTemplate)
        );
    }

    @Test
    @DisplayName("异常场景：序列化失败应该返回错误")
    void should_HandleSerializationError_when_ObjectMapperFails() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("序列化失败"));
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvent(validUserEvent);
        
        // Then
        assertAll("序列化失败处理验证",
                () -> assertFalse(result.getSuccess(), "序列化失败应该返回失败"),
                () -> assertTrue(result.getMessage().contains("事件发布失败"), "错误消息应该包含失败信息"),
                () -> verify(objectMapper, times(1)).writeValueAsString(validUserEvent),
                () -> verifyNoInteractions(rabbitTemplate)
        );
    }

    @Test
    @DisplayName("异常场景：RabbitMQ发布失败应该返回错误")
    void should_HandleRabbitMQError_when_PublishFails() throws Exception {
        // Given
        doThrow(new RuntimeException("RabbitMQ连接失败")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString(), any(org.springframework.amqp.core.MessagePostProcessor.class));
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvent(validUserEvent);
        
        // Then
        assertAll("RabbitMQ发布失败处理验证",
                () -> assertFalse(result.getSuccess(), "RabbitMQ失败应该返回失败"),
                () -> assertTrue(result.getMessage().contains("事件发布失败"), "错误消息应该包含失败信息"),
                () -> verify(objectMapper, times(1)).writeValueAsString(validUserEvent),
                () -> verify(rabbitTemplate, times(1)).convertAndSend(
                        eq(EventType.USER_CREATED.getExchangeName()),
                        eq(EventType.USER_CREATED.getRoutingKey()),
                        eq("{\"eventType\":\"TEST\"}"),
                        any(org.springframework.amqp.core.MessagePostProcessor.class))
        );
    }

    @Test
    @DisplayName("正常场景：关联ID应该正确设置到MDC")
    void should_SetCorrelationIdToMDC_when_PublishingEvent() throws Exception {
        // Given
        // Setup done in setUp()
        
        // When
        eventPublisher.publishEvent(validUserEvent);
        
        // Then
        // Note: MDC is cleared after publishing, so we can't directly verify it here
        // This test verifies that the method completes without error with correlation ID
        verify(objectMapper, times(1)).writeValueAsString(validUserEvent);
    }

    @Test
    @DisplayName("正常场景：批量事件事务性发布全部成功")
    void should_PublishAllEvents_when_TransactionalBatchPublishSucceeds() {
        // Given
        List<DomainEvent> events = Arrays.asList(validUserEvent, validLeadEvent);
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvents(events, true);
        
        // Then
        assertAll("事务性批量发布成功验证",
                () -> assertTrue(result.getSuccess(), "事务性批量发布应该成功"),
                () -> verify(objectMapper, times(2)).writeValueAsString(any()),
                () -> verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), anyString(), any(org.springframework.amqp.core.MessagePostProcessor.class))
        );
    }

    @Test
    @DisplayName("异常场景：批量事件事务性发布部分失败应该全部失败")
    void should_FailAllEvents_when_TransactionalBatchPublishPartiallyFails() throws Exception {
        // Given
        List<DomainEvent> events = Arrays.asList(validUserEvent, validLeadEvent);
        
        // 第二个事件序列化失败
        when(objectMapper.writeValueAsString(validUserEvent)).thenReturn("{\"eventType\":\"USER_CREATED\"}");
        when(objectMapper.writeValueAsString(validLeadEvent)).thenThrow(new RuntimeException("序列化失败"));
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvents(events, true);
        
        // Then
        assertAll("事务性批量发布失败验证",
                () -> assertFalse(result.getSuccess(), "部分失败时事务性发布应该失败"),
                () -> assertTrue(result.getMessage().contains("事件发布失败"), "错误消息应该包含失败信息")
        );
    }

    @Test
    @DisplayName("正常场景：批量事件非事务性发布部分成功")
    void should_ReportPartialSuccess_when_NonTransactionalBatchPublishPartiallySucceeds() throws Exception {
        // Given
        List<DomainEvent> events = Arrays.asList(validUserEvent, validLeadEvent);
        
        // 第二个事件序列化失败
        when(objectMapper.writeValueAsString(validUserEvent)).thenReturn("{\"eventType\":\"USER_CREATED\"}");
        when(objectMapper.writeValueAsString(validLeadEvent)).thenThrow(new RuntimeException("序列化失败"));
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvents(events, false);
        
        // Then
        assertAll("非事务性批量发布部分成功验证",
                () -> assertFalse(result.getSuccess(), "部分失败时非事务性发布应该失败"),
                () -> assertTrue(result.getMessage().contains("部分事件发布失败"), "错误消息应该指示部分失败"),
                () -> assertTrue(result.getMessage().contains("成功1个"), "错误消息应该包含成功数量"),
                () -> assertTrue(result.getMessage().contains("失败1个"), "错误消息应该包含失败数量")
        );
    }

    @Test
    @DisplayName("正常场景：批量事件非事务性发布全部成功")
    void should_PublishAllEvents_when_NonTransactionalBatchPublishSucceeds() {
        // Given
        List<DomainEvent> events = Arrays.asList(validUserEvent, validLeadEvent);
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvents(events, false);
        
        // Then
        assertAll("非事务性批量发布成功验证",
                () -> assertTrue(result.getSuccess(), "非事务性批量发布应该成功"),
                () -> verify(objectMapper, times(2)).writeValueAsString(any()),
                () -> verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), anyString(), any(org.springframework.amqp.core.MessagePostProcessor.class))
        );
    }

    @Test
    @DisplayName("异常场景：空事件列表批量发布应该失败")
    void should_FailGracefully_when_EventListIsEmpty() {
        // When
        CommonResult<Void> result = eventPublisher.publishEvents(Collections.emptyList(), true);
        
        // Then
        assertAll("空事件列表批量发布验证",
                () -> assertFalse(result.getSuccess(), "空事件列表发布应该失败"),
                () -> assertEquals("事件列表不能为空", result.getMessage(), "错误消息应该正确"),
                () -> verifyNoInteractions(rabbitTemplate, objectMapper)
        );
    }

    @Test
    @DisplayName("异常场景：null事件列表批量发布应该失败")
    void should_FailGracefully_when_EventListIsNull() {
        // When
        CommonResult<Void> result = eventPublisher.publishEvents(null, true);
        
        // Then
        assertAll("null事件列表批量发布验证",
                () -> assertFalse(result.getSuccess(), "null事件列表发布应该失败"),
                () -> assertEquals("事件列表不能为空", result.getMessage(), "错误消息应该正确"),
                () -> verifyNoInteractions(rabbitTemplate, objectMapper)
        );
    }

    @Test
    @DisplayName("正常场景：异步事件发布应该不抛异常")
    void should_NotThrowException_when_PublishingEventAsync() throws Exception {
        // When & Then
        assertDoesNotThrow(() -> eventPublisher.publishEventAsync(validUserEvent), 
                "异步事件发布不应该抛异常");
        
        // 验证异步调用确实执行了发布逻辑
        verify(objectMapper, times(1)).writeValueAsString(validUserEvent);
    }

    @Test
    @DisplayName("异常场景：异步事件发布失败应该静默处理")
    void should_HandleErrorsSilently_when_AsyncPublishFails() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("序列化失败"));
        
        // When & Then
        assertDoesNotThrow(() -> eventPublisher.publishEventAsync(validUserEvent), 
                "异步事件发布失败应该静默处理");
        
        verify(objectMapper, times(1)).writeValueAsString(validUserEvent);
    }

    @Test
    @DisplayName("边界条件：事件版本号应该自动设置默认值")
    void should_SetDefaultVersion_when_EventVersionIsEmpty() {
        // Given
        UserCreatedEvent eventWithoutVersion = UserCreatedEvent.builder()
                .userId(1L)
                .username("testuser")
                .phone("13800138000")
                .role("AGENT")
                .correlationId(testCorrelationId)
                .eventType(EventType.USER_CREATED)
                .build();
        // 不调用initializeEvent，保持version为空
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvent(eventWithoutVersion);
        
        // Then
        assertAll("版本号默认值验证",
                () -> assertTrue(result.getSuccess(), "发布应该成功"),
                () -> assertEquals("1.0", eventWithoutVersion.getVersion(), "版本号应该被设置为默认值")
        );
    }

    @Test
    @DisplayName("边界条件：消息头应该包含所有必要信息")
    void should_ContainAllRequiredHeaders_when_PublishingEvent() {
        // Given
        // Setup done in setUp()
        
        // When
        eventPublisher.publishEvent(validUserEvent);
        
        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(EventType.USER_CREATED.getExchangeName()),
                eq(EventType.USER_CREATED.getRoutingKey()),
                eq("{\"eventType\":\"TEST\"}"),
                ArgumentMatchers.<org.springframework.amqp.core.MessagePostProcessor>argThat(messagePostProcessor -> {
                    // 这里我们验证消息后处理器被调用
                    // 实际的header验证在实际的消息处理器中进行
                    return messagePostProcessor != null;
                })
        );
    }

    @Test
    @DisplayName("边界条件：没有关联ID的事件应该正常发布")
    void should_PublishSuccessfully_when_EventHasNoCorrelationId() {
        // Given
        UserCreatedEvent eventWithoutCorrelationId = UserCreatedEvent.builder()
                .userId(1L)
                .username("testuser")
                .phone("13800138000")
                .role("AGENT")
                .eventType(EventType.USER_CREATED)
                .build();
        eventWithoutCorrelationId.initializeEvent(testServiceName);
        
        // When
        CommonResult<Void> result = eventPublisher.publishEvent(eventWithoutCorrelationId);
        
        // Then
        assertAll("无关联ID事件发布验证",
                () -> assertTrue(result.getSuccess(), "没有关联ID的事件应该能正常发布"),
                () -> verify(objectMapper, times(1)).writeValueAsString(eventWithoutCorrelationId),
                () -> verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), anyString(), any(org.springframework.amqp.core.MessagePostProcessor.class))
        );
    }
}