package com.uzak.limit;

import com.uzak.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: liangxiudou
 * @Date: 2020/3/8 10:27
 * @Description: 限流类，适用于时间粒度比较小的场景下
 * 不适用于滑动时间区间内允许次数较多的场景
 */
public class LimitSection {
    private static final String LIMIT_KEY_PREFIX = "limit:";

    //限流类型
    private final String type;
    //时间段内的限流次数
    private final Long count;
    //限流时间段长度
    private final Long timeNanos;

    public LimitSection(String type, Long count, Long period, TimeUnit timeUnit) {
        this.type = LIMIT_KEY_PREFIX + type;
        this.count = count;
        this.timeNanos = timeUnit.toNanos(period);
    }


    public void pass(String key) {
        Assert.isTrue(getPass(key), "操作过于频繁，请稍后再试！");
    }

    public boolean getPass(String key) {
        return LimitUtil.getPass(this, key);
    }

    @Component
    private static class LimitUtil {

        private static RedisTemplate<String, String> redisTemplate;

        private static ExecutorService executorService = Executors.newSingleThreadExecutor();

        /**
         * @param redisTemplate
         */
        @Autowired
        public void setJedisPool(RedisTemplate<String, String> redisTemplate) {
            LimitUtil.redisTemplate = redisTemplate;
        }

        /**
         * 每个长度为time的时间滑动窗口内的执行次数不超过count
         * 基于redis sortedset，
         *
         * @param key 限制某参数请求的次数，如果为空则限制type的次数
         * @return
         */
        static boolean getPass(LimitSection section, String key) {
            SessionCallback<Boolean> sessionCallback = new SessionCallback<Boolean>() {
                @Override
                public Boolean execute(RedisOperations redisOperations) throws DataAccessException {
                    String realKey = section.type + (StringUtil.isBlank(key) ? "" : ":" + key);
                    double nowTimeTemp = (double) System.nanoTime();
                    double lowerTimeTemp = nowTimeTemp - section.timeNanos;
                    redisOperations.multi();
                    try {
                        String member = UUID.randomUUID().toString();
                        //新增一个以当前时间戳为分数的元素进去
                        redisOperations.opsForZSet().add(realKey, member, nowTimeTemp);
                        //查询zset中指定分数区间内的数量
                        redisOperations.opsForZSet().count(realKey, lowerTimeTemp, nowTimeTemp);
                        List<Object> result = redisTemplate.exec();
                        //如果数量大于限制数量返回失败
                        if (result.size() != 2 || (result.get(1) == null || (long) result.get(1) <= section.count)) {
                            return true;
                        } else {
                            redisOperations.opsForZSet().remove(realKey, member);
                            return false;
                        }
                    } finally {
                        executorService.submit(() -> redisTemplate.opsForZSet().removeRangeByScore(realKey, 0, lowerTimeTemp - 60 * 1000));
                    }
                }
            };
            return Optional.ofNullable(redisTemplate.execute(sessionCallback)).orElse(false);
        }
    }
}
