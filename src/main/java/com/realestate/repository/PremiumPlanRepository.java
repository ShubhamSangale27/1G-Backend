package com.realestate.repository;

import com.realestate.entity.PremiumPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PremiumPlanRepository extends JpaRepository<PremiumPlan, Long> {

    List<PremiumPlan> findAllByOrderByPriceAsc();
}
