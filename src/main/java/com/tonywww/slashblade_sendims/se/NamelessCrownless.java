package com.tonywww.slashblade_sendims.se;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;

/**
 * 无冠 (Crownless)
 * <p>
 * 无名系列的终点。王者以名字为祭完成加冕，因此：
 * <ul>
 *     <li>解除「褪名」的耐久钳制——该 SE 会被本效果替换，不再生效</li>
 *     <li>「王威」进化为「王威·极」——由 {@code nameless_se_sovereignty_ex} 承载</li>
 * </ul>
 * 本类仅作为「加冕」的标识与说明载体，具体数值变动由同刀的其余 SE 承担。
 */
public class NamelessCrownless extends SpecialEffect {

    public NamelessCrownless() {
        super(80);
    }
}
