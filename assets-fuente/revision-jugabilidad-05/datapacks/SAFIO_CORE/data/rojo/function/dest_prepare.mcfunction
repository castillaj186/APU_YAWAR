# Si absolutamente TODOS los participantes ya están dentro, cambia el destino una vez.
# En modo prueba (#noshift = 1) el destino NO se cambia, aunque estés dentro.
function rojo:dest_tags
execute unless score #noshift rj_k matches 1 unless entity @a[tag=rj_player,gamemode=!spectator,tag=!rj_in] run function rojo:dest_shift
function rojo:dest_tags
