
scoreboard players set #evento safio.estado 0
scoreboard players set #timer safio.timer 0

weather clear

title @a title {"text":"FIN DE REVIIL","color":"green","bold":true}

tellraw @a {"text":"[SAFIO] El Momento REviil ha terminado.","color":"green"}

execute as @a at @s run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 1 1
