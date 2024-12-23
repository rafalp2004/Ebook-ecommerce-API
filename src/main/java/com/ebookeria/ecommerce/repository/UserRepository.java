package com.ebookeria.ecommerce.repository;

import com.ebookeria.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}
