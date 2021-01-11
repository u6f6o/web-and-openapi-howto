package io.vertx.howtos.openapi;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class APIVerticle extends AbstractVerticle {

  private HttpServer server;
  private AuthenticationHandler foobarAuth1;
  private AuthenticationHandler foobarAuth2;

  public APIVerticle(AuthenticationHandler foobarAuth1, AuthenticationHandler foobarAuth2) {
    this.foobarAuth1 = foobarAuth1;
    this.foobarAuth2 = foobarAuth2;
  }

  final List<JsonObject> pets = new ArrayList<>(Arrays.asList(
    new JsonObject().put("id", 1).put("name", "Fufi").put("tag", "ABC"),
    new JsonObject().put("id", 2).put("name", "Garfield").put("tag", "XYZ"),
    new JsonObject().put("id", 3).put("name", "Puffa")
  ));

  @Override
  public void start(Promise<Void> startPromise) {
    RouterBuilder.create(this.vertx, "petstore.yaml")
      .onSuccess(routerBuilder -> {
        // Add routes handlers
        // tag::listPetsHandler[]

        routerBuilder.securityHandler("FoobarAuth1", foobarAuth1);
        routerBuilder.securityHandler("FoobarAuth2", foobarAuth2);

        routerBuilder.operation("listPets").handler(routingContext ->
          routingContext
            .response() // <1>
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json") // <2>
            .end(new JsonArray(getAllPets()).encode()) // <3>
        );

        Router router = routerBuilder.createRouter(); // <1>
        router.errorHandler(404, routingContext -> { // <2>
          JsonObject errorObject = new JsonObject() // <3>
            .put("code", 404)
            .put("message",
              (routingContext.failure() != null) ?
                routingContext.failure().getMessage() :
                "Not Found"
            );
          routingContext
            .response()
            .setStatusCode(404)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errorObject.encode()); // <4>
        });
        router.errorHandler(400, routingContext -> {
          JsonObject errorObject = new JsonObject()
            .put("code", 400)
            .put("message",
              (routingContext.failure() != null) ?
                routingContext.failure().getMessage() :
                "Validation Exception"
            );
          routingContext
            .response()
            .setStatusCode(400)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errorObject.encode());
        });

        server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost")); // <5>
        server.requestHandler(router).listen(); // <6>
        // end::routerGen[]
        startPromise.complete(); // Complete the verticle start
      })
      .onFailure(startPromise::fail);
  }

  @Override
  public void stop(){
    this.server.close();
  }

//  public static void main(String[] args) {
//    Vertx vertx = Vertx.vertx();
//    vertx.deployVerticle(new APIVerticle());
//  }

  private List<JsonObject> getAllPets() {
    return this.pets;
  }

  private void addPet(JsonObject pet) {
    this.pets.add(pet);
  }

  // tag::loadSpecSampleMethod[]
  // For documentation purpose
  private void loadSpecSample(Promise<Void> startPromise) {
    // tag::loadSpec[]
    RouterBuilder.create(this.vertx, "petstore.yaml")
      .onSuccess(routerBuilder -> { // <1>
        // You can start building the router using routerBuilder
      }).onFailure(cause -> { // <2>
      // Something went wrong during router factory initialization
      startPromise.fail(cause);
    });
    // end::loadSpec[]
  }
  // end::loadSpecSampleMethod[]
}
