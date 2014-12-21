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

        long serverID = 0;
        try {
            response = Unirest.get(this.cacheServerUrl + Long.toString(serverID) + "/cache/{key}")
                              .header("accept", "application/json")
                              .routeParam("key", Long.toString(key)).asJson();
        } catch (UnirestException e) {
            System.err.println(e);
        }

        if (response != null) {
            if (response.getBody() != null) {
                if (response.getBody().getObject() != null) {
                    return response.getBody().getObject().getString("value");
                }
            }
        }

        return "Not found";
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
                    System.out.println("We are done. Done: " + doneCount + " Fail: " + failCount);
                }

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
}

