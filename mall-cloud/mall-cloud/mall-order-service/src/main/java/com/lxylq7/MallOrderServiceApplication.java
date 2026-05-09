package com.lxylq7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.lxylq7.client")
@SpringBootApplication
public class MallOrderServiceApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(MallOrderServiceApplication.class,args);
    }
}
