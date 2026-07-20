package com.realestate.repository;

import com.realestate.entity.CarouselSlide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarouselSlideRepository extends JpaRepository<CarouselSlide, Long> {

    List<CarouselSlide> findByActiveTrueOrderByDisplayOrderAsc();

    List<CarouselSlide> findAllByOrderByDisplayOrderAsc();
}
