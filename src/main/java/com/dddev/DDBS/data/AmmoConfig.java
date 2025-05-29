package com.dddev.DDBS.data;

import java.util.Map;

import static java.util.Map.entry;

public class AmmoConfig {
    public enum Caliber {
        ammo12gauge,
        ammo338lapua,
        ammo357,
        ammo45acp,
        ammo46,
        ammo50ae,
        ammo50bmg,
        ammo545,
        ammo556,
        ammo57,
        ammo68,
        ammo762,
        ammo762x51,
        ammo9mm,
    }

    public enum AmmoType {
        AP, //ArmorPiercing
        HP, //HollowPoint
        ST, //stealth
        BU, //buckshoot
        BI, //birdshoot

    }

    public static final Map<Caliber, Map<AmmoType, AmmoData>> AMMUNITION = Map.ofEntries(
            entry(Caliber.ammo12gauge, Map.of(
                    AmmoType.BU, new AmmoData(4.5f, 3.0f, 5.0f, 100, 0.02f),  // 00 Buckshot
                    AmmoType.BI, new AmmoData(1.2f, 0.8f, 5.0f,  1000, 0.04f),  // Birdshot
                    AmmoType.ST, new AmmoData(18.0f, 18.0f, 4.5f,  10000, 0.001f)  // Slug
            )),
            entry(Caliber.ammo338lapua, Map.of(
                    AmmoType.AP, new AmmoData(30.0f, 32.0f, 7.0f,  1, 0.0f),  // Armor Piercing
                    AmmoType.HP, new AmmoData(45.0f, 12.0f, 7.0f,  1, 0.0f),  // Hollow Point
                    AmmoType.ST, new AmmoData(30.0f, 25.0f, 7.0f,  1, 0.0f)  // Subsonic
            )),
            entry(Caliber.ammo357, Map.of(
                    AmmoType.AP, new AmmoData(9.0f, 18.0f, 5.5f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(12.0f, 6.0f, 5.5f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(9.0f, 12.0f, 5.0f,  1, 0.0f)
            )),
            entry(Caliber.ammo45acp, Map.of(
                    AmmoType.AP, new AmmoData(6.0f, 15.0f, 5.0f, 1, 0.0f),
                    AmmoType.HP, new AmmoData(9.0f, 5.0f, 5.0f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(6.0f, 10.0f, 4.5f,  1, 0.0f)
            )),
            entry(Caliber.ammo46, Map.of(
                    AmmoType.AP, new AmmoData(7.0f, 20.0f, 5.5f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(11.0f, 8.0f, 5.5f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(7.0f, 15.0f, 5.0f,  1, 0.0f)
            )),
            entry(Caliber.ammo50ae, Map.of(
                    AmmoType.AP, new AmmoData(11.0f, 22.0f, 5.5f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(15.0f, 10.0f, 5.5f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(11.0f, 18.0f, 5.0f,  1, 0.0f)
            )),
            entry(Caliber.ammo50bmg, Map.of(
                    AmmoType.AP, new AmmoData(55.0f, 45.0f, 7.0f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(55.0f, 35.0f, 7.0f,  1, 0.0f)
            )),
            entry(Caliber.ammo545, Map.of(
                    AmmoType.AP, new AmmoData(14.0f, 25.0f, 6.0f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(18.0f, 10.0f, 6.0f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(14.0f, 18.0f, 5.5f,  1, 0.0f)
            )),
            entry(Caliber.ammo556, Map.of(
                    AmmoType.AP, new AmmoData(14.0f, 28.0f, 6.0f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(18.0f, 12.0f, 6.0f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(14.0f, 20.0f, 5.5f,  1, 0.0f)
            )),
            entry(Caliber.ammo57, Map.of(
                    AmmoType.AP, new AmmoData(14.0f, 18.0f, 5.5f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(18.0f, 5.0f, 5.5f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(12.0f, 14.0f, 5.0f,  1, 0.0f)
            )),
            entry(Caliber.ammo68, Map.of(
                    AmmoType.AP, new AmmoData(15.0f, 37.0f, 6.0f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(20.0f, 5.0f, 6.0f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(15.0f, 30.0f, 5.5f,  1, 0.0f)
            )),
            entry(Caliber.ammo762, Map.of(
                    AmmoType.AP, new AmmoData(12.0f, 32.0f, 6.0f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(18.0f, 5.0f, 6.0f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(12.0f, 28.0f, 5.5f,  1, 0.0f)
            )),
            entry(Caliber.ammo762x51, Map.of(
                    AmmoType.AP, new AmmoData(20.0f, 40.0f, 7.0f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(35.0f, 10.0f, 7.0f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(20.0f, 32.0f, 6.0f,  1, 0.0f)
            )),
            entry(Caliber.ammo9mm, Map.of(
                    AmmoType.AP, new AmmoData(100.0f, 15.0f, 100.0f,  1, 0.0f),
                    AmmoType.HP, new AmmoData(50.0f, 2.0f, 50.0f,  1, 0.0f),
                    AmmoType.ST, new AmmoData(1.0f, 12.0f, 10.0f,  1, 0.0f)
            ))
    );

    public record AmmoData(float dmg, float ap, float v, int qty, float dis) {
    }
}
