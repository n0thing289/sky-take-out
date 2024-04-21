package com.sky.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Resource(name = "myRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public Object getList(String key){
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void expire(String key, Long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    public boolean isExists(String token) {
        if (token != null) {
            Boolean hasKey = redisTemplate.hasKey(token);
            return Boolean.TRUE.equals(hasKey);
        }
        return false;
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Set<String> showCache() {
        return redisTemplate.keys("*");
    }

    public void setex(String key, Object value, Long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.MILLISECONDS);
    }

    public void del(String key) {
        redisTemplate.delete(key);
    }
}
