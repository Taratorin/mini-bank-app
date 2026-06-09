package ru.cloudwithout.accountsservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String login;
    private String firstLastName;
    private LocalDate birthDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal sum;

}