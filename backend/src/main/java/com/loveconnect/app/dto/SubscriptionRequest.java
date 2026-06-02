package com.loveconnect.app.dto;

import com.loveconnect.app.entity.PlanType;
import java.math.BigDecimal;
import javax.validation.constraints.NotNull;

public class SubscriptionRequest {
    @NotNull private PlanType planType;
    private BigDecimal amount;
    private String paymentReference;

    public PlanType getPlanType() { return planType; }
    public void setPlanType(PlanType planType) { this.planType = planType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
}

