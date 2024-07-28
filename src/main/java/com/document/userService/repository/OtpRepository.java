package com.document.userService.repository;

import com.document.userService.entity.user.OtpEntity;
import com.document.userService.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface OtpRepository extends JpaRepository<OtpEntity, UUID> {
    @Query(nativeQuery = true, value = "select * from otp where user_id = ?1")
    OtpEntity findByUserId(UUID userId);

    OtpEntity findByUser(User user);
}
