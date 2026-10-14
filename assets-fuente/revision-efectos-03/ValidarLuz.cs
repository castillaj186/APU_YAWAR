using System;using System.Drawing;using System.Drawing.Imaging;using System.IO;
public static class ValidarLuz {
 public static string Run(string resources) {
  foreach(string name in new[]{"light_soft","light_sector","gem_light","ring_light"})using(var b=new Bitmap(Path.Combine(resources,name+".png"))) {
   if(b.Width!=512||b.Height!=512||b.PixelFormat!=PixelFormat.Format32bppArgb)throw new Exception("Formato de máscara incorrecto");
   int visible=0;
   for(int y=0;y<512;y++)for(int x=0;x<512;x++) {
    Color p=b.GetPixel(x,y);if(p.R!=255||p.G!=255||p.B!=255||p.A>220)throw new Exception("Máscara no neutra o sólida");if(p.A==0)continue;visible++;
    if(name=="gem_light") {
     double dx=x+.5-52,dy=y+.5-256;if(dx*dx+dy*dy>=169)throw new Exception("Luz fuera del cristal");
     if(x>=99)throw new Exception("Luz sobre punta");
    }
    if(name=="light_sector") {
     double dx=x+.5-256,dy=y+.5-256,r=Math.Sqrt(dx*dx+dy*dy),a=Math.Atan2(dy,dx)*180/Math.PI;
     if(r<22||r>169||a< -90||a> -45)throw new Exception("Flash invade otra casilla");
    }
   }
   if(visible==0)throw new Exception("Máscara vacía");
   if(name=="ring_light"&&b.GetPixel(256,256).A>0)throw new Exception("Máscara del aro rellena el centro");
   if(b.GetPixel(0,0).A!=0)throw new Exception("Fondo opaco");
  }
  return "OK: 4 máscaras RGBA, blancas, transparentes y sin alfa sólido; gema limitada al cristal sin tocar oro/punta; flash limitado a 45 grados; aro sin centro relleno.";
 }
}
