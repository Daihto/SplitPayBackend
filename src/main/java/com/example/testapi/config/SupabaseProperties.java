package com.example.testapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.supabase")
public class SupabaseProperties {

    private String url;
    private String serviceRoleKey;
    private final Storage storage = new Storage();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    public void setServiceRoleKey(String serviceRoleKey) {
        this.serviceRoleKey = serviceRoleKey;
    }

    public Storage getStorage() {
        return storage;
    }

    public static class Storage {

        private String avatarBucket = "avatars";

        public String getAvatarBucket() {
            return avatarBucket;
        }

        public void setAvatarBucket(String avatarBucket) {
            this.avatarBucket = avatarBucket;
        }
    }
}