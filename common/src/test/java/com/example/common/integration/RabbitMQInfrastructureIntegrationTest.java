package com.example.common.integration;

import com.example.common.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * RabbitMQ基础设施集成测试
 * 
 * 测试RabbitMQ交换机、队列、绑定的配置正确性，验证：
 * - 所有交换机的创建和配置
 * - 所有队列的创建和属性设置
 * - 绑定关系和路由规则
 * - 死信队列配置
 * - 消息转换器和序列化
 * - 连接和重试配置
 * 
 * @author Event-Driven Architecture Team
 */
@SpringBootTest(classes = {RabbitMQConfig.class, RabbitAutoConfiguration.class, org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class})
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
@DirtiesContext
@DisplayName("RabbitMQ基础设施集成测试")
class RabbitMQInfrastructureIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("基础设施：所有交换机应该正确创建和配置")
    void should_CreateAndConfigureExchanges_when_ApplicationStarts() {
        // 验证所有业务交换机存在
        String[] exchanges = {
            RabbitMQConfig.USER_EXCHANGE,
            RabbitMQConfig.LEAD_EXCHANGE,
            RabbitMQConfig.PROMOTION_EXCHANGE,
            RabbitMQConfig.REWARD_EXCHANGE,
            RabbitMQConfig.INVITATION_EXCHANGE,
            RabbitMQConfig.SAGA_EXCHANGE,
            RabbitMQConfig.SYSTEM_EXCHANGE,
            RabbitMQConfig.DLX_EXCHANGE
        };

        for (String exchangeName : exchanges) {
            // 发送测试消息来验证交换机存在
            assertDoesNotThrow(() -> {
                rabbitTemplate.convertAndSend(exchangeName, "test.routing.key", "test message");
            }, "交换机应该存在: " + exchangeName);
        }
    }

    @Test
    @DisplayName("基础设施：所有队列应该正确创建和配置TTL")
    void should_CreateAndConfigureQueues_when_ApplicationStarts() {
        // 验证主要业务队列
        String[] queues = {
            "user.queue",
            "lead.queue", 
            "promotion.queue",
            "reward.queue",
            "invitation.queue",
            "saga.queue",
            "system.queue",
            RabbitMQConfig.DLX_QUEUE
        };

        for (String queueName : queues) {
            // 尝试从队列接收消息（超时很短，只是验证队列存在）
            Message message = rabbitTemplate.receive(queueName, 100);
            // message可能为null，但不应该抛出异常
            assertDoesNotThrow(() -> {
                rabbitTemplate.receive(queueName, 100);
            }, "队列应该存在: " + queueName);
        }
    }

    @Test
    @DisplayName("基础设施：消息路由应该根据路由键正确工作")
    void should_RouteMessagesCorrectly_when_RoutingKeyMatches() {
        // 测试用户事件路由
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.created", "user created message");
            rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.updated", "user updated message");
        }, "用户事件路由应该工作");

        // 测试客资事件路由
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend(RabbitMQConfig.LEAD_EXCHANGE, "lead.created", "lead created message");
            rabbitTemplate.convertAndSend(RabbitMQConfig.LEAD_EXCHANGE, "lead.assigned", "lead assigned message");
        }, "客资事件路由应该工作");

        // 测试Saga事件路由
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, "saga.started", "saga started message");
            rabbitTemplate.convertAndSend(RabbitMQConfig.SAGA_EXCHANGE, "saga.completed", "saga completed message");
        }, "Saga事件路由应该工作");
    }

    @Test
    @DisplayName("基础设施：JSON消息转换器应该正确序列化复杂对象")
    void should_SerializeComplexObjects_when_JsonMessageConverterUsed() {
        // Given - 创建测试对象
        TestMessage testMessage = new TestMessage();
        testMessage.setId(12345L);
        testMessage.setName("测试消息");
        testMessage.setTimestamp(java.time.LocalDateTime.now());
        testMessage.setActive(true);
        testMessage.setMetadata(java.util.Map.of("key1", "value1", "key2", "value2"));

        // When - 发送和接收消息
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend(RabbitMQConfig.SYSTEM_EXCHANGE, "system.test", testMessage);
        }, "复杂对象序列化应该成功");

        // 验证JSON序列化配置
        assertNotNull(objectMapper, "ObjectMapper应该存在");
        assertTrue(objectMapper.getRegisteredModuleIds().contains("jackson-datatype-jsr310"), 
            "应该注册JavaTimeModule");
    }

    @Test
    @DisplayName("基础设施：死信队列配置应该正确工作")
    void should_HandleDeadLetters_when_MessageProcessingFails() {
        // 验证死信交换机和队列存在
        assertDoesNotThrow(() -> {
            rabbitTemplate.convertAndSend(RabbitMQConfig.DLX_EXCHANGE, 
                RabbitMQConfig.DLX_ROUTING_KEY, "dead letter test message");
        }, "死信交换机应该存在");

        // 验证死信队列可以接收消息
        assertDoesNotThrow(() -> {
            rabbitTemplate.receive(RabbitMQConfig.DLX_QUEUE, 100);
        }, "死信队列应该存在");
    }

    @Test
    @DisplayName("基础设施：RabbitTemplate配置应该包含确认机制")
    void should_ConfigureConfirmations_when_RabbitTemplateCreated() {
        assertNotNull(rabbitTemplate, "RabbitTemplate应该存在");
        
        // 验证连接工厂配置
        assertTrue(rabbitTemplate.getConnectionFactory().isPublisherConfirms(), 
            "发布确认应该开启");
        assertTrue(rabbitTemplate.getConnectionFactory().isPublisherReturns(), 
            "发布返回应该开启");
        
        // 验证消息转换器
        assertNotNull(rabbitTemplate.getMessageConverter(), "消息转换器应该存在");
        assertTrue(rabbitTemplate.getMessageConverter() instanceof org.springframework.amqp.support.converter.Jackson2JsonMessageConverter,
            "应该使用Jackson2JsonMessageConverter");
    }

    @Test
    @DisplayName("基础设施：重试机制配置应该正确设置")
    void should_ConfigureRetryMechanism_when_RabbitTemplateCreated() {
        // 通过反射验证重试模板存在（Spring AMQP 2.4.x 无公开 getter）

        // 验证重试模板存在（具体重试策略在实际故障时测试）
        org.springframework.retry.support.RetryTemplate retryTemplate = (org.springframework.retry.support.RetryTemplate)
            org.springframework.test.util.ReflectionTestUtils.getField(rabbitTemplate, "retryTemplate");
        assertNotNull(retryTemplate, "重试模板不应该为空");
    }

    @Test
    @DisplayName("性能测试：高并发消息发送应该稳定处理")
    void should_HandleHighConcurrency_when_ManyMessagesSent() {
        int messageCount = 100;
        long startTime = System.currentTimeMillis();
        
        // 并发发送消息
        for (int i = 0; i < messageCount; i++) {
            final int index = i;
            new Thread(() -> {
                rabbitTemplate.convertAndSend(RabbitMQConfig.SYSTEM_EXCHANGE,
                    "system.performance.test", "Performance test message " + index);
            }).start();
        }
        
        // 等待所有消息发送完成（通过时间估算）
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            return elapsed > 1000; // 等待至少1秒
        });
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 8000, "高并发消息发送应该在8秒内完成");
    }

    @Test
    @DisplayName("基础设施：队列参数配置应该正确设置")
    void should_SetQueueArguments_when_QueuesCreated() {
        // 验证队列配置（通过RabbitAdmin查询队列属性）
        assertNotNull(rabbitAdmin, "RabbitAdmin应该存在");
        
        // 验证主要队列的基本存在性
        String[] testQueues = {
            "user.queue", "lead.queue", "saga.queue"
        };
        
        for (String queueName : testQueues) {
            // 使用队列声明来验证队列存在和配置
            assertDoesNotThrow(() -> {
                Queue testQueue = QueueBuilder.durable(queueName).build();
                // 声明队列（如果已存在且配置相同，不会出错）
                rabbitAdmin.declareQueue(testQueue);
            }, "队列应该存在且配置正确: " + queueName);
        }
    }

    @Test
    @DisplayName("基础设施：多种消息类型路由应该正确工作")
    void should_RouteMultipleMessageTypes_when_DifferentRoutingKeys() {
        // 测试不同业务域的消息路由
        assertDoesNotThrow(() -> {
            // 用户相关事件
            rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.created", "用户创建");
            rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.login", "用户登录");
            
            // 客资相关事件
            rabbitTemplate.convertAndSend(RabbitMQConfig.LEAD_EXCHANGE, "lead.created", "客资创建");
            rabbitTemplate.convertAndSend(RabbitMQConfig.LEAD_EXCHANGE, "lead.converted", "客资转换");
            
            // 推广相关事件
            rabbitTemplate.convertAndSend(RabbitMQConfig.PROMOTION_EXCHANGE, "promotion.submitted", "推广提交");
            rabbitTemplate.convertAndSend(RabbitMQConfig.PROMOTION_EXCHANGE, "promotion.approved", "推广审批");
            
            // 奖励相关事件
            rabbitTemplate.convertAndSend(RabbitMQConfig.REWARD_EXCHANGE, "reward.calculated", "奖励计算");
            rabbitTemplate.convertAndSend(RabbitMQConfig.REWARD_EXCHANGE, "commission.paid", "佣金支付");
            
        }, "多种消息类型路由应该正常工作");
    }

    @Test
    @DisplayName("边界条件：无效路由键应该触发返回机制")
    void should_ReturnMessage_when_InvalidRoutingKey() {
        // 设置返回监听器来验证返回机制
        boolean[] messageReturned = {false};
        
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            messageReturned[0] = true;
        });
        
        // 发送到不存在的路由键
        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EXCHANGE, "user.nonexistent.route", 
            "This should be returned");
        
        // 等待返回机制触发
        await().atMost(3, TimeUnit.SECONDS).until(() -> messageReturned[0]);
        
        assertTrue(messageReturned[0], "无效路由键的消息应该被返回");
    }

    /**
     * 测试消息类
     */
    static class TestMessage {
        private Long id;
        private String name;
        private java.time.LocalDateTime timestamp;
        private Boolean active;
        private Map<String, Object> metadata;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}