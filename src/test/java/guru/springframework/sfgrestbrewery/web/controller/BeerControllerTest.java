package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebFluxTest(BeerController.class)
public class BeerControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    BeerService beerService;

    BeerDto validBeer;



    public static final String BEER_UPC = "12324";

    @BeforeEach
    void setUp() {
        validBeer = BeerDto.builder()
                           .beerName("elo")
                           .beerStyle("Apa")
                           .upc(BEER_UPC)
                           .build();

    }

    @Test
    void  getBeerById()  {
        Integer beerId = 1;
        given(beerService.getById(any(), any())).willReturn(Mono.just(validBeer));

        webTestClient.get()
                     .uri("/api/v1/beer/" + beerId)
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(BeerDto.class)
                     .value(beerDto -> beerDto.getBeerName(), equalTo(validBeer.getBeerName()));
    }

    @Test
    void getBeerByUPC() {
        given(beerService.getByUpc(any())).willReturn(Mono.just(validBeer));

        webTestClient.get()
                     .uri("/api/v1/beerUpc/"+BEER_UPC)
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(BeerDto.class)
                     .value(beerDto -> beerDto.getUpc(), equalTo(validBeer.getUpc()));
    }

    @Test
    void listBeers() {

        BeerPagedList beerList = new BeerPagedList(List.of(validBeer));

        given(beerService.listBeers(any(),any(),any(),any())).willReturn(Mono.just(beerList));

        webTestClient.get()
                     .uri("/api/v1/beer")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(BeerPagedList.class)
                     .value( beerPagedList -> beerPagedList.getTotalElements(), equalTo(1l));
    }
}
