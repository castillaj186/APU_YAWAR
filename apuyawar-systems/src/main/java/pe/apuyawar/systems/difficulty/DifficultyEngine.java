package pe.apuyawar.systems.difficulty;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.*;
import net.minecraft.item.*;
import net.minecraft.block.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.*;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import pe.apuyawar.systems.events.*;
import pe.apuyawar.systems.config.JudgmentConfig;
import pe.apuyawar.systems.persistence.WorldRuleJournal;

/** Una dificultad a la vez: evita combinaciones incompatibles y acumulación. */
public final class DifficultyEngine {
    public static final Identifier MODIFIER=Identifier.of("apuyawar_systems","difficulty");
    private final MinecraftServer server;private final JudgmentConfig config;private final WorldRuleJournal journal;
    private final TemporaryEffects effects=new TemporaryEffects();
    private final Set<LivingEntity> modified=new HashSet<>();
    private final Set<Entity> hazards=new HashSet<>();
    private EventCatalog.Difficulty current;private int remaining,ticks;
    private final Map<PassiveEntity,Integer> breeding=new HashMap<>();
    private String owner;
    public boolean owns(Entity entity){return active()&&entity.getCommandTags().contains(owner);}
    public void recoverWorldRules(){journal.recover();}
    public DifficultyEngine(MinecraftServer server,JudgmentConfig config){this.server=server;this.config=config;journal=new WorldRuleJournal(server);}
    public boolean active(){return current!=null;}
    public int code(){return active()?current.legacy():0;}
    public boolean on(int code){return code()==code;}
    public String status(){return active()?current.id()+" | "+(remaining+19)/20+" s":"Sin dificultad activa";}
    public boolean start(EventCatalog.Difficulty difficulty){
        if(active()||!config.orangeEnabled||config.disabledDifficulties.contains(difficulty.id()))return false;
        current=difficulty;owner="apuyawar_owner/"+UUID.randomUUID();remaining=config.difficultySeconds*20;ticks=0;
        int old=code()-1000;
        Map<Integer,String> rules=Map.ofEntries(Map.entry(40,"difficulty hard"),Map.entry(61,"randomTickSpeed 2"),Map.entry(62,"playersSleepingPercentage 101"),Map.entry(63,"doInsomnia true"),Map.entry(82,"doFireTick true"),Map.entry(83,"mobExplosionDropDecay true"),Map.entry(84,"maxEntityCramming 8"),Map.entry(85,"spectatorsGenerateChunks false"),Map.entry(86,"reducedDebugInfo true"),Map.entry(87,"forgiveDeadPlayers false"));
        if(rules.containsKey(old)){String[] rule=rules.get(old).split(" ");journal.change(rule[0],rule[1]);}
        for(var p:server.getPlayerManager().getPlayerList())announce(p);
        return true;
    }
    public void announce(ServerPlayerEntity p){if(active())JudgmentPresentation.announce(p,"EL JUICIO SE INTENSIFICA","¡Una nueva dificultad ha despertado!",current.name()+" — "+current.description()+" | "+config.difficultySeconds+" s",true);}
    public void tick(){
        if(!active())return;ticks++;remaining--;if(remaining<=0){stop();return;}
        if(ticks%20!=0)return;
        effects.prune();hazards.removeIf(Entity::isRemoved);
        Set<MobEntity> mobs=new HashSet<>();
        for(var p:server.getPlayerManager().getPlayerList()){
            if(!p.isAlive()||p.isCreative()||p.isSpectator())continue;
            applyPlayer(p);
            if(p.getWorld().getRegistryKey()==World.OVERWORLD||code()>1000)
                for(MobEntity mob:p.getServerWorld().getEntitiesByClass(MobEntity.class,p.getBoundingBox().expand(38),Entity::isAlive))if(mobs.size()<256)mobs.add(mob);
        }
        for(MobEntity mob:mobs)applyMob(mob);
        modified.removeIf(Entity::isRemoved);
    }
    public void loaded(Entity entity){
        if(entity instanceof LivingEntity living){recover(living);TemporaryEffects.recover(living);}
        if(owns(entity)&&!hazards.contains(entity)){if(hazards.size()>=config.maxHazardMobs){entity.discard();return;}hazards.add(entity);}
        if(active()&&entity instanceof MobEntity mob)applyMob(mob);
    }
    public static void recover(LivingEntity entity){for(var key:List.of(EntityAttributes.GENERIC_ATTACK_DAMAGE,EntityAttributes.GENERIC_MOVEMENT_SPEED,EntityAttributes.GENERIC_FOLLOW_RANGE,EntityAttributes.GENERIC_ARMOR,EntityAttributes.GENERIC_MAX_HEALTH)) {var attr=entity.getAttributeInstance(key);if(attr!=null)attr.removeModifier(MODIFIER);}entity.setHealth(Math.min(entity.getHealth(),entity.getMaxHealth()));if(entity instanceof CreeperEntity creeper&&entity.getCommandTags().contains("apuyawar_temp_charged")){charge(creeper,false);entity.removeCommandTag("apuyawar_temp_charged");}}
    private static void charge(CreeperEntity creeper,boolean powered){var data=new net.minecraft.nbt.NbtCompound();creeper.writeCustomDataToNbt(data);data.putBoolean("powered",powered);creeper.readCustomDataFromNbt(data);}
    private static final Set<String> HISTORIC_HOSTILES=Set.of("zombie","skeleton","creeper","spider","cave_spider","enderman","witch","slime","phantom","drowned","husk","stray","blaze","ghast","piglin","piglin_brute","hoglin","zoglin","wither_skeleton","magma_cube","guardian","elder_guardian","shulker","silverfish","endermite","evoker","vindicator","ravager","pillager","vex");
    private boolean matches(LivingEntity entity,String type){String name=Registries.ENTITY_TYPE.getId(entity.getType()).getPath();return type.equals("hostile")?HISTORIC_HOSTILES.contains(name):name.equals(type);}
    private void attribute(LivingEntity entity,net.minecraft.registry.entry.RegistryEntry<EntityAttribute> key,double value,boolean add){
        var attr=entity.getAttributeInstance(key);if(attr==null)return;
        if(!attr.hasModifier(MODIFIER)){float fraction=entity.getHealth()/entity.getMaxHealth();attr.addTemporaryModifier(new EntityAttributeModifier(MODIFIER,value,add?EntityAttributeModifier.Operation.ADD_VALUE:EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));if(key.equals(EntityAttributes.GENERIC_MAX_HEALTH))entity.setHealth(fraction*entity.getMaxHealth());}
        modified.add(entity);
    }
    public void applyMob(MobEntity mob){
        if(!active())return;
        if(code()<=35&&mob.getWorld().getRegistryKey()!=World.OVERWORLD){recover(mob);return;}
        int c=code();boolean zombie=mob instanceof ZombieEntity && !(mob instanceof DrownedEntity);
        boolean night=mob.getWorld().getTimeOfDay()%24000>=13000&&mob.getWorld().getTimeOfDay()%24000<=23000;
        if(c==23&&!night){var attr=mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);if(attr!=null)attr.removeModifier(MODIFIER);}
        if((c==1&&zombie)||(c==5&&mob instanceof HostileEntity)||(c==16&&mob instanceof PillagerEntity))attribute(mob,c==1?EntityAttributes.GENERIC_ATTACK_DAMAGE:EntityAttributes.GENERIC_FOLLOW_RANGE,c==16?.15:.12,false);
        Map<Integer,String> speed=Map.ofEntries(Map.entry(3,"creeper"),Map.entry(8,"drowned"),Map.entry(10,"zombie"),Map.entry(13,"spider"),Map.entry(18,"witch"),Map.entry(22,"silverfish"),Map.entry(23,"skeleton"),Map.entry(26,"phantom"),Map.entry(29,"slime"),Map.entry(32,"pillager"));
        if(speed.containsKey(c)&&matches(mob,speed.get(c))&&(c!=23||night))attribute(mob,EntityAttributes.GENERIC_MOVEMENT_SPEED,c==10?.06:c==22?.16:c==26?.14:c==3||c==23||c==32?.10:.12,false);
        if(c==20&&zombie)attribute(mob,EntityAttributes.GENERIC_ARMOR,2,true);
        if(c==31&&mob instanceof CreeperEntity)attribute(mob,EntityAttributes.GENERIC_ARMOR,3,true);
        if(c==25&&mob instanceof ZombieVillagerEntity)attribute(mob,EntityAttributes.GENERIC_MAX_HEALTH,.12,false);
        if(c==34&&zombie&&mob.getHealth()<mob.getMaxHealth()/2)effects.apply(mob,StatusEffects.STRENGTH,100,0);
        if(c==11&&mob instanceof VillagerEntity villager)holdBreeding(villager);
        if(c>1000){for(var action:current.actions())if(matches(mob,action.entity())){
            if(action.kind().equals("attribute")){
                var key=switch(action.attribute()){case "max_health"->EntityAttributes.GENERIC_MAX_HEALTH;case "movement_speed"->EntityAttributes.GENERIC_MOVEMENT_SPEED;case "follow_range"->EntityAttributes.GENERIC_FOLLOW_RANGE;default->null;};
                if(key!=null){var attr=mob.getAttributeInstance(key);if(attr!=null)attribute(mob,key,action.value()-attr.getBaseValue(),true);}
            }else if(condition(mob,action.condition()))Registries.STATUS_EFFECT.getEntry(Identifier.ofVanilla(action.effect())).ifPresent(effect->effects.apply(mob,effect,action.duration(),action.amplifier()));
        }applyLegacyMob(mob,c-1000);}
    }
    private boolean condition(MobEntity mob,String condition){return switch(condition){case "iron"->mob.getWorld().getBlockState(mob.getBlockPos().down()).isOf(Blocks.IRON_ORE);case "farmland"->mob.getWorld().getBlockState(mob.getBlockPos().down()).isOf(Blocks.FARMLAND);case "water"->mob.isTouchingWater();case "near"->mob.getWorld().getClosestPlayer(mob,12)!=null;default->true;};}
    private void holdBreeding(PassiveEntity mob){if(mob.getBreedingAge()==0||breeding.containsKey(mob)){breeding.putIfAbsent(mob,0);mob.setBreedingAge(100);}}
    private void applyPlayer(ServerPlayerEntity p){
        if(code()<=35&&p.getWorld().getRegistryKey()!=World.OVERWORLD)return;
        if(on(6)&&p.isOnFire()&&!p.isTouchingWater())p.setFireTicks(Math.max(p.getFireTicks(),100));
        if(on(9)&&p.isSprinting())p.addExhaustion(.0275f);
        if(on(14)&&p.getHealth()<p.getMaxHealth()/2)effects.apply(p,StatusEffects.HUNGER,30,0);
        if(on(19)&&p.getY()<0&&ticks%240==0)effects.apply(p,StatusEffects.DARKNESS,70,0);
        if(on(30)&&p.getY()<-20&&ticks%240==0)effects.apply(p,StatusEffects.MINING_FATIGUE,90,0);
        if(on(33)&&p.getServerWorld().hasRain(p.getBlockPos()))effects.apply(p,StatusEffects.SLOWNESS,30,0);
        if(on(35)&&p.isSprinting()&&fullDiamond(p))p.addExhaustion(.035f);
        if(code()>1000)applyLegacyPlayer(p,code()-1000);
    }
    private static boolean fullDiamond(ServerPlayerEntity p){return p.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.DIAMOND_HELMET)&&p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.DIAMOND_CHESTPLATE)&&p.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.DIAMOND_LEGGINGS)&&p.getEquippedStack(EquipmentSlot.FEET).isOf(Items.DIAMOND_BOOTS);}
    public void damage(LivingEntity entity,DamageSource source,float amount,boolean blocked){
        if(!active()||blocked||amount<=0||!(entity instanceof ServerPlayerEntity p)||p.isCreative()||p.isSpectator()||code()>35||p.getWorld().getRegistryKey()!=World.OVERWORLD)return;
        Entity attacker=source.getAttacker();
        if(attacker instanceof SkeletonEntity){if(on(2))effects.apply(p,StatusEffects.WEAKNESS,80,0);if(on(12))effects.apply(p,StatusEffects.SLOWNESS,80,0);}
        if(on(4)&&source.isIn(DamageTypeTags.IS_FALL))effects.apply(p,StatusEffects.SLOWNESS,80,0);
        if(on(15)&&source.isIn(DamageTypeTags.IS_EXPLOSION)&&attacker instanceof CreeperEntity)effects.apply(p,StatusEffects.WEAKNESS,120,0);
        if(on(17)&&attacker instanceof SpiderEntity&&!(attacker instanceof CaveSpiderEntity))effects.apply(p,StatusEffects.POISON,60,0);
        if(on(24)&&attacker instanceof HuskEntity)effects.apply(p,StatusEffects.HUNGER,100,0);
        if(on(27)&&attacker instanceof StrayEntity)effects.apply(p,StatusEffects.SLOWNESS,80,1);
        if(on(28)&&attacker instanceof CaveSpiderEntity)effects.apply(p,StatusEffects.POISON,100,0);
    }
    public ActionResult useBlock(ServerPlayerEntity p,Hand hand,BlockPos position,Direction face){
        if(!active()||p.isCreative()||p.isSpectator())return ActionResult.PASS;
        var world=p.getServerWorld();var stack=p.getStackInHand(hand);
        if(on(21)&&world.getRegistryKey()==World.OVERWORLD&&world.getBlockState(position).isOf(Blocks.ENCHANTING_TABLE)&&p.experienceLevel<15){p.sendMessage(Text.literal("APU YAWAR | Encantar requiere nivel 15."),true);return ActionResult.FAIL;}
        if(on(7)&&world.getRegistryKey()==World.OVERWORLD&&stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof DoorBlock&&(world.getFluidState(position.offset(face)).isIn(FluidTags.WATER)||world.getFluidState(position.offset(face).up()).isIn(FluidTags.WATER)))return ActionResult.FAIL;
        if(on(1081)&&stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof TorchBlock&&world.getFluidState(position.offset(face)).isIn(FluidTags.WATER))return ActionResult.FAIL;
        return ActionResult.PASS;
    }
    public ActionResult useEntity(ServerPlayerEntity p,Entity entity,Hand hand){
        if(!active()||p.isCreative()||p.isSpectator())return ActionResult.PASS;
        Item item=p.getStackInHand(hand).getItem();int c=code()-1000;
        if((c==88&&entity instanceof MooshroomEntity&&(item==Items.BUCKET||item==Items.BOWL))||((c==89||c==92)&&entity instanceof CowEntity&&item==Items.BUCKET))return ActionResult.FAIL;
        if(c==69&&entity instanceof ZombieVillagerEntity&&item==Items.GOLDEN_APPLE)return ActionResult.FAIL;
        return ActionResult.PASS;
    }
    public void broken(ServerPlayerEntity p,BlockState state){
        if(!active()||p.isCreative()||p.isSpectator())return;
        int c=code()-1000;EntityType<?> type=null;
        if(c==41&&state.isIn(BlockTags.LEAVES))type=EntityType.SILVERFISH;
        if(c==42&&(state.isOf(Blocks.COAL_ORE)||state.isOf(Blocks.DEEPSLATE_COAL_ORE)))type=EntityType.BAT;
        if(c==80&&state.isOf(Blocks.COBWEB))type=EntityType.CAVE_SPIDER;
        if(type!=null&&hazards.size()<config.maxHazardMobs&&p.getServerWorld().random.nextFloat()<.15){Entity entity=type.create(p.getServerWorld());if(entity!=null){entity.refreshPositionAndAngles(p.getX()+2,p.getY(),p.getZ(),0,0);entity.addCommandTag(pe.apuyawar.systems.challenges.ChallengeEngine.HAZARD_TAG);entity.addCommandTag(owner);if(entity instanceof LivingEntity living)living.disableExperienceDropping();hazards.add(entity);if(!p.getServerWorld().spawnEntity(entity))hazards.remove(entity);}}
    }
    private void applyLegacyPlayer(ServerPlayerEntity p,int c){
        BlockState at=p.getWorld().getBlockState(p.getBlockPos()),under=p.getWorld().getBlockState(BlockPos.ofFloored(p.getX(),p.getY()-.2,p.getZ()));
        Map<Integer,Block> blocks=Map.ofEntries(Map.entry(1,Blocks.MAGMA_BLOCK),Map.entry(71,Blocks.CACTUS),Map.entry(72,Blocks.MAGMA_BLOCK),Map.entry(73,Blocks.SWEET_BERRY_BUSH),Map.entry(74,Blocks.CAMPFIRE),Map.entry(75,Blocks.POINTED_DRIPSTONE),Map.entry(76,Blocks.WITHER_ROSE),Map.entry(77,Blocks.POWDER_SNOW),Map.entry(78,Blocks.SOUL_CAMPFIRE));
        if(blocks.containsKey(c)&&(at.isOf(blocks.get(c))||under.isOf(blocks.get(c))))p.damage(p.getDamageSources().magic(),c==1?100:c==77?2:c==71||c==72||c==78?4:3);
        if(c==12&&!p.getServerWorld().getEntitiesByClass(PhantomEntity.class,p.getBoundingBox().expand(4),Entity::isAlive).isEmpty())effects.apply(p,StatusEffects.POISON,80,0);
        if(c==45&&p.getMainHandStack().isOf(Items.DIAMOND)&&!p.getServerWorld().getEntitiesByClass(HostileEntity.class,p.getBoundingBox().expand(16),Entity::isAlive).isEmpty())effects.apply(p,StatusEffects.GLOWING,60,0);
        if(c==51&&p.getWorld().getRegistryKey()==World.NETHER&&ticks%100==0&&!p.getServerWorld().getEntitiesByClass(HostileEntity.class,p.getBoundingBox().expand(8),Entity::isAlive).isEmpty())effects.apply(p,StatusEffects.BLINDNESS,40,0);
        if(c==52&&p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.NETHERITE_CHESTPLATE)&&!p.getServerWorld().getEntitiesByClass(PhantomEntity.class,p.getBoundingBox().expand(16),Entity::isAlive).isEmpty())effects.apply(p,StatusEffects.GLOWING,40,0);
        if((c==9||c==79)&&!p.getServerWorld().getEntitiesByClass(SpiderEntity.class,p.getBoundingBox().expand(c==9?3:4),Entity::isAlive).isEmpty())effects.apply(p,StatusEffects.SLOWNESS,30,1);
        if(c==81&&p.isTouchingWater()&&(p.getMainHandStack().isOf(Items.TORCH)||p.getOffHandStack().isOf(Items.TORCH)))effects.apply(p,StatusEffects.MINING_FATIGUE,30,0);
        if(c==89||c==92){for(int slot=0;slot<p.getInventory().size();slot++)if(p.getInventory().getStack(slot).isOf(Items.MILK_BUCKET))p.getInventory().setStack(slot,new ItemStack(Items.BUCKET));}
    }
    private void applyLegacyMob(MobEntity mob,int c){
        if(c==64&&mob instanceof VillagerEntity v)holdBreeding(v);
        if(c>=65&&c<=68){String target=List.of("cow","pig","sheep","chicken").get(c-65);if(matches(mob,target)&&mob instanceof AnimalEntity animal){int nearby=mob.getWorld().getEntitiesByClass(AnimalEntity.class,mob.getBoundingBox().expand(48),e->matches(e,target)).size();if(nearby>=(c==68?25:20))holdBreeding(animal);}}
        if(c==70&&mob instanceof IronGolemEntity&&!mob.hasCustomName())mob.damage(mob.getDamageSources().generic(),2);
        if((c==44||c==96)&&mob instanceof PiglinEntity){var player=mob.getWorld().getClosestPlayer(mob,12);if(player!=null&&(c==44&&mob.getWorld().getBlockState(mob.getBlockPos().down()).isOf(Blocks.GOLD_ORE)||c==96&&!player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.GOLDEN_HELMET)&&!player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.GOLDEN_CHESTPLATE)&&!player.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.GOLDEN_LEGGINGS)&&!player.getEquippedStack(EquipmentSlot.FEET).isOf(Items.GOLDEN_BOOTS)))mob.setTarget(player);}
        if(c==48&&mob instanceof CreeperEntity creeper&&mob.getWorld().getBlockState(mob.getBlockPos().down()).isOf(Blocks.REDSTONE_ORE)&&!creeper.shouldRenderOverlay()){charge(creeper,true);creeper.addCommandTag("apuyawar_temp_charged");modified.add(creeper);}
        if(c==90&&mob instanceof SkeletonEntity)attribute(mob,EntityAttributes.GENERIC_FOLLOW_RANGE,8,true);
    }
    public void stop(){
        if(!active())return;
        String name=current.name();effects.clear();modified.forEach(DifficultyEngine::recover);modified.clear();
        breeding.forEach((e,age)->{if(!e.isRemoved()&&e.getBreedingAge()<=100&&e.getBreedingAge()>=0)e.setBreedingAge(age);});breeding.clear();
        for(var world:server.getWorlds())for(Entity e:world.iterateEntities())if(owns(e))e.discard();
        hazards.clear();journal.restore();current=null;
        for(var p:server.getPlayerManager().getPlayerList())p.sendMessage(Text.literal("APU YAWAR | Finalizó la dificultad: "+name),false);
    }
}
