
scoreboard players set #evento safio.estado 0
scoreboard players set #timer safio.timer 0

title @a title {"text":"REINICIO PENDIENTE","color":"dark_purple","bold":true}

tellraw @a {"text":"[SAFIO] Tiempo agotado. Un administrador debe reiniciar el servidor.","color":"light_purple"}
