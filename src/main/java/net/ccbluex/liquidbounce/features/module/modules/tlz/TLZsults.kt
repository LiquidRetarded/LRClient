/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.tlz

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.init.Blocks.air
import net.minecraft.entity.player.EntityPlayer
import kotlin.random.Random

object TLZsults : Module("TLZsults", ModuleCategory.TLZ, subjective = true, gameDetecting = false) {

   val tlzwords = arrayOf(
     "You're so bad that if I played with you, I'd be losing every single game",
     "There's not enough adjectives to describe how bad you are",
     "Here's your ticket to spectator mode",
     "You're that kind of non-recycable trash that no one knows what to do with",
     "Are your hands freezing? Because you missed every single hit",
     "I must be in a deranked game if I'm in the lobby with you, %name%",
     "I'm not hacking it's just my 871619-B21 HP Intel Xeon 8180 2.5GHz DL380 G10 processor",
     "When people play with you, it's considered charity work",
     "I know you're rage quitting but with that aim, you'd be having trouble clicking the disconnect button",
     "I don't care about the fact that I'm hacking I just care how you died in a block game",
     "Your gaming chair expired mid-fight so that's how you lost %name%",
     "You're so special that you can be the password requirement",
     "%name% is the type of person who climbs over a glass wall to see what's on the other side",
     "It's a bird! It's a plane! No it's your rank falling!",
     "*yawn* I get so bored playing against you. That's okay though",
     "Damn you have the awareness of a sloth",
     "That aim is so trash I think the safest place to stand is in front of you"
   )

   var target: EntityPlayer? = null

   @EventTarget
   fun onUpdate(event: UpdateEvent) {
       if (mc.objectMouseOver == null || mc.objectMouseOver.blockPos == null || mc.theWorld == null)
           return

       if (mc.objectMouseOver.blockPos != null && mc.theWorld != null && getBlock(mc.objectMouseOver.blockPos) != air) {
           target = mc.objectMouseOver.entityHit as EntityPlayer?
       }
   }

   override fun onDisable() {
       target = null
   }

   fun sendInsult() {
       if (target != null && target?.isDead == true) {
           val randomWord = tlzwords[Random.nextInt(tlzwords.size)]
           val message = randomWord.replace("%name%", target?.getName().orEmpty())
           mc.thePlayer.sendChatMessage(message)
           target = null
       }
   }
}