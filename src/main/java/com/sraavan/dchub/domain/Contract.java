package com.sraavan.dchub.domain;

import com.sraavan.dchub.compat.CompatibilityMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A named data contract — the governed unit (e.g. {@code orders}, {@code payments.settled}).
 * It owns a stream of {@link ContractVersion}s and declares the compatibility tier all new
 * versions are checked against, plus an owner for accountability.
 */
@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompatibilityMode compatibilityMode = CompatibilityMode.BACKWARD;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Contract() {
    }

    public Contract(String name, String owner, CompatibilityMode compatibilityMode) {
        this.name = name;
        this.owner = owner;
        this.compatibilityMode = compatibilityMode;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public CompatibilityMode getCompatibilityMode() {
        return compatibilityMode;
    }

    public void setCompatibilityMode(CompatibilityMode compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
