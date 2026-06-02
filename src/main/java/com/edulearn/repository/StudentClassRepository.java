package com.edulearn.repository;

import com.edulearn.entity.Classroom;
import com.edulearn.entity.StudentClass;
import com.edulearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentClassRepository extends JpaRepository<StudentClass, UUID> {
    List<StudentClass> findByStudent(User student);
    List<StudentClass> findByStudentAndIsActiveTrue(User student);
    List<StudentClass> findByClassroom(Classroom classroom);
    Optional<StudentClass> findByStudentAndClassroom(User student, Classroom classroom);
    List<StudentClass> findByClassroomAndIsActiveTrue(Classroom classroom);
}
