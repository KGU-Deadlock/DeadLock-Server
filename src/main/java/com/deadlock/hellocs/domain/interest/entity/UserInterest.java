package com.deadlock.hellocs.domain.interest.entity;

import com.deadlock.hellocs.domain.user.entity.User;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_interest")
public class UserInterest {

    @EmbeddedId
    private UserInterestId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("interestId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", nullable = false)
    private InterestEntity interest;
}
