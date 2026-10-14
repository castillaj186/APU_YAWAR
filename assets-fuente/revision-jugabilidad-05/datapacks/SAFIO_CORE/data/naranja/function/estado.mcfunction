function naranja:count_remaining
tellraw @s [{"text":"● NARANJA ","color":"gold","bold":true},{"text":"Cambios pendientes: ","color":"#FFD588"},{"score":{"name":"#remaining","objective":"naranja"},"color":"gold","bold":true},{"text":" / 100","color":"gray"}]
tellraw @s [{"text":"  Loop ticks: ","color":"gray"},{"score":{"name":"#tick_count","objective":"naranja"},"color":"green"}]
