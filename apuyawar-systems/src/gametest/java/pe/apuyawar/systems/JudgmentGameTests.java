package pe.apuyawar.systems;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.*;
import net.minecraft.util.math.BlockPos;
import pe.apuyawar.systems.events.*;
import pe.apuyawar.systems.difficulty.DifficultyEngine;
import pe.apuyawar.systems.config.JudgmentConfig;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Solo se carga en el mod auxiliar del servidor de pruebas, nunca en el JAR normal. */
public class JudgmentGameTests implements FabricGameTest {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,tickLimit=100)
    public void recoveryAndDifficultyCatalog(TestContext context){
        var world=context.getWorld();var server=world.getServer();var config=new JudgmentConfig();
        var engine=new DifficultyEngine(server,config);
        String oldRule=server.getGameRules().toNbt().getString("randomTickSpeed");
        var journal=new pe.apuyawar.systems.persistence.WorldRuleJournal(server);
        journal.change("randomTickSpeed","2");
        new pe.apuyawar.systems.persistence.WorldRuleJournal(server).recover();
        context.assertTrue(server.getGameRules().toNbt().getString("randomTickSpeed").equals(oldRule),"Recuperación del diario tras reinicio simulado");
        var zombie=context.spawnEntity(EntityType.ZOMBIE,new BlockPos(2,2,2));
        double damage=zombie.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        context.assertTrue(engine.start(EventCatalog.DIFFICULTIES.get("zombis_feroces")),"Inicio de dificultad");
        engine.applyMob(zombie);engine.applyMob(zombie);
        context.assertTrue(Math.abs(zombie.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)-damage*1.12)<.00001,"Modificador único +12%");
        engine.stop();context.assertTrue(zombie.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)==damage,"Restauración ataque");
        var effects=new TemporaryEffects();
        zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,600,1));
        effects.apply(zombie,StatusEffects.SPEED,30,0);effects.clear();
        context.assertTrue(zombie.getStatusEffect(StatusEffects.SPEED).getAmplifier()==1,"Poción ajena conservada");
        effects.apply(zombie,StatusEffects.WEAKNESS,80,0);effects.clear();
        context.assertTrue(!zombie.hasStatusEffect(StatusEffects.WEAKNESS),"Limpieza efecto propio");
        effects.apply(zombie,StatusEffects.WEAKNESS,80,0);TemporaryEffects.recover(zombie);
        context.assertTrue(!zombie.hasStatusEffect(StatusEffects.WEAKNESS),"Recuperación de efecto marcado al cargar");
        zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,600,0));
        effects.apply(zombie,StatusEffects.SLOWNESS,80,1);
        context.assertTrue(zombie.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier()==1,"Lentitud II mejora temporalmente Lentitud I vanilla");
        effects.clear();context.assertTrue(zombie.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier()==0&&zombie.getStatusEffect(StatusEffects.SLOWNESS).getDuration()==600,"Restaura Lentitud I ajena al retirar dificultad");
        for(var difficulty:EventCatalog.DIFFICULTIES.values()){
            context.assertTrue(engine.start(difficulty),"Inicio "+difficulty.id());
            for(var action:difficulty.actions()){
                if(action.kind().equals("effect"))context.assertTrue(Registries.STATUS_EFFECT.getEntry(Identifier.ofVanilla(action.effect())).isPresent(),"Efecto válido "+difficulty.id());
                if(!action.entity().equals("hostile"))context.assertTrue(Registries.ENTITY_TYPE.containsId(Identifier.ofVanilla(action.entity())),"Entidad válida "+difficulty.id());
                if(action.kind().equals("attribute")){
                    Entity entity=context.spawnEntity(Registries.ENTITY_TYPE.get(Identifier.ofVanilla(action.entity().equals("hostile")?"zombie":action.entity())),new BlockPos(4,2,4));
                    if(entity instanceof net.minecraft.entity.mob.MobEntity mob){
                        var key=switch(action.attribute()){case "max_health"->EntityAttributes.GENERIC_MAX_HEALTH;case "movement_speed"->EntityAttributes.GENERIC_MOVEMENT_SPEED;default->EntityAttributes.GENERIC_FOLLOW_RANGE;};
                        double original=mob.getAttributeValue(key);engine.applyMob(mob);engine.applyMob(mob);
                        context.assertTrue(Math.abs(mob.getAttributeValue(key)-action.value())<.00001,"Atributo absoluto recuperado: "+difficulty.id());
                        DifficultyEngine.recover(mob);context.assertTrue(Math.abs(mob.getAttributeValue(key)-original)<.00001,"Restauración de base "+difficulty.id());
                    }entity.discard();
                }
            }
            engine.applyMob(zombie);engine.stop();
            context.assertTrue(!engine.active(),"Finalización "+difficulty.id());
            context.assertTrue(!zombie.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).hasModifier(DifficultyEngine.MODIFIER),"Sin modificadores duplicados");
        }
        zombie.discard();testChallenges(context);context.complete();
    }
    private void testChallenges(TestContext context){
        var world=context.getWorld();var server=world.getServer();var service=JudgmentService.get(server);
        service.config.challengeSecondsOverride=1;service.config.maxHazardMobs=0;
        var first=createParticipant(context);var second=createParticipant(context);
        first.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);second.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
        first.getAbilities().invulnerable=true;second.getAbilities().invulnerable=true;
        var origin=context.getAbsolutePos(new BlockPos(3,2,3));
        for(var c:EventCatalog.CHALLENGES.values()){
            first.setHealth(20);second.setHealth(20);
            for(var p:java.util.List.of(first,second)){
                p.getInventory().clear();
                double y=c.goal().equals("height")||c.goal().equals("gas")?c.target()+1:c.goal().equals("depth")||c.goal().equals("torch_depth")?c.target()-1:c.goal().startsWith("roof")?-63:300;
                var dimension=c.goal().equals("nether")?server.getWorld(net.minecraft.world.World.NETHER):world;
                p.teleport(dimension,origin.getX(),y,origin.getZ(),0,0);
                if(c.goal().equals("shield")||c.goal().equals("roof_shield"))p.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND,new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHIELD));
                if(c.goal().equals("bow"))p.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND,new net.minecraft.item.ItemStack(net.minecraft.item.Items.BOW));
                if(c.goal().equals("apple"))p.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND,new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLDEN_APPLE));
                if(c.goal().equals("torch_depth"))p.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND,new net.minecraft.item.ItemStack(net.minecraft.item.Items.TORCH));
            }
            BlockPos roof=first.getBlockPos().up(2);
            if(c.goal().startsWith("roof"))world.setBlockState(roof,net.minecraft.block.Blocks.STONE.getDefaultState());
            if(c.goal().equals("destination"))for(int n=1;n<=5;n++)service.config.refuges.put(Integer.toString(n),new JudgmentConfig.Refuge("minecraft:overworld",first.getX(),first.getY(),first.getZ(),12));
            context.assertTrue(service.challenges.start(c,java.util.List.of(first,second)),"Inicio real "+c.id());
            if(!c.goal().equals("survive"))context.assertTrue(service.challenges.passed(first),"Objetivo real con coordenadas/equipo/dimensión: "+c.id());
            for(int n=0;n<20&&service.challenges.active();n++)service.challenges.tick();
            context.assertTrue(!service.challenges.active(),"Finalización temporizador/objetivo: "+c.id());
            context.assertTrue(Boolean.TRUE.equals(service.challenges.lastResults().get(first.getUuid()))&&Boolean.TRUE.equals(service.challenges.lastResults().get(second.getUuid())),"Resultado dos jugadores: "+c.id());
            if(c.goal().startsWith("roof"))world.setBlockState(roof,net.minecraft.block.Blocks.AIR.getDefaultState());
        }
        var c=EventCatalog.CHALLENGES.get("muro_viviente");first.equipStack(EquipmentSlot.OFFHAND,new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHIELD));second.equipStack(EquipmentSlot.OFFHAND,new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHIELD));
        first.teleport(world,origin.getX(),300,origin.getZ(),0,0);second.teleport(world,origin.getX(),300,origin.getZ(),0,0);
        service.challenges.start(c,java.util.List.of(first,second));service.challenges.fail(first.getUuid());
        for(int n=0;n<20;n++)service.challenges.tick();
        context.assertTrue(Boolean.FALSE.equals(service.challenges.lastResults().get(first.getUuid()))&&Boolean.TRUE.equals(service.challenges.lastResults().get(second.getUuid())),"Fracaso individual no castiga al compañero");
        service.config.challengeSecondsOverride=10;service.config.maxHazardMobs=3;
        var hazard=new EventCatalog.Challenge("hazard_check","Prueba interna","Prueba interna","survive",0,200,"global",true,java.util.Map.of("ce",1));
        service.challenges.start(hazard,java.util.List.of(first,second));
        for(int n=0;n<10;n++)service.challenges.tick();
        context.assertTrue(service.challenges.hazardCount()>0&&service.challenges.hazardCount()<=3,"Spawn real de peligros y límite");
        service.challenges.stop();
        context.assertTrue(java.util.stream.StreamSupport.stream(world.iterateEntities().spliterator(),false).noneMatch(e->e.getCommandTags().contains(pe.apuyawar.systems.challenges.ChallengeEngine.HAZARD_TAG)),"Limpieza real de criaturas del juicio");
        server.getPlayerManager().remove(first);server.getPlayerManager().remove(second);
    }
    private net.minecraft.server.network.ServerPlayerEntity createParticipant(TestContext context){
        var server=context.getWorld().getServer();
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"JuicioTest");
        var data=net.minecraft.server.network.ConnectedClientData.createDefault(profile,false);
        var player=new net.minecraft.server.network.ServerPlayerEntity(server,context.getWorld(),profile,data.syncedOptions());
        var connection=new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerManager().onPlayerConnect(connection,player,data);
        return player;
    }
}
