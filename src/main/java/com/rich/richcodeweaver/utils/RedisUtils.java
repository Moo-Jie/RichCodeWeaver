package com.rich.richcodeweaver.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis 工具包类
 * 提供丰富的Redis操作方法和工具函数
 *
 * @author yupi
 * @date 2023/10/15
 */
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据对象生成缓存key (JSON + MD5)
     *
     * @param obj 要生成key的对象
     * @return MD5哈希后的缓存key
     */
    public static String genKey(Object obj) {
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // 先转换为 JSON 字符串，再加密为 MD5，保证唯一性
        return "rcw-" + DigestUtil.md5Hex(JSONUtil.toJsonStr(obj));
    }

    /**
     * 生成带前缀的缓存key
     *
     * @param prefix 前缀
     * @param keys   键值部分
     * @return 完整的缓存key
     */
    public static String genKeyWithPrefix(String prefix, Object... keys) {
        String keyStr = Arrays.stream(keys)
                .map(Object::toString)
                .collect(Collectors.joining(":"));
        return StrUtil.isBlank(prefix) ? keyStr : prefix + ":" + keyStr;
    }

    /**
     * 生成带命名空间的缓存key
     *
     * @param namespace 命名空间
     * @param business  业务标识
     * @param key       键值
     * @return 完整的缓存key
     */
    public static String generateNamespacedKey(String namespace, String business, Object key) {
        return String.format("%s:%s:%s", namespace, business, key.toString());
    }


    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return 是否成功
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.stream(key).collect(Collectors.toList()));
            }
        }
    }

    /**
     * 批量删除key
     *
     * @param keys key集合
     */
    public void del(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 删除匹配模式的key
     *
     * @param pattern 匹配模式，如 "user:*"
     */
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return 增加后的值
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return 减少后的值
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    /**
     * 查询缓存，如果不存在则通过dbSupplier获取并设置缓存
     *
     * @param key        缓存key
     * @param dbSupplier 数据库查询函数
     * @param time       缓存时间(秒)
     * @param <T>        返回值类型
     * @return 数据
     */
    public <T> T getWithFallback(String key, Supplier<T> dbSupplier, long time) {
        T value = (T) get(key);
        if (value != null) {
            return value;
        }

        // 使用双重检查锁防止缓存击穿
        synchronized (this) {
            value = (T) get(key);
            if (value != null) {
                return value;
            }

            value = dbSupplier.get();
            if (value != null) {
                set(key, value, time);
            }
        }
        return value;
    }

    /**
     * 缓存更新工具 - 先更新数据库再删除缓存
     *
     * @param key        缓存key
     * @param updateTask 数据库更新任务
     * @return 是否成功
     */
    public boolean updateWithCacheAside(String key, Runnable updateTask) {
        try {
            // 先更新数据库
            updateTask.run();
            // 再删除缓存
            del(key);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return Set集合
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取分布式锁
     *
     * @param key        锁的key
     * @param value      锁的值（建议使用UUID+线程ID）
     * @param expireTime 锁的过期时间(秒)
     * @return 是否获取成功
     */
    public boolean tryLock(String key, String value, long expireTime) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁
     *
     * @param key   锁的key
     * @param value 锁的值
     * @return 是否释放成功
     */
    public boolean releaseLock(String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), value);
        return result != null && result > 0;
    }

    /**
     * 获取Redis分布式自增ID
     *
     * @param key 业务键
     * @return 自增ID
     */
    public long generateId(String key) {
        String idKey = "id_generator:" + key;
        return redisTemplate.opsForValue().increment(idKey);
    }

    /**
     * 添加元素到HyperLogLog
     *
     * @param key    键
     * @param values 值
     * @return 是否成功
     */
    public boolean pfAdd(String key, Object... values) {
        try {
            redisTemplate.opsForHyperLogLog().add(key, values);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取HyperLogLog基数估算值
     *
     * @param key 键
     * @return 基数估算值
     */
    public long pfCount(String key) {
        return redisTemplate.opsForHyperLogLog().size(key);
    }

    /**
     * 管道批处理操作
     *
     * @param action Redis回调操作
     * @return 执行结果
     */
    public List<Object> executePipelined(RedisCallback action) {
        return redisTemplate.executePipelined(action);
    }

    /**
     * 批量获取值
     *
     * @param keys key列表
     * @return 值列表
     */
    public List<Object> multiGet(List<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 获取键数量统计
     *
     * @param pattern 匹配模式
     * @return 键数量
     */
    public long keyCount(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys.size() : 0;
    }
}