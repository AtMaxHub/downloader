/*
package cn.go.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
*/
/**
 * WebScoket配置处理器
 */
/*

@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/ws").addInterceptors(new HttpSessionHandshakeInterceptor());
        registry.addHandler(myHandler(), "/ws/sockjs").addInterceptors(new HttpSessionHandshakeInterceptor()).withSockJS();
    }
 
    @Bean
    public WebsocketHandler myHandler() {
        return new WebsocketHandler();
    }
}
*/
