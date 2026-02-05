package com.example.helloworld;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.helloworld.web.HelloController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HelloWorldApplicationTests {

    @Autowired
    HelloController controller;

    @Test
    void contextLoads() {
        assertThat(controller).isNotNull();
    }
}
