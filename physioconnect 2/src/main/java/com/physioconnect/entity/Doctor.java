package com.physioconnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String specialization;

    @Column(nullable = false, length = 255)
    private String qualification;

    @Builder.Default
    @Column(name = "experience_years")
    private Integer experienceYears = 0;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Builder.Default
    @Column(name = "average_rating", precision = 2, scale = 1)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @ManyToMany
    @JoinTable(
            name = "doctor_locations",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    private java.util.Set<Location> locations;
}