package com.uzak;

import com.uzak.redis.LimitSection;
import com.uzak.thread.CountDownActuate;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: liangxiudou
 * @Date: 2020/3/22 16:53
 * @Description:
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class Test {

    @org.junit.Test
    public void testThread() throws ExecutionException, InterruptedException {
        CountDownActuate actuate = new CountDownActuate();
        actuate.addTkR(() -> {
            try {
                Thread.sleep(10000);
                System.out.println("tkR1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Future future = actuate.addTkC(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("tkC1");
            return 1;
        });

        actuate.start();
        System.out.println(future.get());
    }

    @org.junit.Test
    public void testLimit() throws InterruptedException {
        LimitSection section = new LimitSection("phone", 5L, 1L, TimeUnit.MINUTES);

        for (int i = 0; i < 130; i++) {
            Thread.sleep(1000);
            System.out.println(section.getPass("136") ? "Pass" : "Unpass");
        }
    }
}
