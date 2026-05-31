package com.tonywww.slashblade_sendims.mixin.ftbquests;

import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.TeamData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chapter.class)
public class ChapterMixin {
    @Inject(
            method = "isVisible(Ldev/ftb/mods/ftbquests/quest/TeamData;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void fixQuestLinkVisibility(TeamData data, CallbackInfoReturnable<Boolean> cir) {
        Chapter chapter = (Chapter) (Object) this;

        if (chapter.isAlwaysInvisible()) {
            cir.setReturnValue(false);
            return;
        }

        boolean isVisible = chapter.getQuests().isEmpty() ||
                chapter.getQuests().stream()
                        .anyMatch(quest -> quest.isVisible(data));

        cir.setReturnValue(isVisible);
    }

}
