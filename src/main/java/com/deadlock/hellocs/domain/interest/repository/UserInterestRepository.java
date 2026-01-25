package com.deadlock.hellocs.domain.interest.repository;

import com.deadlock.hellocs.domain.interest.entity.UserInterest;
import com.deadlock.hellocs.domain.interest.entity.UserInterestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.deadlock.hellocs.domain.interest.entity.Interest;
import java.util.List;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, UserInterestId> {

    @Query("select i.interest from UserInterest ui join ui.interest i where ui.user.id = :userId")
    List<Interest> findInterestNamesByUserId(@Param("userId") Long userId);
}
