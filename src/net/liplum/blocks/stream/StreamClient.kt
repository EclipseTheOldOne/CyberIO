package net.liplum.blocks.stream

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.scene.ui.layout.Table
import arc.struct.ObjectSet
import arc.struct.OrderedSet
import arc.util.Eachable
import arc.util.Time
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.Vars
import mindustry.entities.units.BuildPlan
import mindustry.gen.Building
import mindustry.graphics.Drawf
import mindustry.type.Liquid
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.ItemSelection
import mindustry.world.meta.BlockGroup
import net.liplum.CalledBySync
import net.liplum.CanRefresh
import net.liplum.ClientOnly
import net.liplum.DebugOnly
import net.liplum.animations.anis.AniState
import net.liplum.animations.anis.DrawTR
import net.liplum.animations.anis.config
import net.liplum.api.drawLinkedLineToClientWhenConfiguring
import net.liplum.api.drawStreamGraphic
import net.liplum.api.stream.*
import net.liplum.api.whenNotConfiguringHost
import net.liplum.blocks.AniedBlock
import net.liplum.delegates.Delegate1
import net.liplum.persistance.intSet
import net.liplum.utils.*

private typealias AniStateC = AniState<StreamClient, StreamClient.ClientBuild>

open class StreamClient(name: String) : AniedBlock<StreamClient, StreamClient.ClientBuild>(name) {
    @JvmField var maxConnection = -1
    @ClientOnly lateinit var NoPowerTR: TR
    @ClientOnly lateinit var LiquidTR: TR

    init {
        hasLiquids = true
        update = true
        solid = true
        group = BlockGroup.liquids
        outputsLiquid = true
        configurable = true
        saveConfig = true
        noUpdateDisabled = true
        canOverdrive = false
        sync = true
        config(
            Liquid::class.java
        ) { obj: ClientBuild, liquid ->
            obj.outputLiquid = liquid
        }
        configClear { tile: ClientBuild -> tile.outputLiquid = null }
    }

    override fun setBars() {
        super.setBars()
        DebugOnly {
            bars.addHostInfo<ClientBuild>()
        }
    }

    override fun load() {
        super.load()
        NoPowerTR = this.inMod("rs-no-power")
        LiquidTR = this.sub("liquid")
    }

    override fun drawPlace(x: Int, y: Int, rotation: Int, valid: Boolean) {
        super.drawPlace(x, y, rotation, valid)
        this.drawLinkedLineToClientWhenConfiguring(x, y)
    }

    override fun drawRequestConfig(req: BuildPlan, list: Eachable<BuildPlan>) {
        drawRequestConfigCenter(req, req.config, "center", true)
    }

    open inner class ClientBuild : AniedBuild(), IStreamClient {
        var hosts = OrderedSet<Int>()
        var outputLiquid: Liquid? = null
            set(value) {
                if (field != value) {
                    field = value
                    onRequirementUpdated(this)
                }
            }

        open fun checkHostPos() {
            hosts.removeAll { !it.sh().exists }
        }

        @JvmField var onRequirementUpdated: Delegate1<IStreamClient> = Delegate1()
        override fun getOnRequirementUpdated() = onRequirementUpdated
        override fun getRequirements(): Array<Liquid>? = outputLiquid.req
        @CalledBySync
        override fun connect(host: IStreamHost) {
            hosts.add(host.building.pos())
        }
        @CalledBySync
        override fun disconnect(host: IStreamHost) {
            hosts.remove(host.building.pos())
        }

        override fun connectedHosts(): ObjectSet<Int> = hosts
        override fun maxHostConnection() = maxConnection
        override fun getClientColor(): Color = outputLiquid.clientColor
        override fun updateTile() {
            // Check connection every second
            if (Time.time % 60f < 1) {
                checkHostPos()
            }
            val outputLiquid = outputLiquid
            if (outputLiquid != null) {
                if (consValid()) {
                    if (liquids.total() > 0.1f) {
                        dumpLiquid(outputLiquid)
                    }
                }
            }
        }

        override fun readStream(host: IStreamHost, liquid: Liquid, amount: Float) {
            if (this.isConnectedWith(host)) {
                liquids.add(liquid, amount)
            }
        }

        override fun acceptedAmount(host: IStreamHost, liquid: Liquid): Float {
            if (!consValid()) return 0f
            return if (liquid == outputLiquid)
                liquidCapacity - liquids[outputLiquid]
            else
                0f
        }

        override fun drawSelect() {
            whenNotConfiguringHost {
                this.drawStreamGraphic()
                val outputLiquid = outputLiquid
                if (outputLiquid != null) {
                    G.drawMaterialIcon(this, outputLiquid)
                }
            }
        }

        override fun acceptLiquid(source: Building, liquid: Liquid) = false
        override fun buildConfiguration(table: Table?) {
            ItemSelection.buildTable(this@StreamClient, table, Vars.content.liquids(),
                { outputLiquid },
                { value: Liquid? -> tryConfigOutputLiquid(value) })
        }

        open fun tryConfigOutputLiquid(liquid: Liquid?): Boolean {
            if (liquids.total() > 0.1f) {
                return false
            }
            configure(liquid)
            return true
        }

        override fun onConfigureTileTapped(other: Building): Boolean {
            if (this === other) {
                deselect()
                configure(null)
                return false
            }
            return true
        }

        override fun config(): Liquid? = outputLiquid
        override fun write(write: Writes) {
            super.write(write)
            val outputLiquid = outputLiquid
            write.s(outputLiquid?.id?.toInt() ?: -1)
            write.intSet(hosts)
        }

        override fun read(read: Reads, revision: Byte) {
            super.read(read, revision)
            outputLiquid = Vars.content.liquid(read.s().toInt())
            hosts = read.intSet()
        }

        override fun draw() {
            aniStateM.spend(delta())
            beforeDraw()
            if (CanRefresh()) {
                aniStateM.update()
            }
            Draw.rect(region, x, y)
            Drawf.liquid(
                LiquidTR, x, y,
                liquids.total() / liquidCapacity,
                liquids.current().color,
                (rotation - 90).toFloat()
            )
            fixedDraw()
            aniStateM.drawBuilding()
        }

        override fun getBuilding(): Building = this
        override fun getTile(): Tile = tile
        override fun getBlock(): Block = this@StreamClient
    }

    @ClientOnly lateinit var NormalAni: AniStateC
    @ClientOnly lateinit var NoPowerAni: AniStateC
    override fun genAniState() {
        NormalAni = addAniState("Normal") {
        }
        NoPowerAni = addAniState("NoPower") {
            DrawTR(NoPowerTR, it.x, it.y)
        }
    }

    override fun genAniConfig() {
        config {
            From(NormalAni) To NoPowerAni When {
                !it.consValid()
            }
            From(NoPowerAni) To NormalAni When {
                it.consValid()
            }
        }
    }
}