package pe.apuyawar.systems.challenges;

import pe.apuyawar.systems.events.EventCatalog;

/** Evaluación verificable independiente del render y de efectos secundarios. */
public final class ChallengeRules {
    public record PlayerFacts(double y,String dimension,boolean roof,boolean shield,boolean bow,
                              boolean apple,boolean torch,boolean inDestination,boolean eligible) {}
    private ChallengeRules(){}
    public static boolean passed(EventCatalog.Challenge c,PlayerFacts p,boolean expired){
        if(!p.eligible())return false;
        if(!c.id().startsWith("juicio_")&&!p.dimension().equals("minecraft:overworld"))return false;
        return switch(c.goal()){
            case "height","gas"->p.y()>=c.target();case "depth"->p.y()<=c.target();
            case "roof"->p.roof();case "roof_shield"->p.roof()&&p.shield();case "shield"->p.shield();
            case "bow"->p.bow();case "apple"->p.apple();case "torch_depth"->p.y()<=c.target()&&p.torch();
            case "nether"->p.dimension().equals("minecraft:the_nether");case "survive"->expired;
            case "destination"->p.inDestination();default->throw new IllegalArgumentException("Objetivo desconocido: "+c.goal());
        };
    }
}
