package pe.apuyawar.momentocuy.entity;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;

import java.util.Arrays;

/** Adaptado sin cambios de comportamiento del {@code CuyTradeCatalog} del mod
 * antiguo (ver {@code CuyJardineroEntity} para el proceso de recuperación):
 * ofertas de comercio reales por nivel de experiencia, con reposición diaria
 * si hay un compostador cerca. Claves NBT renombradas de
 * {@code SafioCuyTrader*} a {@code MomentoCuyTrader*}. */
public final class CuyTradeCatalog {
    private static final Oferta[] CATALOG = {
            new Oferta(1, Items.POTATO, 26, Items.EMERALD, 1, 16, 2),
            new Oferta(1, Items.WHEAT, 20, Items.EMERALD, 1, 16, 2),
            new Oferta(2, Items.PUMPKIN, 6, Items.EMERALD, 1, 12, 5),
            new Oferta(2, Items.EMERALD, 1, Items.PUMPKIN_PIE, 4, 12, 5),
            new Oferta(3, Items.CARROT, 22, Items.EMERALD, 1, 16, 10),
            new Oferta(3, Items.EMERALD, 1, Items.APPLE, 6, 16, 10),
            new Oferta(4, Items.MELON, 4, Items.EMERALD, 1, 12, 15),
            new Oferta(4, Items.EMERALD, 3, Items.COOKIE, 18, 12, 15),
            new Oferta(5, Items.EMERALD, 1, Items.GOLDEN_CARROT, 3, 12, 20),
            new Oferta(5, Items.EMERALD, 2, Items.GLISTERING_MELON_SLICE, 3, 12, 20),
    };
    private final CuyJardineroEntity cuy;
    private final int[] uses = new int[CATALOG.length];
    private int experience = 0;
    private long lastRestockDay = -1L;
    private TradeOfferList offers;
    private int builtLevel = 0;

    public CuyTradeCatalog(CuyJardineroEntity cuy) { this.cuy = cuy; }

    public int getExperience() { return this.experience; }

    public int getLevel() {
        if (this.experience >= 250) return 5;
        if (this.experience >= 150) return 4;
        if (this.experience >= 70) return 3;
        return this.experience >= 10 ? 2 : 1;
    }

    public String getRank() {
        return switch (this.getLevel()) {
            case 2 -> "Aprendiz";
            case 3 -> "Cualificado";
            case 4 -> "Experto";
            case 5 -> "Maestro";
            default -> "Novato";
        };
    }

    public void setExperience(int newExperience) {
        int oldLevel = this.getLevel();
        this.experience = Math.max(0, newExperience);
        if (oldLevel != this.getLevel()) {
            this.offers = null;
            this.builtLevel = 0;
        }
    }

    public TradeOfferList getOffers() {
        int level = this.getLevel();
        if (this.offers != null && this.builtLevel == level) return this.offers;
        TradeOfferList result = new TradeOfferList();
        for (int i = 0; i < CATALOG.length; i++) {
            Oferta def = CATALOG[i];
            if (def.level() <= level) {
                TradeOffer offer = new TradeOffer(new TradedItem(def.buy(), def.buyAmount()),
                        new ItemStack(def.sell(), def.sellAmount()), def.maxUses(), def.xp(), 0.05F);
                int count = Math.min(Math.max(0, this.uses[i]), def.maxUses());
                for (int j = 0; j < count; j++) offer.use();
                result.add(offer);
            }
        }
        this.offers = result;
        this.builtLevel = level;
        return this.offers;
    }

    public void trade(TradeOffer offer) {
        TradeOfferList list = this.getOffers();
        int index = list.indexOf(offer);
        offer.use();
        if (index >= 0 && index < this.uses.length) this.uses[index] = offer.getUses();
    }

    public void tick() {
        if (!this.cuy.getWorld().isClient && this.cuy.age % 200 == 0 && this.cuy.getCustomer() == null) {
            long day = Math.floorDiv(this.cuy.getWorld().getTimeOfDay(), 24000L);
            if (day != this.lastRestockDay) {
                BlockPos center = this.cuy.getBlockPos();
                boolean workstation = false;
                for (int x = -4; x <= 4 && !workstation; x++) {
                    for (int z = -4; z <= 4 && !workstation; z++) {
                        for (int y = -1; y <= 1; y++) {
                            if (this.cuy.getWorld().getBlockState(center.add(x, y, z)).isOf(Blocks.COMPOSTER)) {
                                workstation = true;
                                break;
                            }
                        }
                    }
                }
                if (workstation) {
                    Arrays.fill(this.uses, 0);
                    if (this.offers != null) for (TradeOffer offer : this.offers) offer.resetUses();
                    this.lastRestockDay = day;
                }
            }
        }
    }

    public void write(NbtCompound nbt) {
        nbt.putInt("MomentoCuyTraderXp", this.experience);
        nbt.putIntArray("MomentoCuyTraderUses", this.uses);
        nbt.putLong("MomentoCuyTraderRestockDay", this.lastRestockDay);
    }

    public void read(NbtCompound nbt) {
        this.experience = Math.max(0, nbt.getInt("MomentoCuyTraderXp"));
        Arrays.fill(this.uses, 0);
        int[] saved = nbt.getIntArray("MomentoCuyTraderUses");
        System.arraycopy(saved, 0, this.uses, 0, Math.min(saved.length, this.uses.length));
        this.lastRestockDay = nbt.contains("MomentoCuyTraderRestockDay") ? nbt.getLong("MomentoCuyTraderRestockDay") : -1L;
        this.offers = null;
        this.builtLevel = 0;
    }

    private record Oferta(int level, ItemConvertible buy, int buyAmount, ItemConvertible sell, int sellAmount, int maxUses, int xp) {}
}
