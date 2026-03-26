package com.example.demo.db.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
public class User
{
    @Id @Setter
    @Getter
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY) // Auto-incrementing ID
    private Long id;
    @Getter @Setter
    private String username;
    @Getter @Setter
    private String password;


}
