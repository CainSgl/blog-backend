package com.cainsgl.test.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.cainsgl.test.entity.TestEntity;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/test")
public class TestCTL {

    @Resource
    private ElasticsearchClient elasticsearchClient;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @GetMapping("/es")
    public Map<String, Object> testEsIndexExists() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查索引是否存在
            boolean isExists = elasticsearchClient.indices()
                                                  .exists(b -> b.index("test-index"))
                                                  .value();
            // 封装返回结果
            result.put("success", true);
            result.put("exists", isExists);
            result.put("msg", "ElasticsearchClient 调用成功");
        } catch (Exception e) {
            // 捕获异常，方便排查问题
            result.put("success", false);
            result.put("msg", "ElasticsearchClient 调用失败：" + e.getMessage());
            result.put("errorDetail", e.getCause() != null ? e.getCause().getMessage() : "无详细原因");
        }
        return result;
    }

    @GetMapping("/redis")
    public  Map<String, Object>  testRedisTTL() {

        Map<String, Object> result = new HashMap<>();
       try{
           redisTemplate.opsForValue().set("testKey", "testValue", 30, TimeUnit.SECONDS);
           result.put("success", true);
       }catch (Exception e){
           result.put("success", false);
           result.put("msg", "ElasticsearchClient 调用失败：" + e.getMessage());
           result.put("errorDetail", e.getCause() != null ? e.getCause().getMessage() : "无详细原因");

       }
        // 仅返回最简单的提示
        return result;
    }
    @GetMapping("/lombok")
    public  TestEntity  testLombok() {
        TestEntity testEntity = new TestEntity();
        testEntity.setAge(17);
        testEntity.setName("test");
        return testEntity;
    }

}
