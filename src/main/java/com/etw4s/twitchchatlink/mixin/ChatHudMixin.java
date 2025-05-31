package com.etw4s.twitchchatlink.mixin;

import com.etw4s.twitchchatlink.DrawContextExtension;
import com.etw4s.twitchchatlink.emote.EmoteManager;
import com.etw4s.twitchchatlink.emote.UnicodeMapper;
import com.etw4s.twitchchatlink.model.BaseEmoji;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.MessageIndicator.Icon;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

  private final UnicodeMapper unicodeMapper = new UnicodeMapper();

  @Shadow
  protected abstract boolean isChatHidden();

  @Shadow
  public abstract int getVisibleLineCount();

  @Shadow
  @Final
  private List<Visible> visibleMessages;

  @Shadow
  @Final
  private MinecraftClient client;

  @Shadow
  public abstract double getChatScale();

  @Shadow
  public abstract int getWidth();

  @Shadow
  protected abstract int getMessageIndex(double chatLineX, double chatLineY);

  @Shadow
  protected abstract double toChatLineX(double x);

  @Shadow
  protected abstract double toChatLineY(double y);

  @Shadow
  protected abstract int getLineHeight();

  @Shadow
  private int scrolledLines;

  @Shadow
  private static double getMessageOpacityMultiplier(int age) {
    return 0;
  }

  @Shadow
  protected abstract int getIndicatorX(Visible line);

  @Shadow
  protected abstract void drawIndicatorIcon(DrawContext context, int x, int y, Icon icon);

  @Shadow
  private boolean hasUnreadNewMessages;

  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  public void onRender(DrawContext context, int currentTick, int mouseX, int mouseY,
      boolean focused, CallbackInfo ci) {
    if (!this.isChatHidden()) {
      int i = this.getVisibleLineCount();
      int j = this.visibleMessages.size();
      if (j > 0) {
        this.client.getProfiler().push("chat");
        float f = (float) this.getChatScale();
        int k = MathHelper.ceil((float) this.getWidth() / f);
        int l = context.getScaledWindowHeight();
        context.getMatrices().push();
        context.getMatrices().scale(f, f, 1.0F);
        context.getMatrices().translate(4.0F, 0.0F, 0.0F);
        int m = MathHelper.floor((float) (l - 40) / f);
        int n = this.getMessageIndex(this.toChatLineX((double) mouseX),
            this.toChatLineY((double) mouseY));
        double d = this.client.options.getChatOpacity().getValue() * 0.8999999761581421
            + 0.10000000149011612;
        double e = this.client.options.getTextBackgroundOpacity().getValue();
        double g = this.client.options.getChatLineSpacing().getValue();
        int o = this.getLineHeight();
        int p = (int) Math.round(-8.0 * (g + 1.0) + 4.0 * g);
        int q = 0;

        int t;
        int u;
        int v;
        int x;
        for (int r = 0; r + this.scrolledLines < this.visibleMessages.size() && r < i; ++r) {
          int s = r + this.scrolledLines;
          ChatHudLine.Visible visible = this.visibleMessages.get(s);
          if (visible != null) {
            t = currentTick - visible.addedTime();
            if (t < 200 || focused) {
              double h = focused ? 1.0 : getMessageOpacityMultiplier(t);
              u = (int) (255.0 * h * d);
              v = (int) (255.0 * h * e);
              ++q;
              if (u > 3) {
                x = m - r * o;
                int y = x + p;
                context.fill(-4, x - o, k + 4 + 4, x, v << 24);
                MessageIndicator messageIndicator = visible.indicator();
                if (messageIndicator != null) {
                  int z = messageIndicator.indicatorColor() | u << 24;
                  context.fill(-4, x - o, -2, x, z);
                  if (s == n && messageIndicator.icon() != null) {
                    int aa = this.getIndicatorX(visible);
                    Objects.requireNonNull(this.client.textRenderer);
                    int ab = y + 9;
                    this.drawIndicatorIcon(context, aa, ab, messageIndicator.icon());
                  }
                }

                context.getMatrices().push();
                context.getMatrices().translate(0.0F, 0.0F, 50.0F);

                AtomicReference<Style> previousStyle = new AtomicReference<>(Style.EMPTY);
                AtomicReference<StringBuilder> builder = new AtomicReference<>(new StringBuilder());
                List<Text> texts = new ArrayList<>();
                visible.content().accept((index, style, code) -> {
                  if (!style.equals(previousStyle.get())) {
                    texts.add(Text.literal(builder.toString()).setStyle(previousStyle.get()));
                    builder.set(new StringBuilder());
                    previousStyle.set(style);
                  }
                  builder.get().append(Character.toString(code));
                  return true;
                });
                texts.add(Text.literal(builder.get().toString()).setStyle(previousStyle.get()));
                int tailX = 0;
                boolean isAfterEmote = false;
                for (Text text : texts) {
                  String str = text.getString();
                  String name = unicodeMapper.getNameByUnicode(str);
                  if (name != null) {
                    BaseEmoji emote = EmoteManager.getInstance().getEmojiByUnicode(str);
                    if (emote != null) {
                      // Emoji are already loaded
                      Identifier id = emote.getIdentifier();
                      try {
                        ((DrawContextExtension) context).twitchChatLink$drawTexture(id, tailX - o / 4,
                            (int) (y - 1 - 5 * g), o, o, 0f, 0f, o, o, o, o,
                            ColorHelper.Argb.withAlpha(u, -1));
                      } catch (Exception ex) {
                        ex.printStackTrace();
                      }
                      tailX += o;
                    } else {
                      // Emoji are loading
                      tailX = context.drawTextWithShadow(client.textRenderer,
                          Text.literal(name).setStyle(Style.EMPTY.withColor(
                              Formatting.GRAY)),
                          tailX, y,
                          ColorHelper.Argb.withAlpha(u, -1));
                    }
                    isAfterEmote = true;
                  } else {
                    var tmp = context.drawTextWithShadow(client.textRenderer, text, tailX, y,
                        ColorHelper.Argb.withAlpha(u, -1));
                    if (!isAfterEmote) {
                      tailX = tmp;
                    }
                    isAfterEmote = false;
                  }
                }
                context.getMatrices().pop();
              }
            }
          }
        }

        long ac = this.client.getMessageHandler().getUnprocessedMessageCount();
        int ad;
        if (ac > 0L) {
          ad = (int) (128.0 * d);
          t = (int) (255.0 * e);
          context.getMatrices().push();
          context.getMatrices().translate(0.0F, (float) m, 0.0F);
          context.fill(-2, 0, k + 4, 9, t << 24);
          context.getMatrices().translate(0.0F, 0.0F, 50.0F);
          context.drawTextWithShadow(this.client.textRenderer,
              Text.translatable("chat.queue", new Object[] { ac }), 0, 1, 16777215 + (ad << 24));
          context.getMatrices().pop();
        }

        if (focused) {
          ad = this.getLineHeight();
          t = j * ad;
          int ae = q * ad;
          int af = this.scrolledLines * ae / j - m;
          u = ae * ae / t;
          if (t != ae) {
            v = af > 0 ? 170 : 96;
            int w = this.hasUnreadNewMessages ? 13382451 : 3355562;
            x = k + 4;
            context.fill(x, -af, x + 2, -af - u, 100, w + (v << 24));
            context.fill(x + 2, -af, x + 1, -af - u, 100, 13421772 + (v << 24));
          }
        }

        context.getMatrices().pop();
        this.client.getProfiler().pop();
      }
    }
    ci.cancel();
  }

  @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("TAIL"))
  public void onAddMessage(Text message, MessageSignatureData signatureData,
      MessageIndicator indicator, CallbackInfo ci) {
    Set<String> usingUnicode = new HashSet<>();

    for (var visible : visibleMessages) {
      AtomicReference<Style> previousStyle = new AtomicReference<>(Style.EMPTY);
      AtomicReference<StringBuilder> builder = new AtomicReference<>(new StringBuilder());
      List<Text> texts = new ArrayList<>();
      visible.content().accept((index, style, code) -> {
        if (!style.equals(previousStyle.get())) {
          texts.add(Text.literal(builder.toString()).setStyle(previousStyle.get()));
          builder.set(new StringBuilder());
          previousStyle.set(style);
        }
        builder.get().append(Character.toString(code));
        return true;
      });
      texts.add(Text.literal(builder.get().toString()).setStyle(previousStyle.get()));

      for (var text : texts) {
        String str = text.getString();
        if (unicodeMapper.IsUsedUnicode(str)) {
          usingUnicode.add(str);
        }
      }
    }

    EmoteManager.getInstance().applyUsingEmotes(usingUnicode);
  }
}
