package com.physioconnect.repository;

import com.physioconnect.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByDoctorIdAndIsVisibleTrue(Long doctorId, Pageable pageable);
}
