package pe.apuyawar.systems.challenges;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.entity.boss.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.*;
import pe.apuyawar.systems.config.JudgmentConfig;
import pe.apuyawar.systems.events.*;

/** Un reto activo, resultados independientes por UUID y objetivos globales explícitos. */
public final class ChallengeEngine {
    public static final String HAZARD_TAG="apuyawar_judgment_hazard";
    public static final Identifier HEART=Identifier.of("apuyawar_systems","challenge_heart");
    private final MinecraftServer server;private final JudgmentConfig config;
    private final TemporaryEffects effects=new TemporaryEffects();
    private final Map<UUID,Boolean> participants=new LinkedHashMap<>();
    private final Map<UUID,Boolean> lastResults=new LinkedHashMap<>();
    public Map<UUID,Boolean> lastResults(){return Map.copyOf(lastResults);}
    private final List<MobEntity> mobs=new ArrayList<>();
    private final Map<UUID,Integer> gasExposure=new HashMap<>();
    private static final Map<String,EntityType<?>> HAZARD_TYPES=Map.ofEntries(Map.entry("cr",EntityType.CREEPER),Map.entry("ce",EntityType.CREEPER),Map.entry("cn",EntityType.CREEPER),Map.entry("zb",EntityType.ZOMBIE),Map.entry("sk",EntityType.SKELETON),Map.entry("sp",EntityType.SPIDER),Map.entry("ph",EntityType.PHANTOM),Map.entry("hu",EntityType.HUSK),Map.entry("wi",EntityType.WITCH),Map.entry("vi",EntityType.VINDICATOR),Map.entry("hd",EntityType.ZOMBIE),Map.entry("gh",EntityType.GHAST),Map.entry("vx",EntityType.VEX),Map.entry("rv",EntityType.RAVAGER));
    private final ServerBossBar bar=new ServerBossBar(Text.literal("MOMENTO DEL JUICIO"),BossBar.Color.RED,BossBar.Style.PROGRESS);
    private EventCatalog.Challenge challenge;private int elapsed,duration;private JudgmentConfig.Refuge refuge;
    private String owner;
    public boolean owns(Entity entity){return active()&&entity.getCommandTags().contains(owner);}
    public int hazardCount(){return mobs.size();}
    public void loaded(Entity entity){if(owns(entity)&&entity instanceof MobEntity mob&&!mobs.contains(mob)){if(mobs.size()>=config.maxHazardMobs)mob.discard();else mobs.add(mob);}}
    public ChallengeEngine(MinecraftServer server,JudgmentConfig config){this.server=server;this.config=config;}
    public boolean active(){return challenge!=null;}
    public String status(){return active()?challenge.id()+" | "+Math.max(0,(duration-elapsed+19)/20)+" s | participantes: "+participants.size():"Sin reto activo";}
    public boolean start(EventCatalog.Challenge selected,Collection<ServerPlayerEntity> players){
        if(active()||!config.redEnabled||config.disabledChallenges.contains(selected.id()))return false;
        participants.clear();gasExposure.clear();players.stream().filter(ChallengeEngine::eligible).forEach(p->participants.put(p.getUuid(),false));
        if(participants.isEmpty())return false;
        challenge=selected;owner="apuyawar_owner/"+UUID.randomUUID();elapsed=0;duration=config.challengeSecondsOverride>0?config.challengeSecondsOverride*20:selected.duration();
        if(selected.goal().equals("destination")){
            int dest=selected.parameter("dest");if(dest==0)dest=server.getOverworld().random.nextBetween(1,5);
            refuge=config.refuges.get(Integer.toString(dest));
            if(refuge==null){BlockPos spawn=server.getOverworld().getSpawnPos();refuge=new JudgmentConfig.Refuge("minecraft:overworld",spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,12);}
        }
        bar.setName(Text.literal(selected.name()));bar.setPercent(1);bar.setVisible(true);
        for(var player:players)if(participants.containsKey(player.getUuid())){
            bar.addPlayer(player);
            String subtitle=Set.of("survive","roof","roof_shield","destination").contains(selected.goal())?"¡Escóndanse o sobrevivan!":"¡Cumplan el reto antes de que termine el tiempo!";
            String objective=selected.goal().equals("destination")?"Reúnanse en "+refuge.dimension()+" X="+(int)refuge.x()+" Y="+(int)refuge.y()+" Z="+(int)refuge.z()+"; radio "+refuge.radius():selected.description();
            JudgmentPresentation.announce(player,"MOMENTO DEL JUICIO",subtitle,selected.name()+" — "+objective+" | "+duration/20+" s",false);
        }return true;
    }
    public static boolean eligible(ServerPlayerEntity p){return p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&!p.getCommandTags().contains("apuyawar_no_juicio");}
    public void join(ServerPlayerEntity player){
        TemporaryEffects.recover(player);removeHeart(player);
        if(!active())return;
        if(config.joinDuringChallenge&&eligible(player))participants.putIfAbsent(player.getUuid(),false);
        if(participants.containsKey(player.getUuid())&&!participants.get(player.getUuid()))bar.addPlayer(player);
        player.sendMessage(Text.literal("APU YAWAR | "+status()+". "+(participants.containsKey(player.getUuid())?"Participas en este juicio.":"Participarás en el próximo juicio.")),false);
    }
    public void fail(UUID uuid){if(participants.containsKey(uuid))participants.put(uuid,true);}
    public void tick(){
        if(!active())return;
        elapsed++;mobs.removeIf(m->m.isRemoved()||!m.isAlive());effects.prune();
        for(UUID uuid:participants.keySet()){
            ServerPlayerEntity p=server.getPlayerManager().getPlayer(uuid);
            if(p==null||!eligible(p)){participants.put(uuid,true);continue;}
            if(participants.get(uuid))continue;
            if(challenge.parameter("onehp")>0){var a=p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);if(a!=null&&!a.hasModifier(HEART))a.addTemporaryModifier(new EntityAttributeModifier(HEART,2-a.getBaseValue(),EntityAttributeModifier.Operation.ADD_VALUE));p.setHealth(Math.min(p.getHealth(),p.getMaxHealth()));}
            if(elapsed%20==0){
                if(challenge.parameter("blind")>0)effects.apply(p,StatusEffects.BLINDNESS,30,0);
                if(challenge.goal().equals("gas")&&elapsed>=challenge.parameter("gg")){
                    double threshold=challenge.parameter("rise")>0?challenge.target()*(double)elapsed/duration:challenge.target();
                    if(p.getY()<threshold){int exposure=gasExposure.merge(uuid,1,Integer::sum);p.damage(p.getDamageSources().magic(),Math.min(4,1+(exposure-1)/5));}
                    else gasExposure.computeIfPresent(uuid,(key,value)->Math.max(0,value-2));
                }
            }
            hazards(p);
        }
        if(elapsed%20==0){bar.setPercent(Math.max(0,1-elapsed/(float)duration));bar.setName(Text.literal(challenge.name()+" | "+(duration-elapsed+19)/20+" s"));}
        boolean all=participants.entrySet().stream().allMatch(e->!e.getValue()&&passed(server.getPlayerManager().getPlayer(e.getKey())));
        boolean lost=participants.values().stream().allMatch(Boolean::booleanValue);
        if(lost||elapsed>=duration||(!challenge.atDeadline()&&all))finish(false);
    }
    public boolean passed(ServerPlayerEntity p){
        if(p==null||!eligible(p))return false;
        boolean roof=!p.getServerWorld().isSkyVisible(p.getBlockPos());
        boolean shield=p.getMainHandStack().isOf(Items.SHIELD)||p.getOffHandStack().isOf(Items.SHIELD);
        boolean destination=refuge!=null&&p.getWorld().getRegistryKey().getValue().toString().equals(refuge.dimension())&&p.getPos().squaredDistanceTo(refuge.x(),refuge.y(),refuge.z())<=refuge.radius()*refuge.radius();
        return ChallengeRules.passed(challenge,new ChallengeRules.PlayerFacts(p.getY(),p.getWorld().getRegistryKey().getValue().toString(),roof,shield,
            p.getMainHandStack().isOf(Items.BOW),p.getMainHandStack().isOf(Items.GOLDEN_APPLE)||p.getOffHandStack().isOf(Items.GOLDEN_APPLE),
            p.getMainHandStack().isOf(Items.TORCH)||p.getOffHandStack().isOf(Items.TORCH),destination,true),elapsed>=duration);
    }
    private void hazards(ServerPlayerEntity player){
        if(challenge.parameters().isEmpty()||(challenge.goal().equals("destination")&&passed(player)))return;
        for(var entry:HAZARD_TYPES.entrySet()){int interval=challenge.parameter(entry.getKey());if(interval>0&&elapsed%interval==0){String countKey=switch(entry.getKey()){case "ce"->"cen";case "cn"->"cnn";case "hd"->"hn";default->"mc";};int count=Math.max(1,challenge.parameter(countKey));for(int n=0;n<Math.min(3,count);n++)spawn(player,entry.getValue(),entry.getKey());}}
        for(String key:List.of("tnt","mt","st")){int interval=challenge.parameter(key);if(interval>0&&elapsed%interval==0){ServerWorld world=player.getServerWorld();world.spawnParticles(ParticleTypes.EXPLOSION,player.getX(),player.getY()+1,player.getZ(),2,1,1,1,0);player.damage(player.getDamageSources().magic(),key.equals("tnt")?4:2);}}
        if(challenge.parameter("storm")>0&&elapsed%100==0)effects.apply(player,StatusEffects.DARKNESS,30,0);
        // Giros históricos de ceguera/oscuridad: temporales y sin teletransporte inseguro.
        if(challenge.parameter("twist")>0&&elapsed%600==0)effects.apply(player,StatusEffects.DARKNESS,60,0);
    }
    private void spawn(ServerPlayerEntity player,EntityType<?> type,String kind){
        if(mobs.size()>=config.maxHazardMobs)return;
        ServerWorld world=player.getServerWorld();Entity entity=type.create(world);if(!(entity instanceof MobEntity mob))return;
        double angle=world.random.nextDouble()*Math.PI*2;BlockPos pos=player.getBlockPos().add((int)(Math.cos(angle)*6),0,(int)(Math.sin(angle)*6));
        if(!world.isChunkLoaded(pos)||!world.getBlockState(pos).isAir()||!world.getBlockState(pos.up()).isAir())return;
        mob.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);mob.addCommandTag(HAZARD_TAG);mob.addCommandTag(owner);mob.setPersistent();mob.setTarget(player);
        mob.initialize(world,world.getLocalDifficulty(pos),SpawnReason.EVENT,null);
        if(mob instanceof net.minecraft.entity.mob.ZombieEntity){mob.equipStack(EquipmentSlot.MAINHAND,new net.minecraft.item.ItemStack(Items.IRON_SWORD));mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SPEED,Math.max(1,duration-elapsed),0));}
        var data=new net.minecraft.nbt.NbtCompound();mob.writeCustomDataToNbt(data);data.putString("DeathLootTable","minecraft:empty");
        if(kind.equals("ce")||kind.equals("cn"))data.putBoolean("powered",true);
        mob.readCustomDataFromNbt(data);
        for(EquipmentSlot slot:EquipmentSlot.values())mob.setEquipmentDropChance(slot,0);
        mob.disableExperienceDropping();mobs.add(mob);if(!world.spawnEntity(mob))mobs.remove(mob);
    }
    public void stop(){if(active())finish(true);}
    private void finish(boolean cancelled){
        lastResults.clear();
        boolean global=challenge.scope().equals("global");
        boolean team=participants.entrySet().stream().allMatch(e->!e.getValue()&&passed(server.getPlayerManager().getPlayer(e.getKey())));
        for(var entry:participants.entrySet()){var p=server.getPlayerManager().getPlayer(entry.getKey());if(p!=null){boolean success=!entry.getValue()&&passed(p)&&(!global||team);
            if(!cancelled)lastResults.put(entry.getKey(),success);
            p.sendMessage(Text.literal("APU YAWAR | "+challenge.name()+": "+(cancelled?"juicio cancelado sin castigos":success?"¡Juicio superado!":"Juicio no superado.")),false);removeHeart(p);}}
        effects.clear();for(var world:server.getWorlds())for(Entity entity:world.iterateEntities())if(owns(entity))entity.discard();mobs.clear();participants.clear();bar.clearPlayers();bar.setVisible(false);challenge=null;refuge=null;
    }
    public static void removeHeart(LivingEntity entity){var attribute=entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);if(attribute!=null)attribute.removeModifier(HEART);}
}
