package com.paymentprocessor.clearingservice.repository;

import com.paymentprocessor.clearingservice.domain.OutboxEvent;
import com.paymentprocessor.clearingservice.domain.enums.OutboxStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
