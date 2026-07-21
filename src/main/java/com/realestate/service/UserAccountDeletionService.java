package com.realestate.service;

import com.realestate.entity.Property;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.AlertRepository;
import com.realestate.repository.BlogPostRepository;
import com.realestate.repository.DeviceTokenRepository;
import com.realestate.repository.EmailVerificationTokenRepository;
import com.realestate.repository.PaymentRepository;
import com.realestate.repository.PropertyAnalyticsRepository;
import com.realestate.repository.PropertyImageRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.RefreshTokenRepository;
import com.realestate.repository.SiteVisitCommentRepository;
import com.realestate.repository.SiteVisitRepository;
import com.realestate.repository.UserRepository;
import com.realestate.repository.VisitOTPRepository;
import com.realestate.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAccountDeletionService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final PropertyAnalyticsRepository propertyAnalyticsRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final VisitOTPRepository visitOTPRepository;
    private final SiteVisitCommentRepository siteVisitCommentRepository;
    private final WatchlistRepository watchlistRepository;
    private final AlertRepository alertRepository;
    private final PaymentRepository paymentRepository;
    private final BlogPostRepository blogPostRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Transactional
    public void deleteUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        deleteUser(user);
    }

    @Transactional
    public void deleteUser(User user) {
        Long userId = user.getId();

        deleteOwnedProperties(userId);
        siteVisitCommentRepository.deleteByUserId(userId);
        siteVisitRepository.deleteByUserId(userId);
        siteVisitRepository.clearAgentAssignments(userId);
        watchlistRepository.deleteByUserId(userId);
        alertRepository.deleteByUserId(userId);
        paymentRepository.deleteByUserId(userId);
        blogPostRepository.deleteByAuthorId(userId);

        refreshTokenRepository.deleteByUserId(userId);
        deviceTokenRepository.deleteByUserId(userId);
        emailVerificationTokenRepository.deleteByUser_Id(userId);

        userRepository.delete(user);
    }

    private void deleteOwnedProperties(Long userId) {
        List<Property> owned = propertyRepository.findAllByOwnerId(userId);
        for (Property property : owned) {
            deletePropertyAndDependents(property);
        }
    }

    private void deletePropertyAndDependents(Property property) {
        Long propertyId = property.getId();
        siteVisitCommentRepository.deleteBySiteVisitPropertyId(propertyId);
        visitOTPRepository.deleteBySiteVisitPropertyId(propertyId);
        siteVisitRepository.deleteByPropertyId(propertyId);
        watchlistRepository.deleteByPropertyId(propertyId);
        propertyImageRepository.deleteByPropertyId(propertyId);
        propertyAnalyticsRepository.deleteByPropertyId(propertyId);
        propertyRepository.delete(property);
    }

    public void ensureNotLastAdmin(User user) {
        if (user.getRole() == User.Role.ADMIN
                && userRepository.countByRole(User.Role.ADMIN) <= 1) {
            throw new BadRequestException("Cannot delete the last admin account.");
        }
    }
}
