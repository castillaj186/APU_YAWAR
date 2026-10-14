# /function rojo:prueba_meteoro -> deja caer UN meteorito grande cerca de ti, sin iniciar un reto.
scoreboard objectives add rj_k dummy
execute at @s run function rojo:h/mt_cae
tellraw @s ["",{"text":"[ROJO] ","color":"red","bold":true},{"text":"Meteorito GRANDE lanzado: explosion ampliada.","color":"gray"}]
