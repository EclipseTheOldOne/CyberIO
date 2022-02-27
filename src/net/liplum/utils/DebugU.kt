package net.liplum.utils

import mindustry.Vars
import mindustry.gen.Building
import mindustry.graphics.Pal
import mindustry.logic.Ranged
import mindustry.ui.Bar
import mindustry.world.meta.BlockBars
import net.liplum.R
import net.liplum.blocks.AniedBlock

annotation class CioDebugOnly

fun BlockBars.addTeamInfo() {
    this.add<Building>(
        R.Bar.TeamN
    ) {
        Bar(
            { R.Bar.Team.bundle(it.team) },
            { Pal.powerBar },
            { 1f }
        )
    }
}

fun <T> BlockBars.addRangeInfo(maxRange: Float) where T : Building, T : Ranged {
    this.add<T>(
        R.Bar.RangeN
    ) {
        Bar(
            { R.Bar.Range.bundle((it.range() / Vars.tilesize).format(1)) },
            { Pal.range },
            { it.range() / maxRange }
        )
    }
}

fun <T> BlockBars.addAniStateInfo() where T : AniedBlock<*, *>.AniedBuild {
    this.add<T>(
        R.Bar.AniStateN
    ) {
        Bar(
            { it.aniStateM.curState.stateName },
            { Pal.bar },
            { 1f }
        )
    }
}


fun BlockBars.addSleepInfo() {
    this.add<Building>(
        R.Bar.IsAsleep
    ) {
        Bar(
            { R.Bar.IsAsleep.bundle(it.sleeping.yesNo()) },
            { Pal.power },
            { if (it.sleeping) 1f else 0f }
        )
    }
}