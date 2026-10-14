package pe.apuyawar.systems.missions;

/** Definición estática de una misión real del servidor. */
public record MissionDefinition(String id, String name, String description, int required, int rewardSolPe) {}
