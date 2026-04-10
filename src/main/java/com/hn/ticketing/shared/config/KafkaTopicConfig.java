package com.hn.ticketing.shared.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_RESERVATION_CONFIRMED = "reservation.confirmed";
    public static final String TOPIC_RESERVATION_CANCELLED = "reservation.cancelled";
    public static final String TOPIC_NOTIFICATION = "notification";

    @Bean
    public NewTopic reservationConfirmedTopic() {
        // // 결제 완료 및 좌석 확정
        return TopicBuilder.name(TOPIC_RESERVATION_CONFIRMED)
                .partitions(3) // 처리량 3
                .replicas(1) // 데이터 복제본 없이 원본만 둠, 실무에선 데이터를 살리기 위해 3 이상으로
                .build();
    }

    @Bean
    public NewTopic reservationCancelledTopic() {// 예매 취소
        return TopicBuilder.name(TOPIC_RESERVATION_CANCELLED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationTopic() { // 사용자에게 보낼 알림만 모음
        return TopicBuilder.name(TOPIC_NOTIFICATION)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
