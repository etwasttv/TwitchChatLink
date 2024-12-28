package com.etw4s.twitchchatlink.twitch.auth;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuthRedirectServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);
  private static final AuthRedirectServer instance = new AuthRedirectServer();
  private static final int port = 3000;
  private volatile HttpServer server;

  private AuthRedirectServer() {
  }

  public static AuthRedirectServer getInstance() {
    return instance;
  }

  void startRedirectServer() throws IOException {
    synchronized (this) {
      if (server != null) {
        LOGGER.info("Redirect server is already running");
        return;
      }
      server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/", new RedirectHandler());
      server.setExecutor(null);
      server.start();
      LOGGER.info("Redirect server is started");
    }
  }

  void stopRedirectServer() {
    synchronized (this) {
      if (server == null) {
        LOGGER.info("Redirect server is not running");
        return;
      }

      server.stop(0);
      server = null;
      LOGGER.info("Redirect server is closed");
    }
  }

  String getRedirectUrl() {
    return "http://localhost:" + port;
  }
}
