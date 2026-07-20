package com.realestate.service;

import com.realestate.dto.*;
import com.realestate.entity.Faq;
import com.realestate.entity.UnmatchedFaqQuestion;
import com.realestate.entity.User;
import com.realestate.repository.FaqRepository;
import com.realestate.repository.UnmatchedFaqQuestionRepository;
import com.realestate.repository.UserRepository;
import com.realestate.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    @Mock
    private FaqRepository faqRepository;
    @Mock
    private UnmatchedFaqQuestionRepository unmatchedRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FaqService faqService;

    private Faq rentFaq;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(faqService, "supportEmail", "support@1guntha.com");
        ReflectionTestUtils.setField(faqService, "supportWebsite", "https://1guntha.com");
        ReflectionTestUtils.setField(faqService, "supportPhone", "");

        rentFaq = Faq.builder()
                .id(1L)
                .question("How do I list a property for rent?")
                .answer("Go to List Property and choose Rent as listing type.")
                .keywords("rent, list, listing")
                .active(true)
                .sortOrder(0)
                .build();
    }

    @Test
    void ask_returnsMatchedAnswer_whenQuestionOverlapsFaq() {
        when(faqRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(rentFaq));

        FaqAskResponse res = faqService.ask("How do I list property for rent?", null);

        assertThat(res.isMatched()).isTrue();
        assertThat(res.getAnswer()).contains("List Property");
        assertThat(res.getFaqId()).isEqualTo(1L);
        verify(unmatchedRepository, never()).save(any());
    }

    @Test
    void ask_matchesByKeyword() {
        when(faqRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(rentFaq));

        FaqAskResponse res = faqService.ask("Tell me about rent listing", null);

        assertThat(res.isMatched()).isTrue();
        assertThat(res.getFaqId()).isEqualTo(1L);
    }

    @Test
    void ask_persistsUnmatchedAsUnknown_whenNoMatchAndAnonymous() {
        when(faqRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(rentFaq));
        when(unmatchedRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FaqAskResponse res = faqService.ask("What is the weather in Mumbai?", null);

        assertThat(res.isMatched()).isFalse();
        assertThat(res.getAnswer()).contains("support@1guntha.com");
        assertThat(res.getAnswer()).contains("https://1guntha.com");
        assertThat(res.getContactEmail()).isEqualTo("support@1guntha.com");

        ArgumentCaptor<UnmatchedFaqQuestion> captor = ArgumentCaptor.forClass(UnmatchedFaqQuestion.class);
        verify(unmatchedRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionText()).isEqualTo("What is the weather in Mumbai?");
        assertThat(captor.getValue().getUser()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(UnmatchedFaqQuestion.Status.PENDING);
    }

    @Test
    void ask_attachesLoggedInUser_whenUnmatched() {
        when(faqRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of());
        when(unmatchedRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = User.builder().id(42L).email("buyer@test.com").fullName("Buyer").build();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        UserPrincipal principal = new UserPrincipal(42L, "buyer@test.com", "USER");
        faqService.ask("Random out of scope question xyz", principal);

        ArgumentCaptor<UnmatchedFaqQuestion> captor = ArgumentCaptor.forClass(UnmatchedFaqQuestion.class);
        verify(unmatchedRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void create_and_update_and_delete() {
        when(faqRepository.save(any(Faq.class))).thenAnswer(inv -> {
            Faq f = inv.getArgument(0);
            if (f.getId() == null) f.setId(10L);
            return f;
        });
        when(faqRepository.findById(10L)).thenReturn(Optional.of(Faq.builder()
                .id(10L).question("Q").answer("A").active(true).sortOrder(0).build()));
        when(faqRepository.existsById(10L)).thenReturn(true);

        FaqCreateUpdateRequest createReq = new FaqCreateUpdateRequest();
        createReq.setQuestion("How to book a site visit?");
        createReq.setAnswer("Open property detail and click Book Visit.");
        createReq.setKeywords("visit, book");
        FaqDto created = faqService.create(createReq);
        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getQuestion()).contains("site visit");

        FaqCreateUpdateRequest updateReq = new FaqCreateUpdateRequest();
        updateReq.setQuestion("How to book a visit?");
        updateReq.setAnswer("Updated answer");
        updateReq.setActive(false);
        updateReq.setSortOrder(2);
        FaqDto updated = faqService.update(10L, updateReq);
        assertThat(updated.getAnswer()).isEqualTo("Updated answer");
        assertThat(updated.isActive()).isFalse();

        faqService.delete(10L);
        verify(faqRepository).deleteById(10L);
    }

    @Test
    void promote_createsFaqAndResolvesUnmatched() {
        UnmatchedFaqQuestion unmatched = UnmatchedFaqQuestion.builder()
                .id(5L)
                .questionText("Do you charge brokerage?")
                .status(UnmatchedFaqQuestion.Status.PENDING)
                .build();
        when(unmatchedRepository.findById(5L)).thenReturn(Optional.of(unmatched));
        when(faqRepository.save(any(Faq.class))).thenAnswer(inv -> {
            Faq f = inv.getArgument(0);
            f.setId(99L);
            return f;
        });
        when(unmatchedRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FaqPromoteRequest req = new FaqPromoteRequest();
        req.setAnswer("No, 1Guntha does not charge brokerage.");
        req.setKeywords("brokerage, fee");

        UnmatchedFaqQuestionDto dto = faqService.promote(5L, req);

        assertThat(dto.getStatus()).isEqualTo("RESOLVED");
        assertThat(dto.getPromotedFaqId()).isEqualTo(99L);
        assertThat(unmatched.getPromotedFaq().getAnswer()).contains("brokerage");
    }

    @Test
    void resolve_marksPendingAsResolved() {
        UnmatchedFaqQuestion unmatched = UnmatchedFaqQuestion.builder()
                .id(7L)
                .questionText("Hello")
                .status(UnmatchedFaqQuestion.Status.PENDING)
                .build();
        when(unmatchedRepository.findById(7L)).thenReturn(Optional.of(unmatched));
        when(unmatchedRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UnmatchedFaqQuestionDto dto = faqService.resolve(7L);

        assertThat(dto.getStatus()).isEqualTo("RESOLVED");
        assertThat(dto.getResolvedAt()).isNotNull();
    }

    @Test
    void unmatchedDto_showsUnknown_whenNoUser() {
        UnmatchedFaqQuestion unmatched = UnmatchedFaqQuestion.builder()
                .id(1L)
                .questionText("Q")
                .status(UnmatchedFaqQuestion.Status.PENDING)
                .user(null)
                .build();
        UnmatchedFaqQuestionDto dto = UnmatchedFaqQuestionDto.from(unmatched);
        assertThat(dto.getUserFullName()).isEqualTo("Unknown");
        assertThat(dto.getUserEmail()).isNull();
    }
}
