package com.api.rizz.portfolio_api.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * User
 */
public class User implements UserDetails {

  public enum Role {
    USER,
    ADMIN
  }

  public enum AuthProvider {
    LOCAL,
    GITHUB
  }

  public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
  }

  @Id
  private Long id;

  @Column(nullable = false)
  private String nickname;

  @Column(name = "full_name")
  private String fullName;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = true)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.USER;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider = AuthProvider.LOCAL;

  @Column(name = "profile_picture")
  private String profilePict;

  @Column(name = "place_of_birth")
  private String placeOfBirth;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(columnDefinition = "TEXT")
  private String bio;

  @Column(columnDefinition = "TEXT")
  private String address;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // * Spring Security wajib implement method ini
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  // * Udah best practice ternyata
  @Override
  public String getUsername() {
    return email; // * Pakai email sebagai username
  }

  @Override
  public boolean isAccountNonExpired() {
    return true; // diubah nanti kalau kamu mau fitur ban/suspend akun
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

}
