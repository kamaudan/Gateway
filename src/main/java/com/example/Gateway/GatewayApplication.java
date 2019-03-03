package com.example.Gateway;


import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.stream.IntStream;

@SpringBootApplication
public class GatewayApplication {

    @Bean
    ApplicationRunner client(){
        return  args -> {
            WebClient client = WebClient.builder().filter(ExchangeFilterFunctions.basicAuthentication("dan", "3274")).build();

            Flux.fromStream(IntStream.range(1, 100).boxed())
                    .flatMap( number -> client.get().uri("http://localhost:8081/rl").exchange())
                    .flatMap(clientResponse -> clientResponse.toEntity(String.class).map( re -> String.format("status : %s;  body : %s", re.getStatusCodeValue(), re.getBody())))
                    .subscribe(System.out::println);

        };
    }



    @Bean
    public RouteLocator gateway(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter ){
        return builder.routes()

                .route("path_role", r -> r.path("/requisition")
                        .uri("localhost:8081/Requisition"))

                .route("host_route", r -> r.host("*.dankamau.org")
                        .uri("http://httpbin.org:80"))

                .route("rewrite_route", r -> r.host("*.rewrite.org")
                        .filters(f -> f.rewritePath("/foo/(?<segment>.*)", "/${segment}"))
                        .uri("http://httpbin.org:80"))

                .route("hystrix_route", r -> r.host("*.hystrix.org")
                        .filters(f -> f.hystrix( c -> c.setName("slowcmd")))
                        .uri("http://httpbin.org:80"))

                .route("hystrix_fallback_route", r -> r.host("*.hystrixfallback.org")
                        .filters( f -> f.hystrix(c -> c.setName("slowcmd").setFallbackUri("forward:/hystrixfallback")))
                        .uri("http://httpbin.org:80"))
                .route("limit_route", r -> r.host("*.limited.org")
                        .and().path("/anything/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter)))
                        .uri("http://httpbin.org:80"))
                .build();






    }






	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}


	}



