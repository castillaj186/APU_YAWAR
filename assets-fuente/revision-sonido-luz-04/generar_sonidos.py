"""Síntesis original determinista. No lee audio de referencia ni usa muestras externas."""
from pathlib import Path
import math, random, struct, wave, subprocess, json

ROOT = Path(__file__).resolve().parents[2]
STAGE = Path(__file__).resolve().parent
DEST = ROOT / 'apuyawar-systems/src/main/resources/assets/apuyawar_systems'
RATE = 44100
SPECS = {'appear':(.30,180),'tick':(.09,1460),'result':(.65,420),
         'fill':(.20,920),'finish':(1.05,210),'outro':(.90,740)}

def synth(name, duration, base):
    rng = random.Random(104 + list(SPECS).index(name))
    samples=[]
    for i in range(int(duration*RATE)):
        t=i/RATE; u=t/duration
        attack=min(1,t/.004)
        tail=(1-u)**2
        if name=='outro':
            phase=2*math.pi*base*(t-.32*t*t/duration)
            value=.4*math.sin(phase)+.16*math.sin(phase*2.013)
            env=math.sin(math.pi*u)**1.3*tail
        else:
            value=sum(weight*math.sin(2*math.pi*base*ratio*t)*math.exp(-t*decay)
                for ratio,weight,decay in [(1,.48,4),(2.013,.22,12),(3.71,.12,22)])
            if name=='appear': value+=.45*math.sin(2*math.pi*(90*t-35*t*t))*math.exp(-25*t)
            if name in ('result','finish'):
                # Timbres inarmónicos y ascenso continuo, sin melodía de referencia.
                value+=.20*math.sin(2*math.pi*(base*4*t+200*t*t))*math.exp(-3*t)
            value+=.045*(rng.random()*2-1)*math.exp(-65*t)
            env=attack*tail
        samples.append(value*env)
    peak=max(abs(s) for s in samples)
    return [s*.72/peak for s in samples]

def main():
    output=DEST/'sounds/roulette'; output.mkdir(parents=True,exist_ok=True)
    manifest={}
    for name,(duration,base) in SPECS.items():
        samples=synth(name,duration,base)
        wav=STAGE/f'roulette_{name}.wav'
        with wave.open(str(wav),'wb') as f:
            f.setnchannels(1);f.setsampwidth(2);f.setframerate(RATE)
            f.writeframes(b''.join(struct.pack('<h',round(s*32767)) for s in samples))
        subprocess.run(['ffmpeg','-y','-v','error','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(output/f'roulette_{name}.ogg')],check=True)
        manifest[f'roulette_{name}']={'sounds':[{'name':f'apuyawar_systems:roulette/roulette_{name}','stream':False}]}
    config=DEST/'sounds.json'
    existing=json.loads(config.read_text(encoding='utf-8')) if config.exists() else {}
    existing.update(manifest)
    config.write_text(json.dumps(existing,indent=2)+'\n',encoding='utf-8')
    print('Seis OGG Vorbis mono originales y sounds.json generados.')
if __name__=='__main__': main()
