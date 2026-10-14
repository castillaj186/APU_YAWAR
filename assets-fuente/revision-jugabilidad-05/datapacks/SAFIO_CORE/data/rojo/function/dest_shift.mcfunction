scoreboard players add #dest rj_k 1
execute if score #dest rj_k matches 6.. run scoreboard players set #dest rj_k 1
tellraw @a[tag=rj_player] ["",{"text":"● ROJO ","color":"red","bold":true},{"text":"Todos ya estaban en ese destino, así que se cambió a otro lugar.","color":"yellow"}]
