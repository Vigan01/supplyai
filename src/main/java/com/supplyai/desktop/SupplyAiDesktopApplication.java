package com.supplyai.desktop;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.supplyai.SupplyAiApplication;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ConfigurableApplicationContext;

public class SupplyAiDesktopApplication extends Application {

    private ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        DesktopPortListener portListener = new DesktopPortListener();
        SpringApplication application = new SpringApplicationBuilder(SupplyAiApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=0")
                .listeners(event -> {
                    if (event instanceof WebServerInitializedEvent webServerEvent) {
                        portListener.onWebServerReady(webServerEvent);
                    }
                })
                .build();

        springContext = application.run();
        int port = portListener.awaitPort();

        WebView webView = new WebView();
        webView.getEngine().load("http://127.0.0.1:" + port + "/");

        stage.setTitle("SupplyAI");
        stage.setMinWidth(1180);
        stage.setMinHeight(760);
        stage.setScene(new Scene(webView, 1280, 820));
        stage.show();
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }

    static class DesktopPortListener {

        private final AtomicInteger port = new AtomicInteger();
        private final CountDownLatch ready = new CountDownLatch(1);

        void onWebServerReady(WebServerInitializedEvent event) {
            port.set(event.getWebServer().getPort());
            ready.countDown();
        }

        int awaitPort() throws InterruptedException {
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Der interne App-Server konnte nicht gestartet werden.");
            }
            return port.get();
        }
    }
}
