tag @s add rj_tpme
execute as @r[tag=rj_tw,tag=!rj_tpme] at @s run tp @a[tag=rj_tpme,limit=1] ~ ~ ~
tag @s remove rj_tpme
playsound minecraft:entity.enderman.teleport master @s ~ ~ ~ 1 1
