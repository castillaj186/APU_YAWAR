"""Mide energía y ataques; los datos de referencia nunca entran en el sintetizador."""
from pathlib import Path
import subprocess, array, math, json
folder=Path(__file__).resolve().parent
results=[]
for video in (folder/'referencias').glob('*.mp4'):
    raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(video),'-vn','-ac','1','-ar','8000','-f','f32le','pipe:1'])
    pcm=array.array('f');pcm.frombytes(raw)
    hop=160
    rms=[math.sqrt(sum(x*x for x in pcm[i:i+hop])/len(pcm[i:i+hop])) for i in range(0,len(pcm),hop)]
    maximum=max(rms); attacks=[];last=-1
    for i in range(1,len(rms)-1):
        t=i*.02
        if rms[i]>maximum*.16 and rms[i]>rms[i-1]*1.7 and t-last>.09:
            attacks.append(round(t,2));last=t
    results.append({'video':video.name,'duration_seconds':len(pcm)/8000,'peak_rms_db':round(20*math.log10(maximum),2),'detected_attack_seconds':attacks,
                    'method':'RMS 20 ms, onset threshold 1.7x, refractory 90 ms; automatic estimates, not perceptual approval'})
(folder/'referencias-audio.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
for result in results: print(result)
