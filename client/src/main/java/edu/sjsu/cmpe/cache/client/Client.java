package edu.sjsu.cmpe.cache.client;

public class Client {

    public static void main(String[] args) throws Exception {
        //System.out.println("Starting Cache Client...");
        CacheServiceInterface cache = new DistributedCacheService(
                "http://localhost:300");

        cache.put(1, "a");
        //System.out.println("put(1 => a)");

        //String value = cache.get(1);
        //System.out.println("get(1) => " + value);
        
        cache.put(2, "b");
        cache.put(3, "c");
        cache.put(4, "d");
        cache.put(5, "e");
        cache.put(6, "f");
        cache.put(7, "g");
        cache.put(8, "h");
        cache.put(9, "i");
        cache.put(10, "j");
        
        for(int i =1; i<11 ; i++)
        {
        	System.out.println(i +" => " + cache.get(i));
        }
        	

        //System.out.println("Existing Cache Client...");
    }

}
