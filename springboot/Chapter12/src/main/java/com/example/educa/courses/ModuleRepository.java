package com.example.educa.courses;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByCourseIdOrderByOrderAsc(Long courseId);
}
