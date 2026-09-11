package com.commence.novel.repository;


import com.commence.novel.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUname(String uname); //使用uname查找用户
    User findByEmail(String email);

    boolean existsByPenName(String penName);

    @Query("SELECT u FROM User u WHERE u.penName LIKE %:penName%")
    List<User> findByPenNameLike(@Param("penName") String penName);

    @Query("SELECT COUNT(u) FROM User u")
    Long countTotalUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(String role);
}
