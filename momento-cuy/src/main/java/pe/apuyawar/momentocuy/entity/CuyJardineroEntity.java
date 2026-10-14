package pe.apuyawar.momentocuy.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * El Cuy Jardinero: adaptado del {@code CuyJardineroEntity} del mod antiguo
 * {@code safio-content} (ver {@code 20_MOMENTO_CUY_DIMENSION.md} para el
 * proceso completo de recuperación — se desensambló y remapeó el bytecode
 * real a nombres de Yarn 1.21.1 antes de adaptar nada, nunca se copiaron
 * clases descompiladas a ciegas). Conserva íntegra su lógica de
 * comportamiento (agricultura, cosecha, canasta, comercio, refugio, apertura
 * de puertas/portones) a través de sus controladores y objetivos de IA
 * ({@link CuyGardenGoal}, {@link CuyHarvestGoal}, {@link CuyShelterGoal},
 * {@link CuyPauseGoal}, {@link CuyGateController}, {@link CuyFarmController},
 * {@link CuyTradeCatalog}), también adaptados sin cambiar su lógica.
 *
 * <p>Única simplificación deliberada: el mod antiguo tenía una clase base
 * {@code OtterEntity} compartida por 11 criaturas decorativas distintas (solo
 * una, la Nutria Jardinera, era el Cuy). Como este mod SOLO recupera al Cuy
 * Jardinero (no se pidieron las otras 10), sus objetivos base de animal
 * (nadar, escapar del peligro, apareamiento, deambular, mirar) se fusionaron
 * aquí directamente en vez de mantener una jerarquía de una sola clase — y el
 * ítem de apareamiento/tentación cambió de pescado (lógico para una nutria) a
 * trigo/zanahoria (más fiel a un cuy herbívoro de jardín), la única diferencia
 * de comportamiento respecto al original.
 */
