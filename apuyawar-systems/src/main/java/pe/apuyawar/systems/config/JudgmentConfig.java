package pe.apuyawar.systems.config;

import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Se carga al iniciar el servidor; límites seguros también para archivos editados. */
public final class JudgmentConfig {
    public boolean redEnabled=true,orangeEnabled=true,includeDatapackVariants=true,joinDuringChallenge=false;
    public int challengeSecondsOverride=0,difficultySeconds=1200,maxHazardMobs=64;
    public List<String> disabledChallenges=new ArrayList<>(),disabledDifficulties=new ArrayList<>();
    public Map<String,Refuge> refuges=new HashMap<>();
    public record Refuge(String dimension,double x,double y,double z,double radius) {}
    public static JudgmentConfig load(){
        Path path=FabricLoader.getInstance().getConfigDir().resolve("apuyawar-judgments.json");
        var gson=new GsonBuilder().setPrettyPrinting().create();
        try{
            JudgmentConfig cfg;
            if(Files.exists(path)){try(var reader=Files.newBufferedReader(path)){cfg=gson.fromJson(reader,JudgmentConfig.class);}}
            else {cfg=new JudgmentConfig();Files.createDirectories(path.getParent());try(var writer=Files.newBufferedWriter(path)){gson.toJson(cfg,writer);}}
            if(cfg==null||cfg.disabledChallenges==null||cfg.disabledDifficulties==null||cfg.refuges==null)throw new IOException("Configuración incompleta");
            cfg.difficultySeconds=Math.clamp(cfg.difficultySeconds,1,86400);
            cfg.challengeSecondsOverride=Math.clamp(cfg.challengeSecondsOverride,0,86400);
            cfg.maxHazardMobs=Math.clamp(cfg.maxHazardMobs,0,128);
            return cfg;
        }catch(IOException|RuntimeException ex){throw new IllegalStateException("Revisa "+path+": "+ex.getMessage(),ex);}
    }
}
