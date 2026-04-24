package games.alejandrocoria.mapfrontiers.mixin.server;

import games.alejandrocoria.mapfrontiers.server.event.ServerGlobalEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("TAIL"))
    private void mapfrontiers$onPlayerPermissionLevelUpdated(ServerPlayer player, CallbackInfo ci) {
        PlayerList playerList = (PlayerList) (Object) this;
        ServerGlobalEvents.postPlayerPermissionLevelUpdatedEvent(playerList.getServer(), player);
    }
}
