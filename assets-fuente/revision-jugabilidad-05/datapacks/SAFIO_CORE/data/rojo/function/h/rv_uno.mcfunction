execute store result score #c rj_k if entity @e[type=minecraft:ravager,tag=rj_mob,distance=..60]
execute if score #c rj_k matches 1.. run return 0
summon minecraft:ravager ~ ~ ~ {Tags:["rj_mob","reto"],PersistenceRequired:1b,DeathLootTable:"minecraft:empty"}
