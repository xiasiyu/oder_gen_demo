package com.tw.darkhorse.repository;

import com.tw.darkhorse.entity.PlaneTicketEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaneTicketEventRepository extends JpaRepository<PlaneTicketEventEntity, Long> {
}
