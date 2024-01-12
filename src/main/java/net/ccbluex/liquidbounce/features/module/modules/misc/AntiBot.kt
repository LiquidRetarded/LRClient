/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.*
import net.ccbluex.liquidbounce.utils.PacketUtils


object AntiBot : Module("AntiBot", ModuleCategory.MISC) {

    private val tab by BoolValue("Tab", true)
        private val tabMode by ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains") { tab }

    private val entityID by BoolValue("EntityID", true)
    private val color by BoolValue("Color", false)

    private val livingTime by BoolValue("LivingTime", false)
        private val livingTimeTicks by IntegerValue("LivingTimeTicks", 40, 1..200) { livingTime }

    private val ground by BoolValue("Ground", true)
    private val air by BoolValue("Air", false)
    private val invalidGround by BoolValue("InvalidGround", true)
    private val swing by BoolValue("Swing", false)
    private val health by BoolValue("Health", false)
    private val derp by BoolValue("Derp", true)
    private val wasInvisible by BoolValue("WasInvisible", false)
    private val armor by BoolValue("Armor", false)
    private val ping by BoolValue("Ping", false)
    private val needHit by BoolValue("NeedHit", false)
    private val spawnInCombatValue = BoolValue("SpawnInCombat", false)
    private val duplicateInWorld by BoolValue("DuplicateInWorld", false)
    private val duplicateInTab by BoolValue("DuplicateInTab", false)
    private val duplicateCompareMode = ListValue("DuplicateCompareMode", arrayOf("OnTime", "WhenSpawn"), "OnTime") {
        duplicateInTab.get() || duplicateInWorld.get()
    }
    private val reusedEntityIdValue = BoolValue("ReusedEntityId", false)
    private val properties by BoolValue("Properties", false)

    private val alwaysInRadius by BoolValue("AlwaysInRadius", false)
        private val alwaysRadius by FloatValue("AlwaysInRadiusBlocks", 20f, 5f..30f) { alwaysInRadius }

    private val groundList = mutableListOf<Int>()
    private val airList = mutableListOf<Int>()
    private val invalidGroundList = mutableMapOf<Int, Int>()
    private val swingList = mutableListOf<Int>()
    private val spawnInCombat = mutableListOf<Int>()
    private val invisibleList = mutableListOf<Int>()
    private val duplicate = mutableListOf<UUID>()
    private val propertiesList = mutableListOf<Int>()
    private val hitList = mutableListOf<Int>()
    private val hasRemovedEntities = mutableListOf<Int>()
    private val notAlwaysInRadiusList = mutableListOf<Int>()

    fun isBot(entity: EntityLivingBase): Boolean {
        // Check if entity is a player
        if (entity !is EntityPlayer)
            return false

        // Check if anti bot is enabled
        if (!handleEvents())
            return false

        // Anti Bot checks

        if (color && "§" !in entity.displayName.formattedText.replace("§r", ""))
            return true

        if (livingTime && entity.ticksExisted < livingTimeTicks)
            return true

        if(reusedEntityIdValue.get() && hasRemovedEntities.contains(entity.entityId)) {
            return false
        }    

        if (ground && entity.entityId !in groundList)
            return true

        if (air && entity.entityId !in airList)
            return true

        if (swing && entity.entityId !in swingList)
            return true

        if (health && entity.health > 20F)
            return true

        if (entityID && (entity.entityId >= 1000000000 || entity.entityId <= -1))
            return true

        if (derp && (entity.rotationPitch > 90F || entity.rotationPitch < -90F))
            return true

        if (wasInvisible && entity.entityId in invisibleList)
            return true

        if (spawnInCombatValue.get() && spawnInCombat.contains(entity.entityId)) {
            return true
        }

        if (properties && entity.entityId !in propertiesList)
            return true

        if (armor) {
            if (entity.inventory.armorInventory[0] == null && entity.inventory.armorInventory[1] == null &&
                    entity.inventory.armorInventory[2] == null && entity.inventory.armorInventory[3] == null)
                return true
        }

        if (ping) {
            if (mc.netHandler.getPlayerInfo(entity.uniqueID)?.responseTime == 0)
                return true
        }

        if (needHit && entity.entityId !in hitList)
            return true

        if (invalidGround && invalidGroundList.getOrDefault(entity.entityId, 0) >= 10)
            return true

        if (tab) {
            val equals = tabMode == "Equals"
            val targetName = stripColor(entity.displayName.formattedText)

            for (networkPlayerInfo in mc.netHandler.playerInfoMap) {
                val networkName = stripColor(networkPlayerInfo.getFullName())

                if (if (equals) targetName == networkName else networkName in targetName)
                    return false
            }

            return true
        }

        if (duplicateCompareMode.equals("WhenSpawn") && duplicate.contains(entity.gameProfile.id)) {
            return true
        }

        if (duplicateInWorld.get() && duplicateCompareMode.equals("OnTime") && mc.theWorld.loadedEntityList.count { it is EntityPlayer && it.name == it.name } > 1) {
            return true
        }

        if (duplicateInTab.get() && duplicateCompareMode.equals("OnTime") && mc.netHandler.playerInfoMap.count { entity.name == it.gameProfile.name } > 1) {
            return true
        }
        
        if (alwaysInRadius && entity.entityId !in notAlwaysInRadiusList)
            return true

        return entity.name.isEmpty() || entity.name == mc.thePlayer.name
    }

