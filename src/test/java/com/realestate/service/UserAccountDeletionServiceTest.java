package com.realestate.service;

import com.realestate.entity.Property;
import com.realestate.entity.PropertyImage;
import com.realestate.entity.SiteVisit;
import com.realestate.entity.User;
import com.realestate.entity.Watchlist;
import com.realestate.exception.BadRequestException;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.SiteVisitRepository;
import com.realestate.repository.UserRepository;
import com.realestate.repository.WatchlistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAccountDeletionServiceTest {

    @Autowired private UserAccountDeletionService userAccountDeletionService;
    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private SiteVisitRepository siteVisitRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void ensureNotLastAdmin_blocksWhenOnlyOneAdminExists() {
        User onlyAdmin = userRepository.save(User.builder()
                .email("sole-admin@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Sole Admin")
                .mobile("+913333333399")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());

        assertThrows(BadRequestException.class,
                () -> userAccountDeletionService.ensureNotLastAdmin(onlyAdmin));
    }

    @Test
    void deleteUser_withOwnedPropertyAndDependents_removesAllRows() {
        User user = userRepository.save(User.builder()
                .email("owner-delete@test.com")
                .passwordHash(passwordEncoder.encode("pass123"))
                .fullName("Property Owner")
                .mobile("+919888877766")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());

        Property property = Property.builder()
                .title("Listing To Delete")
                .description("Will be removed with account")
                .listingType(Property.ListingType.SALE)
                .propertyType(Property.PropertyType.APARTMENT)
                .price(BigDecimal.valueOf(3_500_000))
                .address("42 Delete Lane")
                .city("Pune")
                .status(Property.PropertyStatus.APPROVED)
                .owner(user)
                .build();
        property.getImages().add(PropertyImage.builder()
                .property(property)
                .imageUrl("https://example.com/listing.jpg")
                .displayOrder(0)
                .build());
        property = propertyRepository.save(property);

        watchlistRepository.save(Watchlist.builder()
                .user(user)
                .property(property)
                .build());

        siteVisitRepository.save(SiteVisit.builder()
                .user(user)
                .property(property)
                .scheduledAt(Instant.now().plusSeconds(86_400))
                .build());

        Long userId = user.getId();
        Long propertyId = property.getId();

        userAccountDeletionService.deleteUser(user);

        assertFalse(userRepository.findById(userId).isPresent());
        assertFalse(propertyRepository.findById(propertyId).isPresent());
        assertTrue(watchlistRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged()).isEmpty());
        assertTrue(siteVisitRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged()).isEmpty());
    }
}
