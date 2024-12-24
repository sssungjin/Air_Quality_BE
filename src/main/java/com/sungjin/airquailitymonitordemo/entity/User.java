package com.sungjin.airquailitymonitordemo.entity;

import com.sungjin.airquailitymonitordemo.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // PM 또는 PARTICIPANT

    @Column(name = "kibana_access_key", unique = true)
    private String kibanaAccessKey; // Kibana 접근 키 (자동 생성)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "pm", cascade = CascadeType.ALL)
    private List<Project> projects; // 유저가 관리하는 프로젝트

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Device> devices; // 유저가 소유한 디바이스

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Update method
    public void update(String name, String email, String password, UserRole role, PasswordEncoder passwordEncoder) {
        if (name != null) this.name = name;
        if (email != null) this.email = email;
        if (password != null) this.password = passwordEncoder.encode(password);
        if (role != null) this.role = role;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }
}
