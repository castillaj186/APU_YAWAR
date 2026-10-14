# ROJO - punto de entrada de todos los retos. Inicia una cuenta regresiva de 10 s
# y SOLO después arranca el reto real (rojo:inicio). La configuración del reto
# (#kind, #dur, #dest, peligros...) ya quedó guardada por la función reto/N.
function rojo:init
execute if score #activo rj_k matches 1 run tellraw @s ["",{"text":"● ROJO ","color":"red","bold":true},{"text":"Ya hay un reto activo.","color":"yellow"}]
execute if score #activo rj_k matches 1 run return 0
execute if score #cuenta_on rj_k matches 1 run return 0
scoreboard players set #cuenta rj_k 10
scoreboard players set #cuenta_on rj_k 1
function rojo:cielo/on
function rojo:cuenta
