package com.cainsgl.user.misc;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
@Deprecated()
public class IncrSessionCallback implements SessionCallback<Boolean> {

    String followerRedisKey;
    String followeeRedisKey;
    long delta;
    public IncrSessionCallback(String followerRedisKey,String followeeRedisKey,long delta)
    {
        this.followerRedisKey=followerRedisKey;
        this.followeeRedisKey=followeeRedisKey;
        this.delta=delta;
    }

    @Override
    public Boolean execute(RedisOperations operations) throws DataAccessException {
        operations.multi();
        var opsForValue = operations.opsForHash();
        opsForValue.increment(followerRedisKey,"followingCount",delta);
        opsForValue.increment(followeeRedisKey,"followerCount",delta);
        return !operations.exec().isEmpty();
    }
}
