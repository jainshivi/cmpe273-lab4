package edu.sjsu.cmpe.cache.client;

public class Client {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Cache Client...");
        CacheServiceInterface cache = new DistributedCacheService(
                "http://localhost:300");

        Client.deleteAllKeys(cache);

        System.out.println("Init step 0");

        cache.put(1, "a");
        System.out.println("Put step 1");

        Client.sleep(15);

        cache.put(1, "b");
        System.out.println("Put step 2");

        Client.sleep(15);

        String value = cache.get(1);
        System.out.println("get => " + value);
    }

    public static void deleteAllKeys(CacheServiceInterface cache) {
        // Delete all the keys
        for (int i=1; i<10; i++)
            for (int j=0; j<3; j++)
                cache.delete(i, j);
    }

    public static void sleep(int secs) {
       try {
            Thread.sleep(1000 * secs);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }    
    }
}
