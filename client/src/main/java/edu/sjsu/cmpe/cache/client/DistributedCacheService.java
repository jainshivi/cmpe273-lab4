package edu.sjsu.cmpe.cache.client;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;
import java.util.concurrent.TimeoutException;

import java.io.EOFException;
import java.lang.RuntimeException;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Options;;

/**
 * Distributed cache service
 * 
 */
public class DistributedCacheService implements CacheServiceInterface {
    private final String cacheServerUrl;

    public DistributedCacheService(String serverUrl) {
        this.cacheServerUrl = serverUrl;
    }


    /**
     * @see edu.sjsu.cmpe.cache.client.CacheServiceInterface#get(long)
     */
    @Override
    public String get(long key) {
        HttpResponse<JsonNode> response = null;
        List<Future<HttpResponse<JsonNode>>> futures = new ArrayList<Future<HttpResponse<JsonNode>>>(3);

        for (long serverID = 0; serverID < 3; serverID++) {
            String url = this.cacheServerUrl + Long.toString(serverID) + "/cache/{key}";

            futures.add(
                Unirest.get(url)
                .header("accept", "application/json")
                .routeParam("key", Long.toString(key))
                .asJsonAsync());
        }

        int doneCount = 0;
        String val1 = "";
        String val2 = "";

        int count1 = 0;
        int count2 = 0;
        String val = "";
        int flag = 0;

        List<String> futuresVal = new ArrayList<String>(3);

        for (int i = 0; i < 3; i++) {
            futuresVal.add("");
        }

        while (true) {
            doneCount = 0;

            for (int i = 0; i < 3; i++) {
                Future<HttpResponse<JsonNode>> f = futures.get(i);

                if (f.isDone()) {
                    doneCount++;

                    try {
                        response = f.get();
                        flag = 0;
                        if (response != null) {
                            if (response.getBody() != null) {
                                if (response.getBody().getObject() != null) {
                                    
                                    val = response.getBody().getObject().getString("value");
                                    futuresVal.set(i, val);

                                    if (val1.equals("")) {
                                        val1 = val;
                                        count1 = 1;
                                    } else if (val1.equals(val)) {
                                        count1++;
                                    } else {
                                        val2 = val;
                                        count2++;
                                    }

                                    flag = 1;
                                }
                            }
                        }

                        if (flag == 0) {
                            futuresVal.set(i, "fail");
                            count2 = 1;
                            val2 = "fail";
                        }
                    } catch (RuntimeException e) {

                    } catch (Exception e) {
                        futuresVal.set(i, "fail");
                        count2 = 1;
                        val2 = "fail";
                    }
                }
            }

            if (doneCount == 3) {
                break;
            }
        }

        if (count1 != 0 && count1 < count2) {
            // count 2 is majority.
            for (int i = 0; i < 3; i++) {
                if (futuresVal.get(i) == val1) {
                    this.putOnServer(key, val2, i);
                }
            }
            return val2;
        } else if (count2 != 0 && count2 < count1) {
            // count 1 is majority.
            for (int i = 0; i < 3; i++) {
                if (futuresVal.get(i) == val2) {
                    this.putOnServer(key, val1, i);
                }
            }
            return val1;
        } else if (count1 == 3) {
            return val1;
        }

        return val2;
        
    }

    /**
     * @see edu.sjsu.cmpe.cache.client.CacheServiceInterface#put(long,
     *      java.lang.String)
     */
    @Override
    public void put(long key, String value) {
        List<Future<HttpResponse<JsonNode>>> futures = new ArrayList<Future<HttpResponse<JsonNode>>>(3);

        for (long serverID = 0; serverID < 3; serverID++) {
            String url = this.cacheServerUrl + Long.toString(serverID) + "/cache/{key}/{value}";

            futures.add(
                Unirest.put(url)
                .header("accept", "application/json")
                .routeParam("key", Long.toString(key))
                .routeParam("value", value)
                .asJsonAsync());
        }

        int doneCount = 0;
        int failCount = 0;
        List<Integer> futuresStatus = new ArrayList<Integer>(3);

        for (int i = 0; i < 3; i++) {
            futuresStatus.add(1);
        }

        while (true) {
            doneCount = failCount = 0;

            for (int i = 0; i < 3; i++) {
                Future<HttpResponse<JsonNode>> f = futures.get(i);

                if (f.isDone()) {
                    doneCount++;

                    try {
                        f.get();
                        futuresStatus.set(i, 1);
                    } catch (Exception e) {
                        failCount++;
                        futuresStatus.set(i, 0);
                    }
                }
            }

            if (doneCount == 3) {

                if (failCount >= 2) {
                    // Revert the change.
                    for (int i = 0; i < 3; i++) {
                        if (futuresStatus.get(i) == 1) {
                            this.delete(key, i);
                        }
                    }
                }
                else {
                }

                futures.clear();
                futures = null;
                break;
            }
        }
    }

    @Override
    public void delete(long key, int serverID) {
        HttpResponse<JsonNode> response = null;

        try {
            response = Unirest.delete(this.cacheServerUrl + Integer.toString(serverID) + "/cache/{key}")
                              .routeParam("key", Long.toString(key))
                              .asJson();
        } catch (UnirestException e) {
            System.err.println(e);
        }
    }

    public void putOnServer(long key, String value, int serverID) {    
        String url = this.cacheServerUrl + Long.toString(serverID) + "/cache/{key}/{value}";

        try {
            Unirest.put(url)
                   .header("accept", "application/json")
                   .routeParam("key", Long.toString(key))
                   .routeParam("value", value)
                   .asJson();
        } catch (UnirestException e) {
            System.err.println(e);
        }
    }
}

