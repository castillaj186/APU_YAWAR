
scoreboard players add #dia safio.dia 1

function safio:actualizar_fase

title @a title {"text":"PROYECTO SAFIO","color":"gold","bold":true}
title @a subtitle {"score":{"name":"#dia","objective":"safio.dia"},"color":"yellow"}

tellraw @a [{"text":"[SAFIO] Comienza el dia ","color":"gold"},{"score":{"name":"#dia","objective":"safio.dia"}},{"text":"."}]

playsound minecraft:ui.toast.challenge_complete master @a
