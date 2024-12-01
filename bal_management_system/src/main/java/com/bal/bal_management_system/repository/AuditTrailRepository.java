package com.bal.bal_management_system.repository;

import com.bal.bal_management_system.model.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    // You can add custom query methods here if needed
}
