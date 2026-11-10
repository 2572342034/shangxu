package com.shangxu.crm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 商序云 · 销售运营一体化平台启动类
 */
@EnableAsync
@EnableTransactionManagement
@MapperScan({"com.shangxu.crm.**.mapper", "com.shangxu.crm.common.sequence"})
@SpringBootApplication
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
