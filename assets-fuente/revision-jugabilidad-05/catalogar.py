from pathlib import Path
import re,json,collections
folder=Path(__file__).resolve().parent
data=folder/'datapacks/SAFIO_CORE/data'
red=[]
for i in range(1,117):
    text=(data/f'rojo/function/reto/{i}.mcfunction').read_text(encoding='utf-8-sig')
    params={k:int(v) for k,v in re.findall(r'scoreboard players set #(\w+) rj_k (-?\d+)',text)}
    name=re.search(r'nombre set value "([^"]+)"',text).group(1)
    line=next(x for x in text.splitlines() if x.startswith('tellraw'))
    message=json.loads(line.split(' ',2)[2])
    desc=' '.join(x.get('text','') for x in message if isinstance(x,dict))[len(message[1]['text']):].strip()
    red.append(dict(id=f'juicio_{i:03}',name=name,description=desc,parameters=params))
orange=[]
apply=(data/'naranja/function/aplicar.mcfunction').read_text(encoding='utf-8-sig')
for i in range(1,101):
    text=(data/f'naranja/function/cambio_{i}.mcfunction').read_text(encoding='utf-8-sig')
    descriptions=[]
    for line in text.splitlines():
        if line.startswith('tellraw'): descriptions += [x.get('text','') for x in json.loads(line.split(' ',2)[2])]
    rules=[x for x in apply.splitlines() if re.search(fr'#c{i} naranja\b',x)]
    orange.append(dict(id=f'adversidad_{i:03}',description=' '.join(descriptions[1:]),rules=rules))
(folder/'catalogos-historicos.json').write_text(json.dumps({'red':red,'orange':orange},ensure_ascii=False,indent=2),encoding='utf-8')
print('Red kinds:',collections.Counter(x['parameters']['kind'] for x in red))
for x in orange: print(x['id'],x['description'][:120],len(x['rules']))
