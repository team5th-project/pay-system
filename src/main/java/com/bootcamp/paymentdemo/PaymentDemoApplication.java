package com.bootcamp.paymentdemo;

import com.bootcamp.paymentdemo.common.config.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class PaymentDemoApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PaymentDemoApplication.class);
        app.addInitializers(new DotenvInitializer());
        app.run(args);
    }
}
