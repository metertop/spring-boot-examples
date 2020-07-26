package com;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.support.SpringBootServletInitializer;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/24 12:21
 */
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, MybatisAutoConfiguration.class, DruidDataSourceAutoConfigure.class, RabbitAutoConfiguration.class})
public class Application  {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }
}