public final class CuyJardineroEntity extends AnimalEntity implements Merchant {
    private static final TrackedData<String> ACTION = DataTracker.registerData(CuyJardineroEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> ACTION_SERIAL = DataTracker.registerData(CuyJardineroEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> FLEEING = DataTracker.registerData(CuyJardineroEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int fleeUntilTick = 0;
    private boolean hostileNearby = false;
    private final CuyGateController gateController = new CuyGateController(this);
    private final CuyTradeCatalog cuyTrades = new CuyTradeCatalog(this);
    private PlayerEntity cuyCustomer;
    private final CuyFarmController cuyFarm = new CuyFarmController(this);
    private int actionTicks = 0;
    private int autoCooldown = 120;
    private boolean walkingToGarden = false;

    public CuyJardineroEntity(EntityType<? extends CuyJardineroEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AnimalEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.19);
    }

    @Override
    protected void initGoals() {
        // Objetivos base de animal (antes en OtterEntity) -- ver javadoc de
        // la clase sobre por qué se fusionaron aquí.
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.65));
        this.goalSelector.add(1, new FleeEntityGoal<>(this, HostileEntity.class, 9.0F, 1.35, 1.7));
        this.goalSelector.add(2, new AnimalMateGoal(this, 0.9));
        this.goalSelector.add(3, new TemptGoal(this, 1.0, stack -> stack.isOf(Items.WHEAT) || stack.isOf(Items.CARROT), false));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 5.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        // Objetivos propios del Cuy Jardinero (sin cambios de comportamiento).
        this.goalSelector.add(0, new CuyShelterGoal(this));
        this.goalSelector.add(2, new CuyPauseGoal(this));
        this.goalSelector.add(3, new CuyHarvestGoal(this));
        this.goalSelector.add(4, new CuyGardenGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.78));
    }

    public boolean isBreedingItem(ItemStack stack) {
        return stack.isOf(Items.WHEAT) || stack.isOf(Items.CARROT);
    }

    @Nullable
    public PassiveEntity createChild(ServerWorld world, PassiveEntity mate) {
        return (PassiveEntity) this.getType().create(world);
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RABBIT_AMBIENT;
    }

    public int getMinAmbientSoundDelay() {
        return 240;
    }

    boolean openShelterGate(BlockPos pos) {
        return this.gateController.openForShelter(pos);
    }

    boolean isCuyBusy() {
        return this.actionTicks > 0;
    }

    void setWalkingToGarden(boolean walking) {
        this.walkingToGarden = walking;
    }

    void startGardenAction(String action, int ticks) {
        this.playCuyAction(action, ticks);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ACTION, "");
        builder.add(ACTION_SERIAL, 0);
        builder.add(FLEEING, false);
    }

    public String getCuyAction() {
        return this.dataTracker.get(ACTION);
    }

    public int getCuyActionSerial() {
        return this.dataTracker.get(ACTION_SERIAL);
    }

    public boolean getCuyFleeing() {
        return this.dataTracker.get(FLEEING);
    }

    private void playCuyAction(String name, int durationTicks) {
        if (!this.getWorld().isClient) {
            this.dataTracker.set(ACTION, name);
            this.dataTracker.set(ACTION_SERIAL, this.dataTracker.get(ACTION_SERIAL) + 1);
            this.actionTicks = durationTicks;
            this.walkingToGarden = false;
            this.getNavigation().stop();
            this.autoCooldown = 100 + this.random.nextInt(100);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            this.gateController.tick();
            this.cuyTrades.tick();
            this.cuyFarm.tick();
            if (this.hurtTime > 0) {
                this.fleeUntilTick = this.age + 80;
            }
            if (this.age % 10 == 0) {
                this.hostileNearby = !this.getWorld()
                        .getEntitiesByClass(HostileEntity.class, this.getBoundingBox().expand(9.0), HostileEntity::isAlive)
                        .isEmpty();
            }
            boolean fleeing = this.age < this.fleeUntilTick || this.hostileNearby;
            if (this.dataTracker.get(FLEEING) != fleeing) {
                this.dataTracker.set(FLEEING, fleeing);
            }
            if (fleeing) {
                if (this.actionTicks > 0) {
                    this.actionTicks = 0;
                    this.dataTracker.set(ACTION, "");
                }
                this.walkingToGarden = false;
                this.autoCooldown = 120;
            } else if (this.actionTicks > 0) {
                this.actionTicks--;
                if (this.actionTicks == 0) {
                    this.dataTracker.set(ACTION, "");
                }
            } else if (!this.walkingToGarden) {
                if (--this.autoCooldown <= 0) {
                    this.autoCooldown = 100 + this.random.nextInt(140);
                    this.chooseAutonomousAction();
                }
            }
        }
    }

    private void chooseAutonomousAction() {
        if (this.hasNearbyPlayer() && this.random.nextInt(4) == 0) {
            this.playCuyAction("wave_hello", 52);
        } else {
            switch (this.random.nextInt(4)) {
                case 0 -> this.playCuyAction("happy", 29);
                case 1 -> this.playCuyAction("basket_check", 62);
                case 2 -> this.playCuyAction("sit_rest", 74);
                default -> this.playCuyAction("garden_proud", 56);
            }
        }
    }

    private boolean hasNearbyPlayer() {
        for (PlayerEntity player : this.getWorld().getPlayers()) {
            if (!player.isSpectator() && this.squaredDistanceTo(player) < 36.0) {
                return true;
            }
        }
        return false;
    }

    public void setCustomer(PlayerEntity customer) { this.cuyCustomer = customer; }
    public PlayerEntity getCustomer() { return this.cuyCustomer; }
    public TradeOfferList getOffers() { return this.cuyTrades.getOffers(); }
    public void setOffersFromServer(TradeOfferList offers) {}
    public void trade(TradeOffer offer) { this.cuyTrades.trade(offer); }
    public void onSellingItem(ItemStack stack) {}
    public int getExperience() { return this.cuyTrades.getExperience(); }
    public void setExperienceFromServer(int experience) { this.cuyTrades.setExperience(experience); }
    public boolean isLeveledMerchant() { return true; }
    public SoundEvent getYesSound() { return SoundEvents.ENTITY_VILLAGER_YES; }
    public boolean isClient() { return this.getWorld().isClient; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.cuyTrades.write(nbt);
        this.cuyFarm.write(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.cuyTrades.read(nbt);
        this.cuyFarm.read(nbt);
    }

    @Override
    public void onDeath(DamageSource source) {
        if (!this.getWorld().isClient) {
            for (int i = 0; i < this.cuyFarm.getInventory().size(); i++) {
                ItemStack loot = this.cuyFarm.getInventory().removeStack(i);
                if (!loot.isEmpty()) this.dropStack(loot);
            }
        }
        super.onDeath(source);
    }

    CuyFarmController getCuyFarm() { return this.cuyFarm; }

    private void openCuyBasket(PlayerEntity player) {
        if (!this.getWorld().isClient) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, user) -> GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this.cuyFarm.getInventory()),
                    Text.literal("Canasta del Cuy Jardinero")));
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.isSneaking() && stack.isOf(Items.COMPOSTER)) {
            if (!this.getWorld().isClient) player.sendMessage(Text.literal(this.cuyFarm.toggleNearbyStation()), true);
            return ActionResult.SUCCESS;
        } else if (player.isSneaking() && stack.isOf(Items.CHEST)) {
            if (!this.getWorld().isClient) this.openCuyBasket(player);
            return ActionResult.SUCCESS;
        } else if (!stack.isOf(Items.WHEAT) && !stack.isOf(Items.CARROT)) {
            if ((!stack.isEmpty() || player.isSneaking())
                    && !stack.isOf(Items.EMERALD) && !stack.isOf(Items.POTATO)
                    && !stack.isOf(Items.BEETROOT_SEEDS) && !stack.isOf(Items.PUMPKIN_SEEDS)) {
                String action = null, message = null;
                int duration = 0;
                if (stack.isEmpty()) {
                    action = "wave_hello"; duration = 52; message = "El Cuy Jardinero te saluda.";
                } else if (stack.isOf(Items.WATER_BUCKET)) {
                    action = "water_plants"; duration = 90; message = "El Cuy simula regar las plantas.";
                } else if (stack.isOf(Items.DANDELION) || stack.isOf(Items.POPPY) || stack.isOf(Items.CORNFLOWER)
                        || stack.isOf(Items.OXEYE_DAISY) || stack.isOf(Items.ALLIUM)) {
                    action = "sniff_flower"; duration = 70; message = "El Cuy huele la flor.";
                } else if (stack.isOf(Items.WHEAT_SEEDS) || stack.isOf(Items.BEETROOT_SEEDS)
                        || stack.isOf(Items.PUMPKIN_SEEDS) || stack.isOf(Items.MELON_SEEDS)) {
                    action = "inspect_crop"; duration = 60; message = "El Cuy inspecciona los cultivos.";
                }
                if (action == null) {
                    return super.interactMob(player, hand);
                }
                if (!this.getWorld().isClient) {
                    this.playCuyAction(action, duration);
                    player.sendMessage(Text.literal(message), true);
                }
                return ActionResult.SUCCESS;
            } else {
                if (!this.getWorld().isClient) {
                    if (this.getCustomer() != null && this.getCustomer() != player) {
                        player.sendMessage(Text.literal("El Cuy esta comerciando con otro jugador."), true);
                    } else {
                        this.setCustomer(player);
                        this.getNavigation().stop();
                        this.sendOffers(player, Text.literal("Agricultor: " + this.cuyTrades.getRank()), this.cuyTrades.getLevel());
                    }
                }
                return ActionResult.SUCCESS;
            }
        } else {
            return super.interactMob(player, hand);
        }
    }
}
