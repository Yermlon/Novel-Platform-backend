package com.commence.novel.repository;

import com.commence.novel.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Integer> {

    Optional<VerificationCode> findTopByEmailOrderByCreateTimeDesc(String email);

    @Modifying
    @Query(value = "DELETE FROM verification_code WHERE email = :email", nativeQuery = true)
    void deleteByEmail(@Param("email") String email);

}

