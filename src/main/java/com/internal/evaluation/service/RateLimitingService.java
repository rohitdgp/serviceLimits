package com.internal.evaluation.service;

public interface RateLimitingService {
    public Boolean isAllowed(String serviceName,String apiName, String method);
    public Boolean isAllowed(String apiName, String method);
}
