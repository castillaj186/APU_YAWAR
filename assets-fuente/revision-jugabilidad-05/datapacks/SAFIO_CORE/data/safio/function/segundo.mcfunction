
execute if score #evento safio.estado matches 1..2 if score #timer safio.timer matches 1.. run scoreboard players remove #timer safio.timer 1

execute if score #evento safio.estado matches 1 if score #timer safio.timer matches 1.. run title @a actionbar [{"text":"REVIIL | ","color":"red","bold":true},{"score":{"name":"#timer","objective":"safio.timer"}},{"text":" segundos","color":"white"}]

execute if score #evento safio.estado matches 2 if score #timer safio.timer matches 1.. run title @a actionbar [{"text":"REINICIO | ","color":"dark_purple","bold":true},{"score":{"name":"#timer","objective":"safio.timer"}},{"text":" segundos","color":"white"}]

execute if score #evento safio.estado matches 1 if score #timer safio.timer matches ..0 run function safio:ruleta/fin_reviil

execute if score #evento safio.estado matches 2 if score #timer safio.timer matches ..0 run function safio:ruleta/fin_reinicio
