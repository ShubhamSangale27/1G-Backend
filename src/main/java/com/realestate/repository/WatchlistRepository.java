package com.realestate.repository;

import com.realestate.entity.Watchlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    Page<Watchlist> findByUserId(Long userId, Pageable pageable);

    Optional<Watchlist> findByUserIdAndPropertyId(Long userId, Long propertyId);

    List<Watchlist> findByPropertyId(Long propertyId);

    boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);

    void deleteByUserIdAndPropertyId(Long userId, Long propertyId);
}
