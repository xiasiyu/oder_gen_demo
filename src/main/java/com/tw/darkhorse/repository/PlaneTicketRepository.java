package com.tw.darkhorse.repository;

import com.tw.darkhorse.entity.PlaneTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaneTicketRepository extends JpaRepository<PlaneTicketEntity, Long> {
    List<PlaneTicketEntity> findByPassengerId(Long passengerId);
}
