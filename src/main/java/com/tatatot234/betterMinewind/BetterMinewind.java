package com.tatatot234.betterMinewind;


import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

public class BetterMinewind implements ModInitializer {

    private static MinecraftClient mc = MinecraftClient.getInstance();

    private static KeyBinding offhandHotswapBinding1;
    private static KeyBinding offhandHotswapBindingZord;
    private static KeyBinding offhandHotswapBindingFmb;
    private static KeyBinding offhandHotswapBindingBs;
    private static KeyBinding offhandHotswapBindingSrage;
    private static KeyBinding offhandHotswapBindingGc;
    private static KeyBinding elytraHotswapBinding;
    private static KeyBinding aegisHotswapBinding;

    private static Soul[] souls;
    private static Soul iceSoul;
    private static Soul fireSoul;
    private static Soul shadowSoul;
    private static Soul dragonSoul;
    private static Soul zordSoul;

    private static ItemStack[] slotsToCheck;
    private static final String DISPLAY_KEY = "display";
    private static final String LORE_KEY    = "Lore";
    private int ticksSinceLastJump;

    public final Identifier soulSprites = new Identifier("bettermw","textures/souls.png");

    /*
    Todo:   Zord doesnt get displayed on the server *sometimes*
            Aegis & Book hotswap (add lore)
            Change placement of number of souls
            Change Shadow soul charged skull to have red eyes to be more easily readable
            Get a Config ready
            Config posX and posY of the SpellHUD
            Config to which place in the hotbar the items get sent (current default = 1)
            Config to switch on and off the spellhud entirely
            Event Alerts
            Better Chat
            Bound timer (Mixin for EntityDamageSource / ProjectileDamageSource + add mixin for firing a bow to add the ItemStack data to the arrow entity)
            Succ Timer (See above)
    */

