package com.ims.repository.mongo;

import com.ims.model.mongo.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);
}
