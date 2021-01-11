package com.internal.evaluation.service.impl;

import com.internal.evaluation.service.RateLimitingService;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    private JSONObject serviceLimits = new JSONObject();
    HashMap<String, Object> globalLimits = new HashMap<>();
    HashMap<String, Object> apiLimits = new HashMap<>();

    private HashMap<String, HashMap<String, List<Long>>> serviceStorages = new HashMap<>(); //This is a local storage created here


    public RateLimitingServiceImpl rateLimitingService(){
        initializeServiceRates();
        return new RateLimitingServiceImpl();
    }

    public Boolean isAllowed(String serviceName,String apiName, String method){

        return isGlobalAllowed(serviceName, method) && isAPIAllowed(apiName, method);

    }

    public Boolean isAllowed(String apiName, String method){

        String serviceName = ((JSONObject)apiLimits.get(apiName)).getString("serviceName");

        return isGlobalAllowed(serviceName, method) && isAPIAllowed(apiName, method);

    }



    private Boolean isGlobalAllowed(String serviceName, String method){
        if(((JSONObject)globalLimits.get(serviceName)).has(method)){
            if(serviceStorages.get(serviceName)!=null && !serviceStorages.get(serviceName).isEmpty() && serviceStorages.get(serviceName).get(method)!=null) {
                return checkRateLimit(serviceName,method, ((JSONObject) globalLimits.get(serviceName)).getJSONObject(method));
            } else {
                if(((JSONObject) globalLimits.get(serviceName)).getJSONObject(method).getLong("limit")>0L) {
                    if(serviceStorages.get(serviceName)==null)
                        serviceStorages.put(serviceName, new HashMap<>());
                    if(serviceStorages.get(serviceName).get(method)==null)
                        serviceStorages.get(serviceName).put(method, new ArrayList<>());
                    serviceStorages.get(serviceName).get(method).add(System.currentTimeMillis());
                    return true;
                }
                else return false;
            }
        }else return true;
    }




    private Boolean isAPIAllowed(String apiName, String method){

        // serviceLimits.{api : api?="<apiToken>".{method: <methodToken>}}

        if(((JSONObject)apiLimits.get(apiName)).has("methods") && ((JSONObject)apiLimits.get(apiName)).getJSONObject("methods").has(method)){
            if(serviceStorages.get(apiName)!=null && !serviceStorages.get(apiName).isEmpty() && serviceStorages.get(apiName).get(method)!=null) {
                return checkRateLimit(apiName, method, ((JSONObject)apiLimits.get(apiName)).getJSONObject("methods").getJSONObject(method));
            } else {
                if(((JSONObject)apiLimits.get(apiName)).getJSONObject("methods").getJSONObject(method).getLong("limit")>0L){
                    if(serviceStorages.get(apiName)==null)
                        serviceStorages.put(apiName, new HashMap<>());
                    if(serviceStorages.get(apiName).get(method)==null)
                        serviceStorages.get(apiName).put(method, new ArrayList<>());
                    serviceStorages.get(apiName).get(method).add(System.currentTimeMillis());
                    return true;
                }
                else return false;
            }
        }else return true;
    }



    private Boolean checkRateLimit(String identifier,String method, JSONObject derive){

        List<Long> hits = serviceStorages.get(identifier).get(method);
         //Sliding window Algorithm
        // [epochTime (in millis), 1, 2, 3, 4 ]
        Long millistocheck = 1000L;
        switch(((JSONObject)derive).getString("granularity")){
            case "second":{
                millistocheck = 1000L;
                break;
            }
            case "minute":{
                millistocheck = 60000L;
                break;
            }
            case "hour":{
                millistocheck = 60*60000L;
                break;

            }
            case "day":{
                millistocheck = 24*60*60000L;
                break;
            }
        }
        Long curMillis = System.currentTimeMillis();
        Long totalCalls = 0L;

        for(int i=hits.size()-1; i>=0; i--){
            if(millistocheck>0 && (curMillis - hits.get(i))<millistocheck ){
                totalCalls++;
            }
        }

        if(totalCalls<((JSONObject)derive).getLong("limit")){
            logger.info("Available Limit - " + (((JSONObject)derive).getLong("limit") - totalCalls));
            serviceStorages.get(identifier).get(method).add(curMillis);
            return true;
        }else{
            logger.info("Trying to make no. of calls - "+ totalCalls);
            logger.info("Unable to make the call. Crossed the defined limit of - "+ (((JSONObject)derive).getLong("limit")));
            return false;
        }


    }


    @PostConstruct()
    private void initializeServiceRates(){
        try {
            ClassPathResource resource = new ClassPathResource("serviceLimits.json");
            String jsonTxt = IOUtils.toString(resource.getInputStream(), "UTF-8");
            serviceLimits = new JSONObject(jsonTxt);


            for(Object item : serviceLimits.getJSONArray("serviceLimits")){
                String serviceName = ((JSONObject)item).getString("service");
                if(!((JSONObject)item).getJSONObject("globalLimits").isEmpty()){
                    globalLimits.put(serviceName, ((JSONObject)item).getJSONObject("globalLimits"));
                }

                for(Object api : ((JSONObject)item).getJSONArray("apiLimits")) {
                    String apiName = ((JSONObject)api).getString("api");
                        JSONObject obj = new JSONObject();
                        obj.put("serviceName", serviceName);
                        obj.put("methods", ((JSONObject) api).getJSONObject("methods"));
                        apiLimits.put(apiName, obj);
                }
            }

        }catch(Exception e){
            logger.error("Unable to fetch limit configurations from File.");
            logger.error(e.getMessage());
        }
    }
}
