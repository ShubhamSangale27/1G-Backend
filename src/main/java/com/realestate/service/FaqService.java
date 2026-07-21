package com.realestate.service;

import com.realestate.dto.*;
import com.realestate.entity.Faq;
import com.realestate.entity.UnmatchedFaqQuestion;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.FaqRepository;
import com.realestate.repository.UnmatchedFaqQuestionRepository;
import com.realestate.repository.UserRepository;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaqService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "to", "of", "in", "on", "for", "with", "at", "by", "from", "as",
            "and", "or", "but", "if", "how", "what", "when", "where", "why",
            "who", "which", "do", "does", "did", "can", "could", "should",
            "would", "will", "i", "me", "my", "we", "you", "your", "it", "its",
            "this", "that", "please", "tell", "about"
    );
    private static final double MATCH_THRESHOLD = 1.0;

    private final FaqRepository faqRepository;
    private final UnmatchedFaqQuestionRepository unmatchedRepository;
    private final UserRepository userRepository;

    @Value("${app.support.email:support@1guntha.com}")
    private String supportEmail;

    @Value("${app.support.website:https://1guntha.com}")
    private String supportWebsite;

    @Value("${app.support.phone:}")
    private String supportPhone;

    public List<FaqDto> listActive() {
        return faqRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(FaqDto::from)
                .collect(Collectors.toList());
    }

    public List<FaqDto> listAll() {
        return faqRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(FaqDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public FaqDto create(FaqCreateUpdateRequest request) {
        Faq faq = Faq.builder()
                .question(request.getQuestion().trim())
                .answer(request.getAnswer().trim())
                .keywords(blankToNull(request.getKeywords()))
                .active(request.getActive() == null || request.getActive())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return FaqDto.from(faqRepository.save(faq));
    }

    @Transactional
    public FaqDto update(Long id, FaqCreateUpdateRequest request) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faq", id));
        faq.setQuestion(request.getQuestion().trim());
        faq.setAnswer(request.getAnswer().trim());
        faq.setKeywords(blankToNull(request.getKeywords()));
        if (request.getActive() != null) {
            faq.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            faq.setSortOrder(request.getSortOrder());
        }
        return FaqDto.from(faqRepository.save(faq));
    }

    @Transactional
    public void delete(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new ResourceNotFoundException("Faq", id);
        }
        faqRepository.deleteById(id);
    }

    @Transactional
    public FaqAskResponse ask(String question, UserPrincipal principal) {
        if (question == null || question.isBlank()) {
            throw new BadRequestException("Question is required");
        }
        String trimmed = question.trim();
        List<Faq> activeFaqs = faqRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
        Faq best = null;
        double bestScore = 0;
        for (Faq faq : activeFaqs) {
            double score = scoreMatch(trimmed, faq);
            if (score > bestScore) {
                bestScore = score;
                best = faq;
            }
        }

        if (best != null && bestScore >= MATCH_THRESHOLD) {
            return FaqAskResponse.builder()
                    .matched(true)
                    .answer(best.getAnswer())
                    .faqId(best.getId())
                    .build();
        }

        User user = null;
        if (principal != null && principal.getId() != null) {
            user = userRepository.findById(principal.getId()).orElse(null);
        }
        unmatchedRepository.save(UnmatchedFaqQuestion.builder()
                .questionText(trimmed)
                .user(user)
                .status(UnmatchedFaqQuestion.Status.PENDING)
                .build());

        return buildFallbackResponse();
    }

    @Transactional(readOnly = true)
    public List<UnmatchedFaqQuestionDto> listUnmatched(UnmatchedFaqQuestion.Status status) {
        UnmatchedFaqQuestion.Status filter = status != null ? status : UnmatchedFaqQuestion.Status.PENDING;
        return unmatchedRepository.findByStatusOrderByCreatedAtDesc(filter).stream()
                .map(UnmatchedFaqQuestionDto::from)
                .collect(Collectors.toList());
    }

    public long countPendingUnmatched() {
        return unmatchedRepository.countByStatus(UnmatchedFaqQuestion.Status.PENDING);
    }

    @Transactional
    public UnmatchedFaqQuestionDto resolve(Long id) {
        UnmatchedFaqQuestion u = unmatchedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UnmatchedFaqQuestion", id));
        if (u.getStatus() == UnmatchedFaqQuestion.Status.RESOLVED) {
            return UnmatchedFaqQuestionDto.from(u);
        }
        u.setStatus(UnmatchedFaqQuestion.Status.RESOLVED);
        u.setResolvedAt(Instant.now());
        return UnmatchedFaqQuestionDto.from(unmatchedRepository.save(u));
    }

    @Transactional
    public UnmatchedFaqQuestionDto promote(Long id, FaqPromoteRequest request) {
        UnmatchedFaqQuestion u = unmatchedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UnmatchedFaqQuestion", id));
        Faq faq = Faq.builder()
                .question(u.getQuestionText())
                .answer(request.getAnswer().trim())
                .keywords(blankToNull(request.getKeywords()))
                .active(request.getActive() == null || request.getActive())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        faq = faqRepository.save(faq);
        u.setPromotedFaq(faq);
        u.setStatus(UnmatchedFaqQuestion.Status.RESOLVED);
        u.setResolvedAt(Instant.now());
        return UnmatchedFaqQuestionDto.from(unmatchedRepository.save(u));
    }

    private FaqAskResponse buildFallbackResponse() {
        return FaqAskResponse.builder()
                .matched(false)
                .answer("Just Missedcall on 9134913491, will help you!")
                .contactEmail(supportEmail)
                .contactWebsite(blankToNull(supportWebsite))
                .contactPhone("9134913491")
                .build();
    }

    double scoreMatch(String userQuestion, Faq faq) {
        String normalizedUser = normalize(userQuestion);
        String normalizedFaqQ = normalize(faq.getQuestion());
        if (normalizedUser.isEmpty() || normalizedFaqQ.isEmpty()) {
            return 0;
        }

        double score = 0;
        if (normalizedUser.equals(normalizedFaqQ)) {
            score += 10;
        } else if (normalizedFaqQ.contains(normalizedUser) || normalizedUser.contains(normalizedFaqQ)) {
            score += 5;
        }

        Set<String> userTokens = tokens(normalizedUser);
        Set<String> faqTokens = tokens(normalizedFaqQ);
        if (!userTokens.isEmpty() && !faqTokens.isEmpty()) {
            long overlap = userTokens.stream().filter(faqTokens::contains).count();
            score += overlap * 1.5;
        }

        if (faq.getKeywords() != null && !faq.getKeywords().isBlank()) {
            for (String kw : faq.getKeywords().split(",")) {
                String nk = normalize(kw.trim());
                if (nk.isEmpty()) continue;
                if (normalizedUser.contains(nk) || tokens(normalizedUser).contains(nk)) {
                    score += 2.5;
                }
            }
        }

        return score;
    }

    private static String normalize(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.ROOT).trim();
        return NON_ALNUM.matcher(lower).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private static Set<String> tokens(String normalized) {
        if (normalized.isEmpty()) return Set.of();
        return Arrays.stream(normalized.split("\\s+"))
                .filter(t -> t.length() > 1)
                .filter(t -> !STOP_WORDS.contains(t))
                .collect(Collectors.toSet());
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
