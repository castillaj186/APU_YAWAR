scoreboard players operation #m rj_k = #e rj_k
scoreboard players operation #m rj_k %= #gh rj_k
execute if score #m rj_k matches 0 as @a[tag=rj_player,tag=!rj_in,gamemode=!spectator] at @s run function rojo:h/gh_uno
