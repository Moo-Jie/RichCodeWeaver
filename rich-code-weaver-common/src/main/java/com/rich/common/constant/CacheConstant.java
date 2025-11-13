package com.rich.common.constant;

import java.time.Duration;

public class CacheConstant {

    /**
     * 默认缓存过期时间
     **/
    public static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * HOT KEY 接口的缓存过期时间
     **/
    public static final Duration HOT_KEY_CACHE_TTL = Duration.ofMinutes(100);

    /**
     * 星标应用接口的缓存名称
     **/
    public static final String STAR_APP_CACHE_NAME = "star_app_page";
}
