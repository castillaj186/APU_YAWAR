package pe.apuyawar.systems.persistence;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.util.WorldSavePath;

/** Escribe el valor anterior ANTES de cambiarlo. Recupera también tras un cierre abrupto. */
public final class WorldRuleJournal {
    private final MinecraftServer server;private final Path file;
    private final Map<String,String> previous=new LinkedHashMap<>();
    public WorldRuleJournal(MinecraftServer server){this.server=server;file=server.getSavePath(WorldSavePath.ROOT).resolve("apuyawar-rule-recovery.json");}
    public void change(String rule,String value){
        if(!previous.containsKey(rule)){
            if(rule.equals("difficulty"))previous.put(rule,server.getSaveProperties().getDifficulty().getName());
            else {String old=server.getGameRules().toNbt().getString(rule);if(old.isEmpty())throw new IllegalArgumentException("Regla inexistente: "+rule);previous.put(rule,old);}
            save();
        }command(rule,value);
    }
    private void command(String key,String value){server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),key.equals("difficulty")?"difficulty "+value:"gamerule "+key+" "+value);}
    private void save(){try{Path temp=file.resolveSibling(file.getFileName()+".tmp");Files.writeString(temp,new Gson().toJson(previous));try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){throw new IllegalStateException("No se pudo guardar recuperación de reglas",e);}}
    public void recover(){
        if(!Files.exists(file))return;
        try{var object=JsonParser.parseString(Files.readString(file)).getAsJsonObject();object.entrySet().forEach(e->previous.put(e.getKey(),e.getValue().getAsString()));restore();}
        catch(IOException|RuntimeException e){throw new IllegalStateException("No se pudo recuperar "+file,e);}
    }
    public void restore(){previous.forEach(this::command);previous.clear();try{Files.deleteIfExists(file);}catch(IOException e){throw new IllegalStateException(e);}}
}
