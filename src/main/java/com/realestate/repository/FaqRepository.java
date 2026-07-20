package com.realestate.repository;

import com.realestate.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<Faq> findAllByOrderBySortOrderAscIdAsc();
}
