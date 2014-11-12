package edu.sjsu.cmpe.cache.client;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Distributed cache service
 * 
 */
public class DistributedCacheService implements CacheServiceInterface {
    private final String cacheServerUrl;
    //HashFunction hashFunction = Hashing.md5();
    List<Long> servers;//Lists.newArrayList("0", "1",);
    ConsistentHash<Long> consistentHash;
    public DistributedCacheService(String serverUrl) {
        this.cacheServerUrl = serverUrl;
        this.servers = Arrays.asList(0L, 1L , 2L);
        HashFunction hf = Hashing.md5();

        this.consistentHash = new ConsistentHash<Long>(hf, 1000, this.servers); 

    }

    private long getServerId(long key) 
    {
    	//int serverid = consistentHash(Hashing.md5().hashString(Long.toString(key)),10000, servers.size());
    	/*long hash = hashFunction.hashString(Long.toString(key)).asLong() % 3;
    	System.out.println(hashFunction.hashString(Long.toString(key)).asLong());
    	System.out.println(hashFunction.hashString(Long.toString(key)).asLong() % 3);
    	if(hash < 0)
    	{
    		return hash + 3;
    	}
    	else
    	{
    		return hash ;
    	}*/
    	//int serverID = Hashing.consistentHash(hashFunction(key.toString()));
    	//return Integer.parseInt(serverID);
    	return this.consistentHash.get(Long.toString(key));
    }
    /**
     * @see edu.sjsu.cmpe.cache.client.CacheServiceInterface#get(long)
     */
    @Override
    public String get(long key) {
        HttpResponse<JsonNode> response = null;
        
        long serverID = this.getServerId(key);
        try {
            response = Unirest.get(this.cacheServerUrl + Long.toString(serverID) + "/cache/{key}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key)).asJson();
            
            
        } catch (UnirestException e) {
            System.err.println(e);
        }
        String value = response.getBody().getObject().getString("value");
        return value;
    }

    /**
     * @see edu.sjsu.cmpe.cache.client.CacheServiceInterface#put(long,
     *      java.lang.String)
     */
    @Override
    public void put(long key, String value) {
        HttpResponse<JsonNode> response = null;
        long serverID = this.getServerId(key);
        try {
            response = Unirest
                    .put(this.cacheServerUrl + Long.toString(serverID) + "/cache/{key}/{value}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key))
                    .routeParam("value", value).asJson();
        } catch (UnirestException e) {
            System.err.println(e);
        }

        if (response.getCode() != 200) {
            System.out.println("Failed to add to the cache.");
        }
    }
    
    public String hashFunction(String md5) {
 	   try {
 	        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
 	        byte[] array = md.digest(md5.getBytes());
 	        StringBuffer sb = new StringBuffer();
 	        for (int i = 0; i < array.length; ++i) {
 	          sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
 	       }
 	        return sb.toString();
 	    } catch (java.security.NoSuchAlgorithmException e) {
 	    }
 	    return null;
 	}
}

