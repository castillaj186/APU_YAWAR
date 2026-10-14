from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[2]
stage=Path(__file__).resolve().parent
out=root/'apuyawar-systems/src/main/resources/data/apuyawar_systems/catalogs'
out.mkdir(parents=True,exist_ok=True)
old=json.loads((stage/'catalogos-historicos.json').read_text(encoding='utf-8'))
weekly=[('ascenso_desesperado','Ascenso desesperado','Alcanza Y=155 o superior','height',155),('abismo','El abismo llama','Desciende hasta Y=0 o inferior','depth',0),('refugio_negro','Refugio negro','Permanece bajo techo sin cielo visible','roof',0),('ultima_cumbre','Última cumbre','Alcanza Y=180 o superior','height',180),('mineros_malditos','Mineros malditos','Desciende hasta Y=-25 o inferior','depth',-25),('muro_viviente','Muro viviente','Equipa un escudo en alguna mano','shield',0),('arqueros_caos','Arqueros del caos','Sujeta un arco en la mano principal','bow',0),('tierra_prohibida','Tierra prohibida','Desciende hasta Y=-35 o inferior','depth',-35),('guardia_noche','Guardia de la noche','Permanece bajo techo con un escudo equipado','roof_shield',0),('hacia_vacio','Hacia el vacío','Desciende hasta Y=-45 o inferior','depth',-45),('ultimo_alimento','Último alimento','Sujeta una manzana dorada','apple',0),('juramento_oscuro','Juramento oscuro','A Y=-35 o inferior, sujeta una antorcha','torch_depth',-35)]
red=[dict(id=i,name=n,description=d,goal=g,target=t,duration=3600,scope='individual',atDeadline=True,parameters={}) for i,n,d,g,t in weekly]
for x in old['red']:
    p=x['parameters'];kind=p['kind'];goal={1:'destination',2:'survive',3:'height',4:'gas',5:'nether',6:'depth'}[kind]
    desc={1:'Todo el grupo debe reunirse en el refugio indicado.',2:'Sobrevivan hasta el final del temporizador.',3:f'Todo el grupo debe alcanzar Y={p.get("alt",0)} o superior.',4:f'Escapen del gas: todo el grupo debe alcanzar Y={p.get("alt",0)} o superior.',5:'Todo el grupo debe llegar a la dimensión Nether.',6:f'Todo el grupo debe descender a Y={p.get("alt",0)} o inferior.'}[kind]
    hazards={'tnt':'bombardeo mágico','mt':'impactos celestes','st':'descargas','cr':'creepers','ce':'creepers','cn':'creepers','zb':'zombis','sk':'esqueletos','sp':'arañas','ph':'phantoms','hu':'husks','wi':'brujas','vi':'vindicadores','hd':'creepers','gh':'ghasts','vx':'vexes','rv':'ravagers'}
    present=[name for key,name in hazards.items() if p.get(key,0)>0]
    if present:desc+=' Peligros: '+', '.join(dict.fromkeys(present))+'.'
    red.append(dict(id=x['id'],name=x['name'],description=desc,goal=goal,target=p.get('alt',0),duration=p['dur'],scope='global',atDeadline=kind==2,parameters=p))
(out/'challenges.json').write_text(json.dumps(red,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
strings=re.findall(r'// String (.*)',(stage/'DifficultyCatalog.javap.txt').read_text(encoding='utf-8-sig'))
names=strings[::2];descs=strings[1::2]
ids=['zombis_feroces','flechas_crueles','creepers_veloces','caida_dolorosa','olfato_depredador','llamas_persistentes','sin_puertas_aire','ahogados_veloces','carrera_agotadora','zombis_incansables','aldeanos_sin_cria','flecha_entumecedora','aranas_cazadoras','heridas_debilitantes','humo_explosivo','saqueadores_atentos','colmillos_toxicos','brujas_agiles','tinieblas_profundas','zombis_blindados','encantamiento_exigente','lepisma_rabioso','esqueleto_nocturno','hambre_momia','aldeano_infectado','fantasmas_fugaces','flecha_glacial','veneno_cavernario','slimes_rapidos','mina_extenuante','creepers_resistentes','saqueadores_veloces','lluvia_hostil','furia_no_muerto','armadura_pesada']
diff=[dict(id=id,name=names[i],description=descs[i],legacy=i+1,actions=[]) for i,id in enumerate(ids)]
for i,x in enumerate(old['orange'],1):
    actions=[]
    for rule in x['rules']:
        a=re.search(r'as @e\[type=([^,\]]+).*? run attribute @s minecraft:generic\.(\w+) base set ([\d.]+)',rule)
        e=re.search(r'as @e\[type=([^,\]]+).*? run effect give @s (\w+) (\d+) (\d+)',rule)
        if a:actions.append(dict(kind='attribute',entity=a[1].replace('#naranja:hostiles','hostile'),attribute=a[2],value=float(a[3])))
        elif e:
            condition='always'
            if 'iron_ore' in rule:condition='iron'
            elif 'farmland' in rule:condition='farmland'
            elif 'water' in rule:condition='water'
            elif 'distance=' in rule:condition='near'
            actions.append(dict(kind='effect',entity=e[1].replace('#naranja:hostiles','hostile'),effect=e[2],duration=int(e[3])*20,amplifier=int(e[4]),condition=condition))
    description=x['description'].split('Este cambio queda activo')[0].strip()
    alternatives={9:'Las arañas cercanas ralentizan al jugador durante 1,5 s; sin colocar bloques.',10:'Las arañas de cueva reciben Fuerza I.',41:'Romper hojas tiene un 15% de probabilidad de invocar un lepisma temporal.',42:'Minar carbón tiene un 15% de probabilidad de invocar un murciélago temporal.',51:'Hostiles cercanos en el Nether provocan Ceguera breve cada 5 s.',52:'Con pechera de netherita, los phantoms cercanos revelan tu posición con Brillo.',64:'Los aldeanos adultos dejan de reproducirse; no se eliminan bebés.',65:'A partir de 20 vacas cercanas se bloquea temporalmente su reproducción.',66:'A partir de 20 cerdos cercanos se bloquea temporalmente su reproducción.',67:'A partir de 20 ovejas cercanas se bloquea temporalmente su reproducción.',68:'A partir de 25 pollos cercanos se bloquea temporalmente su reproducción.',69:'Se bloquea el inicio de la cura con manzana dorada de aldeanos zombi.',79:'Las arañas cercanas ralentizan al jugador durante 1,5 s; sin colocar bloques.',80:'Romper telarañas tiene un 15% de probabilidad de invocar una araña de cueva temporal.',81:'Bajo agua, sostener antorchas provoca Fatiga minera I; se bloquea su colocación sumergida.',87:'Los enemigos no perdonan a los jugadores muertos (forgiveDeadPlayers=false).',88:'No se puede ordeñar mooshrooms con cubos ni cuencos.',89:'No se pueden ordeñar vacas; la leche almacenada se convierte en cubos vacíos.',90:'Los esqueletos detectan jugadores ocho bloques más lejos; sus flechas vanilla ya son ilimitadas.',92:'No se pueden ordeñar vacas; la leche almacenada se convierte en cubos vacíos.'}
    description=alternatives.get(i,description)
    diff.append(dict(id=x['id'],name=f'Adversidad {i:03}',description=description,legacy=1000+i,actions=actions))
(out/'difficulties.json').write_text(json.dumps(diff,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'{len(red)} retos; {len(diff)} dificultades; catálogos propios generados.')
