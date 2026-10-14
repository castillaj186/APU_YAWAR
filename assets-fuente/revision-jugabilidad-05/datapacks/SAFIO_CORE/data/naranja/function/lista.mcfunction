tellraw @s [{"text":"● NARANJA cambios ACTIVOS:","color":"gold","bold":true}]
execute if score #c1 naranja matches 1 run tellraw @s {"text":"  ✓ 1. Creepers cargados","color":"green"}
execute if score #c2 naranja matches 1 run tellraw @s {"text":"  ✓ 2. Creepers veloces","color":"green"}
execute if score #c3 naranja matches 1 run tellraw @s {"text":"  ✓ 3. Zombies reforzados","color":"green"}
execute if score #c4 naranja matches 1 run tellraw @s {"text":"  ✓ 4. Zombies brutales","color":"green"}
execute if score #c5 naranja matches 1 run tellraw @s {"text":"  ✓ 5. Esqueletos reforzados","color":"green"}
execute if score #c6 naranja matches 1 run tellraw @s {"text":"  ✓ 6. Esqueletos ágiles","color":"green"}
execute if score #c7 naranja matches 1 run tellraw @s {"text":"  ✓ 7. Arañas reforzadas","color":"green"}
execute if score #c8 naranja matches 1 run tellraw @s {"text":"  ✓ 8. Arañas veloces","color":"green"}
execute if score #c9 naranja matches 1 run tellraw @s {"text":"  ✓ 9. Arañas tejedoras","color":"green"}
execute if score #c10 naranja matches 1 run tellraw @s {"text":"  ✓ 10. Veneno de cueva","color":"green"}
execute if score #c11 naranja matches 1 run tellraw @s {"text":"  ✓ 11. Phantoms reforzados","color":"green"}
execute if score #c12 naranja matches 1 run tellraw @s {"text":"  ✓ 12. Toque espectral","color":"green"}
execute if score #c13 naranja matches 1 run tellraw @s {"text":"  ✓ 13. Endermans reforzados","color":"green"}
execute if score #c14 naranja matches 1 run tellraw @s {"text":"  ✓ 14. Blazes reforzados","color":"green"}
execute if score #c15 naranja matches 1 run tellraw @s {"text":"  ✓ 15. Blazes ágiles","color":"green"}
tellraw @s {"text":"  (usa /function naranja:estado para el total)","color":"dark_gray"}
