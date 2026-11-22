package dev.shelfie.digest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

/**
 * One client per transport. Documents come from src/main/resources/graphql-documents/ (the default
 * location) by name: documentName("clubDigest"). The same interceptor is on both HTTP clients.
 */
@Configuration
public class Clients {
  @Bean
  HttpSyncGraphQlClient shelfieSync(@Value("${digest.shelfie-graphql}") String url, OperationStats stats) {
    return HttpSyncGraphQlClient.builder(RestClient.create(url)).interceptor(stats.sync()).build();
  }

  @Bean
  HttpGraphQlClient shelfieReactive(@Value("${digest.shelfie-graphql}") String url, OperationStats stats) {
    return HttpGraphQlClient.builder(WebClient.create(url)).interceptor(stats.reactive()).build();
  }

  @Bean
  WebSocketGraphQlClient chat(@Value("${digest.chat}") String url) {
    return WebSocketGraphQlClient.builder(url, new ReactorNettyWebSocketClient()).build();
  }
}
