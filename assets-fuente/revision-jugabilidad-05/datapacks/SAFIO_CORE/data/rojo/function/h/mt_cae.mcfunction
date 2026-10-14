# Meteorito con explosion moderada de potencia 4 y silueta visual ampliada mediante particulas.
summon minecraft:fireball ~ ~45 ~ {Motion:[0.0,-1.25,0.0],power:[0.0,-0.2,0.0],ExplosionPower:4b,Tags:["rj_mob","reto"]}
particle minecraft:flame ~ ~43 ~ 1.25 1.25 1.25 0.12 180 force
particle minecraft:lava ~ ~43 ~ 0.9 0.9 0.9 0.06 55 force
particle minecraft:large_smoke ~ ~43 ~ 1.0 1.0 1.0 0.05 70 force
particle minecraft:smoke ~ ~43 ~ 1.1 1.1 1.1 0.04 90 force
playsound minecraft:entity.firework_rocket.launch master @a[distance=..80] ~ ~ ~ 1 0.6
