package com.rich.richcodeweaverserveradmin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// 启动 spring boot admin server
@EnableAdminServer
public class RichCodeWeaverServerAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(RichCodeWeaverServerAdminApplication.class, args);
    }

}
