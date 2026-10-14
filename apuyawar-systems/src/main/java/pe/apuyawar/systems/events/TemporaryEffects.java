package pe.apuyawar.systems.events;

import java.util.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/** No reemplaza pociones ajenas. TTL corto y marcas recuperables al cargar entidades. */
public final class TemporaryEffects {
    private record Key(LivingEntity entity,RegistryEntry<StatusEffect> effect){}
    private record Owned(int amplifier,int duration,int age,StatusEffectInstance previous,int previousAge){}
    private final Map<Key,Owned> owned=new HashMap<>();
    private static final String PREFIX="apuyawar_temp_effect/";
    private static String tag(Key key,Owned value){return PREFIX+Registries.STATUS_EFFECT.getId(key.effect().value()).getPath()+"/"+value.amplifier()+(value.previous()!=null?"/hidden":"");}
    public void prune(){owned.entrySet().removeIf(e->{var key=e.getKey();boolean expired=key.entity().isRemoved()||key.entity().age-e.getValue().age()>=e.getValue().duration();if(expired)key.entity().removeCommandTag(tag(key,e.getValue()));return expired;});}
    public void apply(LivingEntity entity,RegistryEntry<StatusEffect> effect,int duration,int amplifier){
        int ticks=Math.clamp(duration,1,120);Key key=new Key(entity,effect);
        var current=entity.getStatusEffect(effect);var prior=owned.get(key);
        if(current!=null&&prior==null&&current.getAmplifier()>=amplifier)return;
        if(current!=null&&prior!=null&&(current.getDuration()<0||current.getAmplifier()!=prior.amplifier()||current.getDuration()>Math.max(0,prior.duration()-(entity.age-prior.age()))+2))return;
        StatusEffectInstance original=prior!=null?prior.previous():current!=null?new StatusEffectInstance(current):null;
        int originalAge=prior!=null?prior.previousAge():entity.age;
        entity.addStatusEffect(new StatusEffectInstance(effect,ticks,amplifier,false,false,true));
        var value=new Owned(amplifier,ticks,entity.age,original,originalAge);owned.put(key,value);
        entity.addCommandTag(tag(key,value));
    }
    public void clear(){
        for(var entry:owned.entrySet()){
            Key key=entry.getKey();Owned prior=entry.getValue();var effect=key.entity().getStatusEffect(key.effect());
            if(effect!=null&&effect.getDuration()>=0&&effect.getAmplifier()==prior.amplifier()&&effect.getDuration()<=Math.max(0,prior.duration()-(key.entity().age-prior.age()))+2)
                {key.entity().removeStatusEffect(key.effect());
                if(prior.previous()!=null){var old=prior.previous();int left=old.getDuration()<0?-1:old.getDuration()-(key.entity().age-prior.previousAge());if(left>0||left==-1)key.entity().addStatusEffect(new StatusEffectInstance(key.effect(),left,old.getAmplifier(),old.isAmbient(),old.shouldShowParticles(),old.shouldShowIcon()));}}
            key.entity().removeCommandTag(tag(key,prior));
        }owned.clear();
    }
    public static void recover(LivingEntity entity){
        for(String tag:List.copyOf(entity.getCommandTags()))if(tag.startsWith(PREFIX)){
            String[] parts=tag.substring(PREFIX.length()).split("/");
            if(parts.length==2){var effect=Registries.STATUS_EFFECT.getEntry(Identifier.ofVanilla(parts[0]));
                try {int amplifier=Integer.parseInt(parts[1]);effect.ifPresent(e->{var current=entity.getStatusEffect(e);if(current!=null&&current.getDuration()<=120&&current.getAmplifier()==amplifier)entity.removeStatusEffect(e);});}catch(NumberFormatException ignored){}}
            entity.removeCommandTag(tag);
        }
    }
}
