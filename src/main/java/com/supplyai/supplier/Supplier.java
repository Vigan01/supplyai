package com.supplyai.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Email
    private String email;

    @Min(1)
    @Column(nullable = false)
    private int leadTimeDays;

    @Column(nullable = false)
    private boolean delayed;

    protected Supplier() {
    }

    public Supplier(String name, String email, int leadTimeDays, boolean delayed) {
        this.name = name;
        this.email = email;
        this.leadTimeDays = leadTimeDays;
        this.delayed = delayed;
    }

    public void update(String name, String email, int leadTimeDays, boolean delayed) {
        this.name = name;
        this.email = email;
        this.leadTimeDays = leadTimeDays;
        this.delayed = delayed;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public boolean isDelayed() {
        return delayed;
    }
}
