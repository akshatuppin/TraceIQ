package com.traceiq.core.model.entity;

public enum EvidenceType {
    STACK_TRACE,
    SLOW_QUERY,
    STATUS_5XX_BURST,
    TIMEOUT,
    ML_ANOMALY_FEATURE,
    CORRELATED_TRACE
}
