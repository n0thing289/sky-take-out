package com.sky.utils;

import org.springframework.util.ObjectUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 缓存数据通用类
 */
public class CacheUtil {

    /**
     * 带有缓存的工具方法
     * @param cacheSelector
     * @param databaseSelector
     * @param cacheSave
     * @param <T>
     * @return
     */
    public static <T> T getCacheData(Supplier<T> cacheSelector, Supplier<T> databaseSelector, Consumer<T> cacheSave){
        //从redis查
        T data = cacheSelector.get();
        //如果redis没查到
        if(ObjectUtils.isEmpty(data)){
            //从数据库查
            data = databaseSelector.get();
            //数据库查到了数据
            if(!ObjectUtils.isEmpty(data)){
                //把数据放入redis
                cacheSave.accept(data);
            }
        }
        //返回数据
        return data;
    }
}
