

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;
import io.vertx.howtos.openapi.APIVerticle;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(VertxExtension.class)
public class OpenApiAndTest {


  @Test
  public void testAllSchemesAuthenticated(Vertx vertx, VertxTestContext testCtx) {
    AtomicBoolean scheme1CredsParsed = new AtomicBoolean(false);
    AtomicBoolean scheme1Authenticated = new AtomicBoolean(false);
    AtomicBoolean scheme2CredsParsed = new AtomicBoolean(false);
    AtomicBoolean scheme2Authenticated = new AtomicBoolean(false);

    APIVerticle verticle = new APIVerticle(
      new FoobarAuth(
        "scheme1",
        scheme1CredsParsed,
        scheme1Authenticated
      ),
      new FoobarAuth(
        "scheme2",
        scheme2CredsParsed,
        scheme2Authenticated
      ));
    vertx.deployVerticle(verticle)
      .onSuccess(s -> {
        HttpClient client = vertx.createHttpClient();

        client.request(HttpMethod.GET, 8080, "localhost", "/pets")
          .onComplete(testCtx.succeeding(buffer -> testCtx.verify(() -> {
            if (!scheme1CredsParsed.get()) {
              testCtx.failNow("Scheme 1 credentials not parsed.");
            }
            if (!scheme1Authenticated.get()) {
              testCtx.failNow("Scheme 1 authentication did not happen.");
            }
            if (!scheme2CredsParsed.get()) {
              testCtx.failNow("Scheme 2 credentials not parsed.");
            }
            if (!scheme2Authenticated.get()) {
              testCtx.failNow("Scheme 2 authentication did not happen.");
            }
            testCtx.completeNow();
          })));
      });
  }

  private static class FoobarAuth extends AuthenticationHandlerImpl<FoobarAuth> implements AuthenticationProvider {

    final String id;
    final AtomicBoolean parseReached;
    final AtomicBoolean authReached;

    public FoobarAuth(String id, AtomicBoolean parseReached, AtomicBoolean authReached) {
      super(null, "");
      this.id = id;
      this.parseReached = parseReached;
      this.authReached = authReached;
    }

    @Override
    public void parseCredentials(RoutingContext routingContext, Handler<AsyncResult<Credentials>> handler) {
      parseReached.set(true);
      handler.handle(Future.succeededFuture(new TokenCredentials(id)));
    }

    @Override
    public void authenticate(JsonObject jsonObject, Handler<AsyncResult<User>> handler) {
      authReached.set(true);
      User user = User.create(new JsonObject(), new JsonObject().put(id, id));
      handler.handle(Future.succeededFuture(user));
    }

    @Override
    protected AuthenticationProvider getAuthProvider(RoutingContext ctx) {
      return this;
    }
  }
}
