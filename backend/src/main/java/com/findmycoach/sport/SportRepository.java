package com.findmycoach.sport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SportRepository extends JpaRepository<Sport, String> {
    List<Sport> findAllByOrderBySortOrderAscNameAsc();

    List<Sport> findByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<Sport> findByNameIgnoreCase(String name);
}
