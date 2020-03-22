package com.uzak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Auther: liangxiudou
 * @Date: 2020/3/22 17:40
 * @Description:
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        try {
            SpringApplication.run(Application.class, args);
        }catch (Throwable t){
            t.printStackTrace();
        }
    }
}
