package com.ticketa.authservice.models;

import com.ticketa.authservice.models.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.Constraint;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name= "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(value = EnumType.STRING)
    private Role role;

}
