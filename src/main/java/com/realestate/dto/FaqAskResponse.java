package com.realestate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqAskResponse {

    private boolean matched;
    private String answer;
    private Long faqId;
    private String contactEmail;
    private String contactWebsite;
    private String contactPhone;
}
