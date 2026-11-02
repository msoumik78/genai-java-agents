package com.genai.agents.tools;

import com.genai.agents.vo.Eligibility;
import com.genai.agents.vo.OrderSnapshot;
import com.genai.agents.vo.RefundPolicy;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class SupportTools {

    static final RefundPolicy POLICY = new RefundPolicy(
            30,
            new BigDecimal("200.00"),
            "Delivered orders can be refunded within 30 days. Damaged items qualify inside that window. "
                    + "The refund cannot exceed the amount paid. Automatic approval stops at 200."
    );

    private static final Map<String, OrderSnapshot> ORDERS = Map.of(
            "ORD-1001", new OrderSnapshot("ORD-1001", "DELIVERED", new BigDecimal("49.00"), 12),
            "ORD-1002", new OrderSnapshot("ORD-1002", "DELIVERED", new BigDecimal("80.00"), 45),
            "ORD-1003", new OrderSnapshot("ORD-1003", "CANCELLED", new BigDecimal("15.00"), 0)
    );

    @Tool(description = "Look up an order by id. Returns status, total paid, and days since delivery.")
    public OrderSnapshot getOrder(@ToolParam(description = "Order id, for example ORD-1001") String orderId) {
        OrderSnapshot order = ORDERS.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Unknown order " + orderId);
        }
        return order;
    }

    @Tool(description = "Return the refund policy: window in days, auto-approve limit, and a short summary.")
    public RefundPolicy getPolicy() {
        return POLICY;
    }

    @Tool(description = "Decide whether an order can be refunded and the maximum refund amount. Does not issue a refund.")
    public Eligibility checkRefundEligibility(@ToolParam(description = "Order id, for example ORD-1001") String orderId) {
        OrderSnapshot order = getOrder(orderId);
        if (!"DELIVERED".equals(order.status())) {
            return new Eligibility(orderId, false, BigDecimal.ZERO, "Only delivered orders can be refunded");
        }
        if (order.daysSinceDelivery() > POLICY.windowDays()) {
            return new Eligibility(orderId, false, BigDecimal.ZERO,
                    "Delivery was " + order.daysSinceDelivery() + " days ago, past the " + POLICY.windowDays() + " day window");
        }
        return new Eligibility(orderId, true, order.total(), "Inside the refund window");
    }
}
