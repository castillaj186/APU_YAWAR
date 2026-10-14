function rojo:dest_tags
effect give @a[tag=rj_in] minecraft:resistance 2 255 true
tag @a[tag=rj_player,tag=!rj_in] remove rj_noti
function rojo:prog
execute if entity @a[tag=rj_ok] unless entity @a[tag=rj_player,tag=!rj_ok,gamemode=!spectator] run function rojo:exito
