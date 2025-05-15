package com.etw4s.twitchchatlink.mixin;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.twitch.auth.AuthManager;
import com.etw4s.twitchchatlink.twitch.eventsub.EventSubClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClinetMixin {

  @Unique
  Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);

  @Shadow
  @Nullable
  public ClientWorld world;

  private AuthManager authManager;

  public MinecraftClinetMixin() {
    this.authManager = new AuthManager();
  }

  @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"))
  private void onDisconnect(Screen disconnectionScreen, boolean transferring, CallbackInfo ci) {
    if (world == null) {
      return;
    }
    LOGGER.info("onDisconnect");
    CompletableFuture.runAsync(() -> {
      authManager.stopAuth();
    });
    EventSubClient.getInstance().disconnect();
  }
}
