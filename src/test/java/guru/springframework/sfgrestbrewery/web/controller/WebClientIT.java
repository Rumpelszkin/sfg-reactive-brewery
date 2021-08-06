package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
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

/**
 * Created by jt on 3/7/21.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientIT {

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
                                             .uri("api/v1/beer/1")
                                             .accept(MediaType.APPLICATION_JSON)
                                             .retrieve()
                                             .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {
            assertThat(beer).isNotNull();
            assertThat(beer.getBeerName()).isNotNull();

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByUpc() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get()
                                             .uri("api/v1/beerUpc/" + BeerLoader.BEER_1_UPC)
                                             .accept(MediaType.APPLICATION_JSON)
                                             .retrieve()
                                             .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beer -> {
            assertThat(beer).isNotNull();
            assertThat(beer.getBeerName()).isNotNull();

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testListBeers() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerPagedList> beerPagedListMono = webClient.get()
                                                         .uri("/api/v1/beer")
                                                         .accept(MediaType.APPLICATION_JSON)
                                                         .retrieve()
                                                         .bodyToMono(BeerPagedList.class);

        beerPagedListMono.publishOn(Schedulers.parallel())
                         .subscribe(beerPagedList -> {

                             beerPagedList.getContent()
                                          .forEach(beerDto -> System.out.println(beerDto.toString()));

                             countDownLatch.countDown();
                         });

        countDownLatch.await();
    }

    @Test
    void testSavedBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                                 .beerName("Rumpis favs")
                                 .beerStyle("IPA")
                                 .price(new BigDecimal("6.99"))
                                 .upc("123123321")
                                 .build();

        Mono<ResponseEntity<Void>> responseEntityMono = webClient.post()
                                                                 .uri("/api/v1/beer")
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

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testSavedBeerBadRequest() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                                 .price(new BigDecimal("6.99"))
                                 .upc("123123321")
                                 .build();

        Mono<ResponseEntity<Void>> responseEntityMono = webClient.post()
                                                                 .uri("/api/v1/beer")
                                                                 .accept(MediaType.APPLICATION_JSON)
                                                                 .body(BodyInserters.fromValue(beerDto))
                                                                 .retrieve()
                                                                 .toBodilessEntity();

        responseEntityMono.publishOn(Schedulers.parallel())
                          .doOnError(throwable -> {
                              countDownLatch.countDown();
                          })
                          .subscribe(responseEntity -> {

                          });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        webClient.get()
                 .uri("/api/v1/beer")
                 .retrieve()
                 .bodyToMono(BeerPagedList.class)
                 .publishOn(Schedulers.single())
                 .subscribe(pagedList -> {
                     countDownLatch.countDown();

                     BeerDto beerDto = pagedList.getContent()
                                                .get(0);

                     BeerDto updatedPayload = BeerDto.builder()
                                                     .beerName("New Name Son")
                                                     .beerStyle(beerDto.getBeerStyle())
                                                     .upc(beerDto.getUpc())
                                                     .price(beerDto.getPrice())
                                                     .build();
                     webClient.put()
                              .uri("/api/v1/beer/" + beerDto.getId())
                              .contentType(MediaType.APPLICATION_JSON)
                              .body(BodyInserters.fromValue(updatedPayload))
                              .retrieve()
                              .toBodilessEntity()
                              .flatMap(responseEntity -> {
                                  countDownLatch.countDown();
                                  return webClient.get()
                                                  .uri("/api/v1/beer/" + beerDto.getId())
                                                  .accept(MediaType.APPLICATION_JSON)
                                                  .retrieve()
                                                  .bodyToMono(BeerDto.class);
                              })
                              .subscribe(savedDto -> {
                                  assertThat(savedDto.getBeerName()).isEqualTo("New Name Son");
                                  countDownLatch.countDown();
                              });

                 });
        countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeerNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);

        BeerDto beerDto = BeerDto.builder()
                                 .beerName("Test Beer")
                                 .beerStyle("PALE_ALE")
                                 .upc("31415")
                                 .price(new BigDecimal("4.50"))
                                 .build();

        webClient.put()
                 .uri("/api/v1/beer/" + 15432)
                 .contentType(MediaType.APPLICATION_JSON)
                 .body(BodyInserters.fromValue(beerDto))
                 .retrieve()
                 .toBodilessEntity()
                 .subscribe(responseEntity -> {
                 }, throwable -> {
                     if (throwable.getClass()
                                  .getName()
                                  .equals("org.springframework.web.reactive.function.client.WebClientResponseException$NotFound")) {
                         WebClientResponseException ex = (WebClientResponseException) throwable;
                         if (ex.getStatusCode()
                               .equals(HttpStatus.NOT_FOUND)) {
                             countDownLatch.countDown();
                         }
                     }

                 });

        countDownLatch.countDown();

        countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }
}
