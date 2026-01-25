package com.deadlock.hellocs.domain.interest.repository;

import com.deadlock.hellocs.domain.interest.entity.InterestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestRepository extends JpaRepository<InterestEntity, Long> {
}
