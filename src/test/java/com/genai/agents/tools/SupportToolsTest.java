package com.genai.agents.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupportToolsTest {

    private final SupportTools tools = new SupportTools();

    @Test
    void refundsADeliveredOrderInsideTheWindow() {
        var eligibility = tools.checkRefundEligibility("ORD-1001");

        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.maxRefund()).isEqualByComparingTo("49.00");
    }

    @Test
    void refusesAnOrderOutsideTheWindow() {
        var eligibility = tools.checkRefundEligibility("ORD-1002");

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.maxRefund()).isEqualByComparingTo("0");
        assertThat(eligibility.reason()).contains("45");
    }

    @Test
    void refusesACancelledOrder() {
        assertThat(tools.checkRefundEligibility("ORD-1003").eligible()).isFalse();
    }

    @Test
    void unknownOrderFailsTheLookup() {
        assertThatThrownBy(() -> tools.getOrder("ORD-404"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORD-404");
    }
}
