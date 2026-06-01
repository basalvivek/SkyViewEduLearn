package com.edulearn.repository;

import com.edulearn.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {
    List<Classroom> findByIsActiveTrue();
    Optional<Classroom> findByName(String name);
}
