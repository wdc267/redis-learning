package com.hmdp;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class HmDianPingApplicationTests {
    @Autowired
    private ShopServiceImpl shopService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    // 在类的成员变量位置（定义 es）
    private static final ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    public void testSaveShop(){
        shopService.saveShop2Redis(2L, 2L);
    }

    @Test
    public void testStartDateTime(){
        //设置一下起始时间，时间戳就是起始时间与当前时间的秒数差
        LocalDateTime tmp = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        System.out.println(tmp.toEpochSecond(ZoneOffset.UTC));
    }

    @Test
    public void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

}
