package com.example.redisdistributedlock.controller;

import com.example.redisdistributedlock.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private DefaultRedisScript<Long> script;

    @PostConstruct
    public void init() {
        script = new DefaultRedisScript<Long>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("util.lua")));
    }

    @PostMapping(value = "/addUser")
    public String createOrder(@RequestBody User user) {

        String key = user.getUsername();
        String value = UUID.randomUUID().toString().replace("-", "");

        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, value, 10000, TimeUnit.MILLISECONDS);
        if (flag != null && flag) {
            log.info("Get LOCK: {} successfully", key);
            try {
                Thread.sleep(1000 * 15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String lockValue = (String) redisTemplate.opsForValue().get(key);
            if (lockValue != null && lockValue.equals(value)) {
                // redisTemplate.delete(key);

                List<String> keys = new ArrayList<>();
                keys.add(key);
                redisTemplate.execute(script, keys, lockValue);

                log.info("Unlock {} successfully", key);
            }
            return "SUCCESS";
        } else {
            log.info("Get LOCK: {} fail", key);
            return "FAIL";
        }

    }
}