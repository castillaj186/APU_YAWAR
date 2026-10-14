scoreboard players add @s rj_gas 1
execute if score @s rj_gas matches ..5 run damage @s 1 minecraft:magic
execute if score @s rj_gas matches 6..10 run damage @s 2 minecraft:magic
execute if score @s rj_gas matches 11..15 run damage @s 3 minecraft:magic
execute if score @s rj_gas matches 16.. run damage @s 4 minecraft:magic
playsound minecraft:entity.player.hurt master @s ~ ~ ~ 0.5 0.6
