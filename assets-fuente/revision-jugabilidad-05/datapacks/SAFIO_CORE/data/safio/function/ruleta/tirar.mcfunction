# Los retos REVIIL importados bloquean nuevos giros hasta terminar o cancelar.
execute if score #activo rj_k matches 1 run tellraw @a {"text":"[SAFIO] REVIIL importado activo: espera o cancela.","color":"red"}
execute if score #cuenta_on rj_k matches 1 run tellraw @a {"text":"[SAFIO] REVIIL en cuenta regresiva: espera o cancela.","color":"red"}
execute if score #type safio.live matches 1.. run tellraw @a {"text":"[SAFIO] Un desafio esta activo; espera que termine.","color":"red"}
# Impide iniciar otra ruleta durante un giro o un evento real/de ensayo.
execute if score #animando safio.anim matches 1 run tellraw @a {"text":"[SAFIO] La ruleta ya esta girando.","color":"red"}
execute unless score #animando safio.anim matches 1 if score #evento safio.estado matches 1.. run tellraw @a {"text":"[SAFIO] Espera a que termine el evento activo.","color":"red"}
execute unless score #animando safio.anim matches 1 unless score #evento safio.estado matches 1.. unless score #type safio.live matches 1.. unless score #activo rj_k matches 1 unless score #cuenta_on rj_k matches 1 run function safio:ruleta/iniciar
