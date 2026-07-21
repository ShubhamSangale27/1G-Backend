package com.realestate.repository;

import com.realestate.entity.PushCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushCampaignRepository extends JpaRepository<PushCampaign, Long> {

    Page<PushCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
