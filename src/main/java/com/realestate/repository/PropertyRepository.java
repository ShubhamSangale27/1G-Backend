package com.realestate.repository;

import com.realestate.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    Page<Property> findByStatus(Property.PropertyStatus status, Pageable pageable);

    List<Property> findTop10ByStatusOrderByCreatedAtDesc(Property.PropertyStatus status);

    List<Property> findByFeaturedTrueAndStatusOrderByCreatedAtDesc(Property.PropertyStatus status, Pageable pageable);

    Page<Property> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Property> findByOwnerIdAndStatus(Long ownerId, Property.PropertyStatus status, Pageable pageable);

    Page<Property> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Property> findByFeatured(Boolean featured, Pageable pageable);

    Page<Property> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after, Pageable pageable);

    @Query("SELECT p FROM Property p WHERE p.status = :status " +
           "AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:listingType IS NULL OR p.listingType = :listingType) " +
           "AND (:propertyType IS NULL OR p.propertyType = :propertyType) " +
           "AND (:bedrooms IS NULL OR p.bedrooms >= :bedrooms)")
    Page<Property> search(@Param("status") Property.PropertyStatus status,
                          @Param("city") String city,
                          @Param("minPrice") BigDecimal minPrice,
                          @Param("maxPrice") BigDecimal maxPrice,
                          @Param("listingType") Property.ListingType listingType,
                          @Param("propertyType") Property.PropertyType propertyType,
                          @Param("bedrooms") Integer bedrooms,
                          Pageable pageable);
}
