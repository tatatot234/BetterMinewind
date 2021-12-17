package com.tatatot234.betterMinewind;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;

public class Soul {
    private int soulCount;
    private int soulMaxCount;
    private int soulTicks;
    private int soulChargeTime;
    public String soulId;
    private int[] soulsInSlot;
    private int spritePosX;
    private int spritePosY;

    Soul(int chargeTime, String id, int posX, int posY){
        soulCount       = 0;
        soulMaxCount    = 0;
        soulTicks       = 0;
        soulChargeTime  = chargeTime;
        soulId          = id;
        soulsInSlot     = new int[6];
        //Zord has always 3 possible souls.
        if(id == "blinking!"){
            soulsInSlot[0] = 3;
        }

        spritePosX = posX;
        spritePosY = posY;
    }

    public void setSoulSlot(int slot, int soulAmount){
        soulsInSlot[slot] = soulAmount;
    }

    public void SoulTick(){

        soulMaxCount = this.getMaxSouls();                                  //update the number of souls we can max have

        if(soulCount>soulMaxCount) {
            soulCount = soulMaxCount;
        }

        if(soulCount>soulMaxCount) {
            soulCount = soulMaxCount;
        }

        if(soulCount < soulMaxCount) {
            soulTicks++;
            if(soulTicks==soulChargeTime){
                AccumulateSoul();
            }
        }
        if(this.soulId != "blinking!"){                                     //Zord doesnt get reset
            this.resetSoulSlots();
        }
    }

    private int getMaxSouls(){
        int sum = 0;
        for(int value : soulsInSlot){
            sum += value;
        }
        return sum;
    }

    public void resetSoulSlots(){
        soulsInSlot = new int[] {0,0,0,0,0,0};
    }

    private void AccumulateSoul(){
        soulCount++;
        soulTicks = 0;
    }

    public void ConsumeSoul(int amount){
        if(soulCount-amount>=0){
            soulCount -= amount;
        }
    }

    public void drawSoul(InGameHud hud, MatrixStack matrixStack, int soulNumber){
        if(this.soulCount>0){
            DrawableHelper.drawTexture(matrixStack, 5, 80+(soulNumber*18), 16, 16,
                    spritePosX, spritePosY, 16, 16, 64, 64);
        } else{
            DrawableHelper.drawTexture(matrixStack, 5, 80+(soulNumber*18), 16, 16,
                    spritePosX+16, spritePosY, 16, 16, 64, 64);
        }
        drawSoulCount(hud, matrixStack, soulNumber);
    }

    public void drawSoulCount(InGameHud hud, MatrixStack matrixStack, int activeSouls){
        TextRenderer textRenderer = hud.getTextRenderer();
        textRenderer.drawWithShadow(matrixStack,String.valueOf(soulCount) ,5+16-7, 80+(activeSouls*18)+16-5,16777215);

    }

    public boolean isActive(){
        return this.soulMaxCount != 0;
    }
}