    override fun onDisable() {
        super.onDisable()
    }

    @EventTarget(ignoreCondition=true)
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return

        val packet = event.packet

        if (packet is S14PacketEntity) {
            val entity = packet.getEntity(mc.theWorld)

            if (entity is EntityPlayer) {
                if (entity.onGround && entity.entityId !in groundList)
                    groundList += entity.entityId

                if (!entity.onGround && entity.entityId !in airList)
                    airList += entity.entityId

                if (entity.onGround) {
                    if (entity.prevPosY != entity.posY)
                        invalidGroundList[entity.entityId] = invalidGroundList.getOrDefault(entity.entityId, 0) + 1
                } else {
                    val currentVL = invalidGroundList.getOrDefault(entity.entityId, 0) / 2
                    if (currentVL <= 0)
                        invalidGroundList -= entity.entityId
                    else
                        invalidGroundList[entity.entityId] = currentVL
                }

                if (entity.isInvisible && entity.entityId !in invisibleList)
                    invisibleList += entity.entityId

                if (entity.entityId !in notAlwaysInRadiusList && mc.thePlayer.getDistanceToEntity(entity) > alwaysRadius)
                    notAlwaysInRadiusList += entity.entityId
            }
        }

        if (packet is S0BPacketAnimation) {
            val entity = mc.theWorld.getEntityByID(packet.entityID)

            if (entity != null && entity is EntityLivingBase && packet.animationType == 0
                    && entity.entityId !in swingList)
                swingList += entity.entityId
        } else if (packet is S38PacketPlayerListItem) {
            if (duplicateCompareMode.equals("WhenSpawn") && packet.action == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                packet.entries.forEach { entry ->
                    val name = entry.profile.name
                    if (duplicateInWorld.get() && mc.theWorld.playerEntities.any { it.name == name } ||
                        duplicateInTab.get() && mc.netHandler.playerInfoMap.any { it.gameProfile.name == name }) {
                        duplicate.add(entry.profile.id)
                    }
                }
            }
        } else if (packet is S0CPacketSpawnPlayer) {
            if(LiquidBounce.combatManager.inCombat && !hasRemovedEntities.contains(packet.entityID)) {
                spawnInCombat.add(packet.entityID)
            }
        } 
        if (packet is S20PacketEntityProperties) {
            propertiesList += packet.entityId
        }
        

        if (packet is S13PacketDestroyEntities) {
            for (entityID in packet.entityIDs) {
                // Check if entityID exists in groundList and remove if found
                if (entityID in groundList) groundList -= entityID

                // Check if entityID exists in airList and remove if found
                if (entityID in airList) airList -= entityID

                // Check if entityID exists in invalidGroundList and remove if found
                if (entityID in invalidGroundList) invalidGroundList -= entityID

                // Check if entityID exists in swingList and remove if found
                if (entityID in swingList) swingList -= entityID

                // Check if entityID exists in invisibleList and remove if found
                if (entityID in invisibleList) invisibleList -= entityID

                // Check if entityID exists in notAlwaysInRadiusList and remove if found
                if (entityID in notAlwaysInRadiusList) notAlwaysInRadiusList -= entityID
            }
        }
    }

    @EventTarget(ignoreCondition=true)
    fun onAttack(e: AttackEvent) {
        val entity = e.targetEntity

        if (entity != null && entity is EntityLivingBase && entity.entityId !in hitList)
            hitList += entity.entityId
    }

    @EventTarget(ignoreCondition=true)
    fun onWorld(event: WorldEvent) {
        clearAll()
    }

    private fun clearAll() {
        hitList.clear()
        swingList.clear()
        groundList.clear()
        invalidGroundList.clear()
        invisibleList.clear()
        notAlwaysInRadiusList.clear()
        spawnInCombat.clear()
        hasRemovedEntities.clear()
        duplicate.clear()
    }

}