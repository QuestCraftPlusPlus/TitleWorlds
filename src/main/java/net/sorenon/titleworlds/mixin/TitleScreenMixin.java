package net.sorenon.titleworlds.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageException;
import net.sorenon.titleworlds.Screenshot3D;
import net.sorenon.titleworlds.TitleWorldsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Unique
    private boolean noLevels;

    @Inject(method = "init", at = @At("HEAD"))
    void onInit(CallbackInfo ci) {
        boolean modmenu = FabricLoader.getInstance().isModLoaded("modmenu");

        var level = Minecraft.getInstance().level;
        if (!TitleWorldsMod.state.isTitleWorld && level != null) {
            this.addRenderableWidget(new ImageButton(
                    this.width / 2 + 104,
                    (this.height / 4 + 48) + 60 + (modmenu ? 24 : 0), 20,
                    20, 0, 0, 20,
                    new ResourceLocation("titleworlds", "/textures/gui/3dscreenshot.png"),
                    32, 64,
                    (button -> {
                        String name = Screenshot3D.take3DScreenshot(level, null);
                        Minecraft.getInstance().player.sendSystemMessage(Component.translatable("titleworlds.message.saved_3d_screenshot", name));
                    })));
        } else if (TitleWorldsMod.CONFIG.enabled) {
            if (TitleWorldsMod.CONFIG.reloadButton) {
                this.addRenderableWidget(new ImageButton(
                        this.width / 2 + 104,
                        (this.height / 4 + 48) + 60 + (modmenu ? 24 : 0), 20,
                        20, 0, 0, 20,
                        new ResourceLocation("titleworlds", "/textures/gui/reload.png"),
                        32, 64,
                        (button -> {
                            if (!TitleWorldsMod.state.reloading) {
                                TitleWorldsMod.state.reloading = true;
                                Minecraft.getInstance().clearLevel();
                            }
                        })));
            }
        }

        if (TitleWorldsMod.state.isTitleWorld) {
            this.noLevels = false;
        } else {
            try {
                this.noLevels = TitleWorldsMod.LEVEL_SOURCE.findLevelCandidates().isEmpty();
            } catch (LevelStorageException e) {
                TitleWorldsMod.LOGGER.error(e);
            }
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"), method = "render")
    void cancelCubemapRender(PanoramaRenderer instance, float f, float g) {
        if (Minecraft.getInstance().level == null || !Minecraft.getInstance().isRunning()) {
            if (TitleWorldsMod.state.isTitleWorld) {
                this.renderDirtBackground(0);
            } else {
                instance.render(f, g);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    void render(PoseStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.noLevels) {
            GuiComponent.drawCenteredString(matrices, font, "Put one or more worlds in the titleworlds folder and restart the game", this.width / 2, 2, 16777215);
        }
    }

    @Inject(method = "isPauseScreen", cancellable = true, at = @At("HEAD"))
    void isPauseScreen(CallbackInfoReturnable<Boolean> cir) {
        if (TitleWorldsMod.state.isTitleWorld) {
            cir.setReturnValue(TitleWorldsMod.state.pause);
        }
    }

    @Inject(method = "shouldCloseOnEsc", cancellable = true, at = @At("HEAD"))
    void shouldCloseOnEsc(CallbackInfoReturnable<Boolean> cir) {
        if (!TitleWorldsMod.state.isTitleWorld) {
            cir.setReturnValue(true);
        }
    }
}
