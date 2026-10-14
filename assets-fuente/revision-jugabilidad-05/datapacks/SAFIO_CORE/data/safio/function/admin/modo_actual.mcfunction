# Cambiar recursos solamente cuando no esta girando la ruleta.
execute if score #animando safio.anim matches 1 run tellraw @a {"text":"[SAFIO] Espera a que el giro termine para cambiar la interfaz.","color":"red"}
execute unless score #animando safio.anim matches 1 run scoreboard players set #visual safio.config 0
execute unless score #animando safio.anim matches 1 run tellraw @a {"text":"[SAFIO] Modo visual ACTUAL activado.","color":"green"}
