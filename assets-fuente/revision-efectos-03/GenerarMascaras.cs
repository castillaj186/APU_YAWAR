using System;using System.Drawing;using System.Drawing.Imaging;using System.IO;
public static class GenerarMascaras {
    public static void RunRing(string destination,string ringPath) {
        using(var ring=new Bitmap(ringPath))using(var image=new Bitmap(512,512,PixelFormat.Format32bppArgb)) {
            for(int y=0;y<512;y++)for(int x=0;x<512;x++) {
                Color p=ring.GetPixel(x,y);double lum=(.2126*p.R+.7152*p.G+.0722*p.B)/255;
                int alpha=(int)Math.Round(110*p.A/255.0*Smooth((lum-.35)/.55));
                image.SetPixel(x,y,Color.FromArgb(alpha,255,255,255));
            }
            using(var stream=new FileStream(Path.Combine(destination,"ring_light.png"),FileMode.CreateNew))image.Save(stream,ImageFormat.Png);
        }
    }
    static double Smooth(double t){t=Math.Max(0,Math.Min(1,t));return t*t*(3-2*t);}
    public static void Run(string destination,string pointerPath) {
        Directory.CreateDirectory(destination);
        using(var pointer=new Bitmap(pointerPath))foreach(string name in new[]{"light_soft","light_sector","gem_light"})using(var image=new Bitmap(512,512,PixelFormat.Format32bppArgb)) {
            for(int y=0;y<512;y++)for(int x=0;x<512;x++) {
                double dx=x+.5-256,dy=y+.5-256,r=Math.Sqrt(dx*dx+dy*dy),alpha=0;
                if(name=="light_soft")alpha=210*Math.Exp(-r*r/(2*75*75))*Smooth((220-r)/35);
                if(name=="light_sector") {
                    double a=Math.Atan2(dy,dx)*180/Math.PI;
                    if(r>=22&&r<=169&&a>=-90&&a<=-45) {
                        double edge=Math.Min(r*Math.Sin((a+90)*Math.PI/180),r*Math.Sin((-45-a)*Math.PI/180));
                        alpha=(85*Math.Exp(-Math.Pow((r-157)/7,2))+60*Math.Exp(-edge*edge/10)+12)*Smooth((169-r)/2)*Smooth((r-22)/3);
                    }
                }
                if(name=="gem_light") {
                    double gx=x+.5-52,gy=y+.5-256,gr=Math.Sqrt(gx*gx+gy*gy);
                    if(gr<13) {
                        Color p=pointer.GetPixel(x,y);double lum=(p.R+p.G+p.B)/(3.0*255);
                        alpha=(80+120*lum)*Smooth((13-gr)/3)*p.A/255.0;
                    }
                }
                image.SetPixel(x,y,Color.FromArgb((int)Math.Round(Math.Max(0,Math.Min(220,alpha))),255,255,255));
            }
            using(var stream=new FileStream(Path.Combine(destination,name+".png"),FileMode.CreateNew))image.Save(stream,ImageFormat.Png);
        }
    }
}
