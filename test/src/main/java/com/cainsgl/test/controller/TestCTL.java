package com.cainsgl.test.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.cainsgl.common.service.TestService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/test")
@Slf4j
public class TestCTL {

    @Resource
    private ElasticsearchClient elasticsearchClient;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private TestService testService;


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

    @GetMapping("/logstash")
    public Object testlogstash(@RequestParam(required = false) String message) {
        log.error("测试日志: {}", message != null ? message : "此信息请忽略，仅用于测试logstash接受日志能力");
        return "success";
    }
    @GetMapping("/service")
    public Object testService() {
        return testService.getClass().getName();
    }
}
