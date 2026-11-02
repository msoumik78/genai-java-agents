package com.genai.agents.controllers;

import com.genai.agents.services.RefundAgentService;
import com.genai.agents.vo.AgentRun;
import com.genai.agents.vo.RefundRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RefundController {

    private final RefundAgentService refundAgentService;

    public RefundController(RefundAgentService refundAgentService) {
        this.refundAgentService = refundAgentService;
    }

    @PostMapping("/api/refund-decisions")
    public AgentRun decide(@Valid @RequestBody RefundRequest request) {
        return refundAgentService.decide(request.orderId(), request.customerNote());
    }
}
