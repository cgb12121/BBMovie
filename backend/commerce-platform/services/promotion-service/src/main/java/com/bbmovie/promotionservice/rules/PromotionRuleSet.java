package com.bbmovie.promotionservice.rules;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PromotionRuleSet {
    private List<PromotionRule> rules = new ArrayList<>();
}
