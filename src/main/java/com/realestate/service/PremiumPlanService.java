package com.realestate.service;

import com.realestate.dto.PremiumPlanDto;
import com.realestate.entity.PremiumPlan;
import com.realestate.repository.PremiumPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PremiumPlanService {

    private final PremiumPlanRepository planRepository;

    public List<PremiumPlanDto> getAllPlans() {
        return planRepository.findAllByOrderByPriceAsc().stream()
                .map(PremiumPlanDto::from)
                .collect(Collectors.toList());
    }
}
