package com.internal.evaluation.controller;

import com.internal.evaluation.service.RateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
public class GenericController {

    @Autowired
    private RateLimitingService rateLimitingService;

    @RequestMapping(value = "/checkByServiceAndApi", method = RequestMethod.GET)
    public String callAllowedByAllFilter(@RequestParam("serviceName") String serviceName, @RequestParam("apiName") String apiName, @RequestParam("method") String method){
        if(rateLimitingService.isAllowed(serviceName, apiName, method)){
            return "Allowed";
        }else return "not Allowed";
    }

    @RequestMapping(value = "/checkByApi", method = RequestMethod.GET)
    public String callAllowedByApi(@RequestParam("apiName") String apiName, @RequestParam("method") String method){
        if(rateLimitingService.isAllowed(apiName, method)){
            return "Allowed";
        }else return "not Allowed";
    }
}
