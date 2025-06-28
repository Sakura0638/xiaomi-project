package com.xiaomiproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users") // 建议显式指定表名
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String roles = "ROLE_USER";

    @Column(name = "registration_time", nullable = false, updatable = false)
    private LocalDateTime registrationTime;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
}
