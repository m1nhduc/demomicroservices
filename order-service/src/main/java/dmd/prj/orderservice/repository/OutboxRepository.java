package dmd.prj.orderservice.repository;

import dmd.prj.orderservice.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, String> {
    @Query(value = "SELECT * FROM outbox WHERE status = 'PENDING' ORDER BY created_at ASC FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<Outbox> findPendingForUpdate();
}
