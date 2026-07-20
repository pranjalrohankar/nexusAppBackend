package com.nexus.backend.repository;

import com.nexus.backend.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    List<Enquiry> findAll(Sort sort);
}