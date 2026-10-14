package pe.apuyawar.systems.events;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Datos propios, sin clases ni funciones del proyecto histórico en ejecución. */
public final class EventCatalog {
    public record Challenge(String id,String name,String description,String goal,int target,int duration,
                            String scope,boolean atDeadline,Map<String,Integer> parameters) {
        public Challenge {parameters=Map.copyOf(parameters);if(duration<=0||!Set.of("height","depth","roof","roof_shield","shield","bow","apple","torch_depth","destination","survive","gas","nether").contains(goal)||!Set.of("individual","global").contains(scope))throw new IllegalArgumentException("Reto inválido: "+id);}
        public int parameter(String key){return parameters.getOrDefault(key,0);}
    }
    public record Action(String kind,String entity,String attribute,double value,String effect,int duration,int amplifier,String condition) {}
    public record Difficulty(String id,String name,String description,int legacy,List<Action> actions) {
        public Difficulty {actions=List.copyOf(actions);if(!((legacy>=1&&legacy<=35)||(legacy>=1001&&legacy<=1100)))throw new IllegalArgumentException("Dificultad inválida: "+id);}
    }
    public static final Map<String,Challenge> CHALLENGES=load("challenges",Challenge[].class);
    public static final Map<String,Difficulty> DIFFICULTIES=load("difficulties",Difficulty[].class);
    private EventCatalog(){}
    private static <T> Map<String,T> load(String file,Class<T[]> type){
        try(var stream=EventCatalog.class.getResourceAsStream("/data/apuyawar_systems/catalogs/"+file+".json")){
            if(stream==null)throw new IOException("Catálogo ausente: "+file);
            T[] entries=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),type);
            var result=new LinkedHashMap<String,T>();
            for(T entry:entries){String id=entry instanceof Challenge c?c.id():((Difficulty)entry).id();
                if(result.put(id,entry)!=null)throw new IOException("ID duplicado: "+id);}
            return Collections.unmodifiableMap(result);
        }catch(IOException ex){throw new IllegalStateException(ex);}
    }
}
