package com.union.solutions.saascore.application.port;

import java.time.Instant;

public record BillingInvoice(
    String id,
    String status,
    String currency,
    long amountDueCents,
    Instant createdAt,
    Instant periodStart,
    Instant periodEnd,
    String hostedInvoiceUrl,
    String invoicePdfUrl) {}
