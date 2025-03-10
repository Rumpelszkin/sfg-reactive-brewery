package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.functional.BeerHandlerV2;
import guru.springframework.sfgrestbrewery.web.functional.BeerRouterConfiguration;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientV2IT {
    public static final String BASE_URL = "http://localhost:8080";

    WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebClient.builder()
                             .baseUrl(BASE_URL)
                             .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                                                                                       .wiretap(true)))
                             .build();
    }

    @Test
    void getBeerById() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get()
                                             .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + 1)
                                             .accept(MediaType.APPLICATION_JSON)
                                             .retrieve()
                                             .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {
            assertThat(beer).isNotNull();
            assertThat(beer.getBeerName()).isNotNull();
            countDownLatch.countDown();
        });

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByIdNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get()
                                             .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + 1777)
                                             .accept(MediaType.APPLICATION_JSON)
                                             .retrieve()
                                             .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {

        }, throwable -> {
            countDownLatch.countDown();
        });
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByUpc() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get()
                                             .uri(BeerRouterConfiguration.BEER_V2_UPC + "/" + BeerLoader.BEER_7_UPC)
                                             .accept(MediaType.APPLICATION_JSON)
                                             .retrieve()
                                             .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {
            assertThat(beer).isNotNull();
            assertThat(beer.getBeerName()).isNotNull();
            countDownLatch.countDown();
        });

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByUpcNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get()
                                             .uri(BeerRouterConfiguration.BEER_V2_UPC + "/" + 1777)
                                             .accept(MediaType.APPLICATION_JSON)
                                             .retrieve()
                                             .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {

        }, throwable -> {
            countDownLatch.countDown();
        });
        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testSaveBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                                 .beerName("PissWasser")
                                 .beerStyle("IPA")
                                 .price(new BigDecimal("11.20"))
                                 .upc("123321231")
                                 .build();

        Mono<ResponseEntity<Void>> responseEntityMono = webClient.post()
                                                                 .uri(BeerRouterConfiguration.BEER_V2_URL)
                                                                 .accept(MediaType.APPLICATION_JSON)
                                                                 .body(BodyInserters.fromValue(beerDto))
                                                                 .retrieve()
                                                                 .toBodilessEntity();

        responseEntityMono.publishOn(Schedulers.parallel())
                          .subscribe(responseEntity -> {
                              assertThat(responseEntity.getStatusCode()
                                                       .is2xxSuccessful());
                              countDownLatch.countDown();
                          });

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testSaveBeerBadRequest() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                                 .price(new BigDecimal("11.20"))
                                 .build();

        Mono<ResponseEntity<Void>> responseEntityMono = webClient.post()
                                                                 .uri(BeerRouterConfiguration.BEER_V2_URL)
                                                                 .accept(MediaType.APPLICATION_JSON)
                                                                 .body(BodyInserters.fromValue(beerDto))
                                                                 .retrieve()
                                                                 .toBodilessEntity();

        responseEntityMono.publishOn(Schedulers.parallel())
                          .subscribe(responseEntity -> {

                          }, throwable -> {
                              if (throwable.getClass()
                                           .getName()
                                           .equals("org.springframework.web.reactive.function.client.WebClientResponseException$BadRequest")) {
                                  WebClientResponseException ex = (WebClientResponseException) throwable;
                                  if (ex.getStatusCode()
                                        .equals(HttpStatus.BAD_REQUEST)) {
                                      countDownLatch.countDown();
                                  }
                              }
                          });

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeer() throws InterruptedException {
        final String newBeerName = "New Beer Name";
        final Integer beerId = 1;
        CountDownLatch countDownLatch = new CountDownLatch(2);

        webClient.put()
                 .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                 .accept(MediaType.APPLICATION_JSON)
                 .body(BodyInserters.fromValue(BeerDto.builder()
                                                      .beerName(newBeerName)
                                                      .upc("1233455")
                                                      .beerStyle("PALE_ALE")
                                                      .price(new BigDecimal("10.99"))
                                                      .build()))
                 .retrieve()
                 .toBodilessEntity()
                 .subscribe(responseEntity -> {
                     assertThat(responseEntity.getStatusCode()
                                              .is2xxSuccessful());
                     countDownLatch.countDown();
                 });
        //wait for update
        countDownLatch.await(500, TimeUnit.MILLISECONDS);

        webClient.get()
                 .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                 .accept(MediaType.APPLICATION_JSON)
                 .retrieve()
                 .bodyToMono(BeerDto.class)
                 .subscribe(beer -> {
                     assertThat(beer).isNotNull();
                     assertThat(beer.getBeerName()).isNotNull();
                     assertThat(beer.getBeerName()).isEqualTo(newBeerName);
                     countDownLatch.countDown();
                 });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeerNotFound() throws InterruptedException {
        final String newBeerName = "New Beer Name";
        final Integer beerId = 1000;
        CountDownLatch countDownLatch = new CountDownLatch(1);

        webClient.put()
                 .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                 .accept(MediaType.APPLICATION_JSON)
                 .body(BodyInserters.fromValue(BeerDto.builder()
                                                      .beerName(newBeerName)
                                                      .upc("1233455")
                                                      .beerStyle("PALE_ALE")
                                                      .price(new BigDecimal("10.99"))
                                                      .build()))
                 .retrieve()
                 .toBodilessEntity()
                 .subscribe(responseEntity -> {
                     assertThat(responseEntity.getStatusCode()
                                              .is2xxSuccessful());
                 }, throwable -> {
                     countDownLatch.countDown();
                 });
        //wait for update

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testDeleteBeerById() throws InterruptedException {
        Integer beerId = 3;
        CountDownLatch countDownLatch = new CountDownLatch(2);

        webClient.delete()
                 .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                 .retrieve()
                 .toBodilessEntity()
                 .flatMap(responseEntity -> {
                     countDownLatch.countDown();

                     return webClient.get()
                                     .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                                     .accept(MediaType.APPLICATION_JSON)
                                     .retrieve().bodyToMono(BeerDto.class);
                 }).subscribe(savedDto -> {

        }, throwable -> {
                     countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testDeleteBeerByIdNotFound() throws InterruptedException {
        Integer beerId = 4;

        webClient.delete()
                 .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                 .retrieve()
                 .toBodilessEntity().block();
        assertThrows(WebClientResponseException.NotFound.class, () -> {
            webClient.delete()
                     .uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                     .retrieve()
                     .toBodilessEntity().block();
        });
    }
}
