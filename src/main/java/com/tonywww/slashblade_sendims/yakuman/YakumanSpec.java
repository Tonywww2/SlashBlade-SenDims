package com.tonywww.slashblade_sendims.yakuman;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/** One immutable table for registrations, ammunition and AP. No string dispatch per tick. */
public enum YakumanSpec {
    KOKUSHI("kokushi",13,3,300,.15),
    SUUANKOU("suuankou",0,0,250,.10),
    DAISANGEN("daisangen",3,17,350,.15),
    SHOUSUUSHII("shousuushii",0,0,250,.10),
    TSUUIISOU("tsuuiisou",0,0,450,.20),
    RYUUIISOU("ryuuiisou",0,0,300,.15),
    CHINROUTOU("chinroutou",0,0,250,.10),
    CHUUREN("chuuren",9,0,300,.15),
    SUUKANTSU("suukantsu",4,16,300,.15),
    TENHOU("tenhou",0,0,450,.20),
    CHIIHOU("chiihou",0,0,350,.15),
    DAISUUSHII("daisuushii",0,0,350,.15),
    SANGEN_JIREI("sangen_jirei",3,17,400,.20),
    CHINROU_SUUKOU("chinrou_suukou",0,0,400,.20),
    HEKIGYOKU_SUUKANTSU("hekigyoku_suukantsu",4,12,400,.20),
    TENJU_KOKUSHI("tenju_kokushi",13,3,600,.25),
    CHISEI_CHUUREN("chisei_chuuren",14,0,550,.25),
    SUUSHI_JIKOKU("suushi_jikoku",0,0,750,.30),
    SHISHOU_KIITSU("shishou_kiitsu",4,15,2000,.64);
    public final String path;
    public final ResourceLocation id;
    public final int ammo, interval, ap;
    public final double apRatio;
    YakumanSpec(String path,int ammo,int interval,int ap,double ratio) {
        this.path=path;this.id=SenDims.prefix("yakuman_"+path);
        this.ammo=ammo;this.interval=interval;this.ap=ap;this.apRatio=ratio;
    }
    private static final Map<ResourceLocation,YakumanSpec> LOOKUP=new HashMap<>();
    static { for(var v:values()) LOOKUP.put(v.id,v); }
    public static YakumanSpec find(ResourceLocation id) { return LOOKUP.get(id); }
}