    @Override
    public void onInitialize() {
        MwHotswapInit();
        EventAlert();
        BetterInventory();
        SpellHudInit();

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if(client.player != null){
                MwHotswapOnTick(client);
                SpellHudOnTick(client);
            }
        });

        //Gets called once cliendside and then the client gets sent the server packet once aswell ( Only if you are running the mod on a singleplayer world )
        UseItemCallback.EVENT.register((player, world, hand) ->{
            if(world.isClient()){
                if(hand.equals(Hand.MAIN_HAND)){
                    ItemStack itemStack = player.inventory.getMainHandStack();
                    if(hasLore(itemStack)) {
                        String lore = itemStack.getTag().getCompound(DISPLAY_KEY).get(LORE_KEY).asString();
                        CheckForConsume(lore);
                    }
                    return new TypedActionResult<ItemStack>(ActionResult.PASS, player.inventory.getMainHandStack());
                }
                else if(hand.equals(Hand.OFF_HAND)){
                    ItemStack itemStack = player.inventory.offHand.get(0);
                    if(hasLore(itemStack)) {
                        String lore = itemStack.getTag().getCompound(DISPLAY_KEY).get(LORE_KEY).asString();
                        CheckForConsume(lore);
                    }
                    return new TypedActionResult<ItemStack>(ActionResult.PASS, player.inventory.offHand.get(0));
                }
            }
            return new TypedActionResult<ItemStack>(ActionResult.PASS, player.inventory.getMainHandStack());
        });

        //render event
        HudRenderCallback.EVENT.register((matrixStack, val)-> {
            InGameHud hud = mc.inGameHud;

            for(int i= 0,activeSouls = 0; i<souls.length;i++){
                if(souls[i].isActive()){
                    mc.getTextureManager().bindTexture(soulSprites);            //this works, but look at betterpvp at how they register their renders.
                    if(souls[i].soulId == zordSoul.soulId){
                        if(findSlotWithLore(0,1,souls[i].soulId,mc.player.inventory.offHand) != -1){
                            souls[i].drawSoul(hud, matrixStack, activeSouls);
                        }
                    } else{
                        souls[i].drawSoul(hud, matrixStack, activeSouls);
                    }
                    activeSouls++;
                }
            }

        });

    }

    private void SpellHudInit(){

        souls = new Soul[] {
                iceSoul = new Soul(160,"ice",0,0),
                fireSoul = new Soul(160,"fire",0,16),
                shadowSoul = new Soul(140,"shadow",0,32),
                dragonSoul = new Soul(160,"dragon",0,48),
                //Always have zord be the last soul !!
                zordSoul = new Soul(140,"blinking!",32,0),
        };

        slotsToCheck = new ItemStack[6];
        ticksSinceLastJump = 20;
    }

    private void SpellHudOnTick(MinecraftClient client) {
        //Fill slotsToCheck
        for (int i = 0; i < 4; i++) { slotsToCheck[i] = client.player.inventory.armor.get(i); }
        slotsToCheck[4] = client.player.inventory.offHand.get(0);
        slotsToCheck[5] = client.player.inventory.getMainHandStack();
        /*slotsToCheck  0:Boots
         *               1:Legs
         *               2:Chest
         *               3:Helmet
         *               4:offHand
         *               5:MainHand*/

        //count the amount of souls in player's current setup (Zord always has 3)
        for (int i = 0; i < 6; i++) {
            if(hasLore(slotsToCheck[i])){
                String lore = slotsToCheck[i].getTag().getCompound(DISPLAY_KEY).get(LORE_KEY).asString();
                if (lore.contains("- Accumulates")) {
                    int soulAmount = Integer.parseInt(lore.replaceAll("(?<!- Accumulates )[0-9]", "").replaceAll("\\D", ""));
                    if (lore.contains(iceSoul.soulId)) {
                        iceSoul.setSoulSlot(i, soulAmount);
                    } else if (lore.contains(shadowSoul.soulId)) {
                        shadowSoul.setSoulSlot(i, soulAmount);
                    } else if (lore.contains(fireSoul.soulId)) {
                        fireSoul.setSoulSlot(i, soulAmount);
                    } else if (lore.contains(dragonSoul.soulId)) {
                        dragonSoul.setSoulSlot(i, soulAmount);

                        //Rift Double jump logic:
                        if (i == 0) {
                            if (ticksSinceLastJump < 20) {
                                ticksSinceLastJump++;
                            }
                            if (client.player.input.jumping) {
                                if (ticksSinceLastJump < 6 && ticksSinceLastJump > 1) {
                                    dragonSoul.ConsumeSoul(1);
                                    ticksSinceLastJump = 20;
                                } else {
                                    ticksSinceLastJump = 0;
                                }
                            }   }   }   }   }   }

        for(int i = 0; i<souls.length;i++){
            souls[i].SoulTick();
        }

    }

    private void MwHotswapInit(){
        offhandHotswapBinding1      = KeyBindingHelper.registerKeyBinding(new KeyBinding("Offhand Hotswap #1"   , InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "BetterMinewind"));
        //Name of KeyBinding in Menu        //Keyboard or Mouse     //Default Key         //Category under which you find keybinds in the menu
        offhandHotswapBindingZord   = KeyBindingHelper.registerKeyBinding(new KeyBinding("Zord Hotswap"         , InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "BetterMinewind"));
        offhandHotswapBindingFmb    = KeyBindingHelper.registerKeyBinding(new KeyBinding("FMB Hotswap"          , InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "BetterMinewind"));
        offhandHotswapBindingBs     = KeyBindingHelper.registerKeyBinding(new KeyBinding("Backstabber Hotswap"  , InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "BetterMinewind"));
        offhandHotswapBindingSrage  = KeyBindingHelper.registerKeyBinding(new KeyBinding("Santas Rage Hotswap"  , InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "BetterMinewind"));
        offhandHotswapBindingGc     = KeyBindingHelper.registerKeyBinding(new KeyBinding("Glass Cannon Hotswap" , InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "BetterMinewind"));

        elytraHotswapBinding        = KeyBindingHelper.registerKeyBinding(new KeyBinding("Elytra Hotswap"       , InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "BetterMinewind"));
        aegisHotswapBinding         = KeyBindingHelper.registerKeyBinding(new KeyBinding("Aegis Hotswap"        , InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "BetterMinewind"));
    }

    private void MwHotswapOnTick(MinecraftClient client){
        while (offhandHotswapBinding1.wasPressed()) {
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, 9, 40,          //40 = offhand
                    SlotActionType.SWAP, client.player);
        }

        while (elytraHotswapBinding.wasPressed()){
            int startSlot = 9;
            ItemStack armor = client.player.inventory.armor.get(2);
            if(!armor.isEmpty()){
                String sArmor = armor.getItem().toString();
                if(sArmor.contains("_chestplate")){
                    int slot = findSlotWithItem(startSlot, 36,"elytra", client.player.inventory);
                    if (slot != -1){
                        client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                                slot,
                                38,
                                SlotActionType.SWAP, client.player);

                        //Put rocket into slot #1 if found
                        int rocket = findSlotWithItem(startSlot,36,"firework_rocket", client.player.inventory);
                        if (rocket != -1){
                            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                                    rocket,
                                    0,
                                    SlotActionType.SWAP, client.player);
                        }
                    }
                }
                else {
                    int slot = findSlotWithItem(startSlot, 36, "_chestplate", client.player.inventory);
                    if (slot != -1){
                        client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                                slot,
                                38,
                                SlotActionType.SWAP, client.player);
                    }
                }

            }
        }

        while (aegisHotswapBinding.wasPressed()){
            int startSlot = 9;
            String aegisLore = "Fabled animal\\'s skin, worn";
            String aegisBookLore = "Right click your dear friend.";
            ItemStack armor = client.player.inventory.armor.get(2);
            if(!armor.isEmpty()){
                if(hasLore(armor)){
                    //  if we find an aegis already in the chestplate slot, look for another chestplate in the main inventory to swap to.
                    if(armor.getTag().getCompound(DISPLAY_KEY).get(LORE_KEY).asString().contains(aegisLore)){
                        int slot = findSlotWithItem(startSlot, 36,"_chestplate", client.player.inventory);
                        if (slot != -1){
                            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                                    slot,
                                    38,
                                    SlotActionType.SWAP, client.player);
                        }
                    } else{
                        int aegisSlot = findSlotWithLore(startSlot, 36,aegisLore, client.player.inventory.main);
                        if(aegisSlot != -1){
                            int aegisBookSlot = findSlotWithLore(startSlot, 36, aegisBookLore, client.player.inventory.main);
                            if(aegisBookSlot != -1){
                                client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                                        aegisBookSlot,
                                        0,
                                        SlotActionType.SWAP, client.player);
                                client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                                        aegisSlot,
                                        38,
                                        SlotActionType.SWAP, client.player);
                            }

                        }
                    }
                }
                else {
                    int slot = findSlotWithItem(startSlot, 36, "_chestplate", client.player.inventory);
                    if (slot != -1){
                        client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                                slot,
                                38,
                                SlotActionType.SWAP, client.player);
                    }
                }

            }
        }

        while(offhandHotswapBindingZord.wasPressed()){
            PlayerInventory inventory = client.player.inventory;
            int zordBookSlot    = findSlotWithLore(0,36,"This book keeps blinking!", inventory.main);
            if(zordBookSlot == -1) {
                zordBookSlot = findSlotWithLore(0, 1, "This book keeps blinking!", inventory.offHand);
            }

            if(zordBookSlot != -1){
                int zordSwordSlot   = findSlotWithLore(0,36,"sword suffocated in a wall.", inventory.main);
                if(zordSwordSlot!= -1){
                    //Zord book to offhand
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, zordBookSlot, 40,
                            SlotActionType.SWAP, client.player);
                    //Zord sword to slot #1
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, zordSwordSlot, 0,
                            SlotActionType.SWAP, client.player);
                }
            }
        }

        while(offhandHotswapBindingGc.wasPressed()){
            PlayerInventory inventory = client.player.inventory;
            int gcBookSlot    = findSlotWithLore(0,36,"Forbidden texts written", inventory.main);
            if(gcBookSlot == -1) {
                gcBookSlot = findSlotWithLore(0, 1, "Forbidden texts written", inventory.offHand);
            }

            if(gcBookSlot != -1){
                int gcSlot   = findSlotWithLore(0,36,"Doctor Hu\\'s first prototype", inventory.main);
                if(gcSlot!= -1){
                    //Zord book to offhand
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, gcBookSlot, 40,
                            SlotActionType.SWAP, client.player);
                    //Zord sword to slot #1
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, gcSlot, 0,
                            SlotActionType.SWAP, client.player);
                }
            }
        }

        while (offhandHotswapBindingBs.wasPressed()) {
            int slot = findSlotWithLore(0,36,"Sweet screams of death",client.player.inventory.main);
            if(slot != -1){
                client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, slot, 40,          //40 = offhand
                        SlotActionType.SWAP, client.player);
            }
        }

        while (offhandHotswapBindingSrage.wasPressed()) {
            int slot = findSlotWithLore(0,36,"Favorite tool of the Jolly Man",client.player.inventory.main);
            if(slot != -1){
                client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, slot, 40,          //40 = offhand
                        SlotActionType.SWAP, client.player);
            }
        }

        while (offhandHotswapBindingFmb.wasPressed()) {
            PlayerInventory inventory = client.player.inventory;
            int iceBookSlot    = findSlotWithLore(0,36,"- Consumes 2 ice souls", inventory.main);
            if(iceBookSlot == -1) {
                iceBookSlot = findSlotWithLore(0, 1, "- Consumes 1 ice soul", inventory.offHand);
            }

            int fmbSlot   = findSlotWithLore(0,36,"Offhand fingers of it\\'s previous", inventory.main);
            if(fmbSlot != -1){
                //FrostMasterBlade to offhand
                client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, fmbSlot, 40,
                        SlotActionType.SWAP, client.player);
                if(iceBookSlot != -1){
                    //any Ice book to slot #1
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId, iceBookSlot, 0,
                            SlotActionType.SWAP, client.player);
                }
            }

        }
    }

    private int findSlotWithItem(int lowerSlotBound, int upperSlotBound, String key, PlayerInventory inventory){
        int i = lowerSlotBound;
        while(i < upperSlotBound){
            if(inventory.main.get(i).getItem().toString().contains(key)){
                return i;
            }
            i++;
        }
        return -1;                                                  //Means item not found
    }

    private int findSlotWithLore(int lowerSlotBound, int upperSlotBound, String lore, DefaultedList<ItemStack> main){
        int i = lowerSlotBound;
        while(i < upperSlotBound){
            ItemStack stack = main.get(i);
            if(hasLore(stack)){
                String s = stack.getTag().getCompound(DISPLAY_KEY).get(LORE_KEY).asString();
                if(stack.getTag().getCompound(DISPLAY_KEY).get(LORE_KEY).asString().contains(lore)){
                    return i;
                }
            }
            i++;
        }
        return -1;                                                  //Means item not found
    }

    private boolean hasLore(ItemStack itemStack){
        if(!itemStack.isEmpty()){
            if(itemStack.hasTag()){
                CompoundTag tag = itemStack.getTag();
                if(tag.contains(DISPLAY_KEY)){
                    if(tag.getCompound(DISPLAY_KEY).contains(LORE_KEY)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void CheckForConsume(String lore){
        if(lore.contains("sword suffocated in a wall.")){
            if(hasLore(slotsToCheck[4])){
                if(slotsToCheck[4].getTag().getCompound(DISPLAY_KEY).get(LORE_KEY).asString().contains("blinking!")){
                    souls[souls.length-1].ConsumeSoul(1);                                                        //souls.length-1 = last slot of the souls array which is always supposed to be zord
                }
            }
        }
        else if (lore.contains("- Consumes")) {
            boolean hasFoundMatchingSoul = false;
            int i = 0;
            while(i < souls.length && !hasFoundMatchingSoul){
                String consumableSouls = lore.replaceAll("[0-9](?! " + souls[i].soulId +")","").replaceAll("\\D","");
                if(!consumableSouls.isEmpty()){
                    souls[i].ConsumeSoul(Integer.parseInt(consumableSouls));
                    hasFoundMatchingSoul = true;
                }
                i++;
            }
        }
    }


    private void BetterInventory() {

    }

    private void EventAlert() {

    }
}

/*      Itemslots:
 *                   0-8 = Hotbar
 *                   9-35 = Main
 *                   36 = boots
 *                   37 = legs
 *                   38 = chest
 *                   39 = helmet
 *                   40 = offhand
 */