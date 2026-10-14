using System;using System.IO;using System.Drawing;using System.Drawing.Imaging;using System.Drawing.Drawing2D;using System.Globalization;using System.Runtime.InteropServices;
public static class SimularEfectos {
    static readonly string[] Names={"rojo","rosa","morado","azul","turquesa","verde","amarillo","naranja"};
    static readonly int[] Palette={0xE83233,0xF052AF,0x8139CE,0x2780E4,0x22C6CE,0x37B752,0xFFD235,0xEF8C27};
    static Bitmap[] textures;
    static void Draw(Graphics g,Bitmap b,int rgb,double alpha,double scale,double angle) {
        if(alpha<=0)return;
        var state=g.Save();g.RotateTransform((float)angle);g.ScaleTransform((float)scale,(float)scale);
        using(var attrs=new ImageAttributes()){var m=new ColorMatrix();m.Matrix00=((rgb>>16)&255)/255f;m.Matrix11=((rgb>>8)&255)/255f;m.Matrix22=(rgb&255)/255f;m.Matrix33=(float)Math.Min(1,alpha);attrs.SetColorMatrix(m);g.DrawImage(b,new Rectangle(-256,-256,512,512),0,0,512,512,GraphicsUnit.Pixel,attrs);}g.Restore(state);
    }
    static void Disk(Graphics g,double[] row,double rotation,double alpha) {
        int winner=(int)row[1];for(int slot=0;slot<8;slot++) {
            double build=row[14+slot],fill=row[22+slot],scale=.86+.14*build;
            if(fill<1||slot==winner)Draw(g,textures[slot],0xFFFFFF,alpha*build,scale,rotation+slot*45);
            if(fill>0&&slot!=winner)Draw(g,textures[winner],0xFFFFFF,alpha*build*fill,scale,rotation+slot*45);
        }
        Draw(g,textures[8],0xFFFFFF,alpha*row[5],1,rotation);Draw(g,textures[9],0xFFFFFF,alpha*row[5],1,rotation);
    }
    static double Pulse(double t,double start,double duration){double u=(t-start)/duration;return u<=0||u>=1?0:Math.Pow(Math.Sin(Math.PI*u),2);}
    static double Ring(double r,double radius,double width){return Math.Max(0,1-Math.Abs(r-radius)/width);}
    static void Add(ref double r,ref double g,ref double b,int rgb,double a){r+=((rgb>>16)&255)*a;g+=((rgb>>8)&255)*a;b+=(rgb&255)*a;}
    static void Expand(ref double r,ref double g,ref double b,double rad,double tick,double start,double duration,int color,double opacity){double u=(tick-start)/duration;if(u<=0||u>=1)return;double a=Ring(rad,228+55*u,3.5*(1-u)+.8)*opacity*Math.Sin(Math.PI*u)*(1-u);Add(ref r,ref g,ref b,color,a);}
    public static Bitmap Frame(double[] row) {
        double tick=row[0],opacity=row[4],scale=row[3],rotation=row[2],energy=row[7],flare=Math.Max(row[8],row[9]);int winner=(int)row[1],color=Palette[winner];
        var image=new Bitmap(640,640,PixelFormat.Format32bppArgb);using(var g=Graphics.FromImage(image)) {
            g.Clear(Color.FromArgb(29,37,46));g.InterpolationMode=InterpolationMode.HighQualityBicubic;
            g.TranslateTransform(320,320);g.ScaleTransform((float)scale,(float)scale);
            var disk=g.Save();g.TranslateTransform((float)(row[12]*(3.2*Math.Sin(tick*1.8)+.9*Math.Sin(tick*4.1))),(float)(row[12]*(2.4*Math.Cos(tick*2.3)+.6*Math.Sin(tick*3.6))));
            if(energy>.4){Disk(g,row,rotation+row[13]-54*energy*.28,opacity*.045*energy);Disk(g,row,rotation+row[13]-54*energy*.14,opacity*.065*energy);}
            Disk(g,row,rotation+row[13],opacity);g.Restore(disk);
            Draw(g,textures[10],0xFFFFFF,opacity*row[6],1,0);
            Draw(g,textures[12],(int)row[10],opacity*row[6]*row[46],1,0);
        }
        using(var lights=new Bitmap(640,640,PixelFormat.Format32bppArgb)) {
            using(var g=Graphics.FromImage(lights)) {
                g.InterpolationMode=InterpolationMode.HighQualityBicubic;g.TranslateTransform(320,320);g.ScaleTransform((float)scale,(float)scale);
                var disk=g.Save();g.TranslateTransform((float)(row[12]*(3.2*Math.Sin(tick*1.8)+.9*Math.Sin(tick*4.1))),(float)(row[12]*(2.4*Math.Cos(tick*2.3)+.6*Math.Sin(tick*3.6))));
                for(int slot=0;slot<8;slot++){double strength=.45*row[30+slot]+.42*row[38+slot]+.30*row[8];int rgb=tick<216?Palette[slot]:color;Draw(g,textures[11],rgb,strength*opacity*row[14+slot],.86+.14*row[14+slot],rotation+row[13]+slot*45);}
                Draw(g,textures[14],color,opacity*row[5]*(.25*row[8]+.12*row[9]),1,rotation+row[13]);g.Restore(disk);
                Draw(g,textures[12],(int)row[10],opacity*row[6]*row[11]*.35,1,0);
                Draw(g,textures[13],0xFFFFFF,Pulse(tick,256,6)*.60*opacity,.07,0);
            }
            var rect=new Rectangle(0,0,640,640);var pixels=image.LockBits(rect,ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);var light=lights.LockBits(rect,ImageLockMode.ReadOnly,PixelFormat.Format32bppArgb);
            try {
                var dest=new byte[640*640*4];var emit=new byte[dest.Length];Marshal.Copy(pixels.Scan0,dest,0,dest.Length);Marshal.Copy(light.Scan0,emit,0,emit.Length);
                for(int y=0;y<640;y++)for(int x=0;x<640;x++) {
                    int index=(y*640+x)*4;double red=emit[index+2]*emit[index+3]/255.0,green=emit[index+1]*emit[index+3]/255.0,blue=emit[index]*emit[index+3]/255.0;
                    if(opacity>0&&scale>0) {
                        double dx=(x+.5-320)/scale,dy=(y+.5-320)/scale,rad=Math.Sqrt(dx*dx+dy*dy);
                        if(rad>221&&rad<285) {
                            int ambient=tick<216?0xFFD38B:color;
                            Add(ref red,ref green,ref blue,ambient,Ring(rad,225,3)*.035*opacity*row[5]);
                            double angle=Math.Atan2(dy,dx)*180/Math.PI;
                            for(int i=0;i<8;i++){double phase=((angle-rotation-i*45)%360+360)%360;if(phase<22)Add(ref red,ref green,ref blue,Palette[i],Ring(rad,233+i%2*3,1.8)*Math.Sin(Math.PI*phase/22)*.15*energy*opacity);}
                            Expand(ref red,ref green,ref blue,rad,tick,30,12,0xFFE0A0,opacity*.20);
                            Expand(ref red,ref green,ref blue,rad,tick,256,24,color,opacity*.32);
                            Expand(ref red,ref green,ref blue,rad,tick,276,26,color,opacity*.26);
                            for(int i=0;i<16&&flare>0;i++){double a=(i*137.508+tick*.15)*Math.PI/180,rr=229+32*flare+(i%3)*4,s=(i%3==0?3:1.6)*flare;
                                double distance=Math.Abs(dx-rr*Math.Cos(a))+Math.Abs(dy-rr*Math.Sin(a));if(distance<s)Add(ref red,ref green,ref blue,color,(1-distance/s)*.55*flare*opacity);
                                if(i%4==0){double diff=Math.Abs(((angle-a*180/Math.PI+540)%360)-180)*Math.PI/180;double u=(rad-230)/(32+10*flare);if(u>=0&&u<=1&&diff<.012*(1-u))Add(ref red,ref green,ref blue,color,.12*flare*opacity*(1-u));}
                            }
                        }
                    }
                    dest[index]=(byte)Math.Min(255,dest[index]+blue);dest[index+1]=(byte)Math.Min(255,dest[index+1]+green);dest[index+2]=(byte)Math.Min(255,dest[index+2]+red);
                }
                Marshal.Copy(dest,0,pixels.Scan0,dest.Length);
            } finally {image.UnlockBits(pixels);lights.UnlockBits(light);}
        }
        return image;
    }
    public static void Run(string resources,string csv,string root,bool updateCelebration=false,bool resultsOnly=false) {
        textures=new Bitmap[15];for(int i=0;i<8;i++)textures[i]=new Bitmap(Path.Combine(resources,"sector_"+Names[i]+".png"));string[] rest={"ring","medallion","pointer","light_sector","gem_light","light_soft","ring_light"};for(int i=0;i<7;i++)textures[8+i]=new Bitmap(Path.Combine(resources,rest[i]+".png"));
        string frames=Path.Combine(root,"frames-nueva");Directory.CreateDirectory(frames);var lines=File.ReadAllLines(csv);
        using(var results=new Bitmap(1280,680))using(var rg=Graphics.FromImage(results))using(var font=new Font("Segoe UI",13)) {
            rg.Clear(Color.FromArgb(29,37,46));int nFrame=0;
            try {
                for(int n=1;n<lines.Length;n++){string[] tokens=lines[n].Split(',');var row=new double[tokens.Length];for(int j=0;j<tokens.Length;j++)row[j]=double.Parse(tokens[j],CultureInfo.InvariantCulture);
                    int winner=(int)row[1];bool result=Math.Abs(row[0]-256)<.001;if(winner!=0&&!result)continue;
                    if(resultsOnly&&!result)continue;
                    if(updateCelebration&&winner==0&&row[0]<256)continue;
                    using(var b=Frame(row)) {
                        if(winner==0){nFrame=(int)Math.Round(row[0]*30/20)+1;b.Save(Path.Combine(frames,"f"+nFrame.ToString("D4")+".png"),ImageFormat.Png);if(nFrame%30==0)Console.WriteLine("Simulado fotograma "+nFrame);
                            if(Math.Abs(row[0]-310)<.001)for(int y=0;y<640;y++)for(int x=0;x<640;x++)if(b.GetPixel(x,y).ToArgb()!=Color.FromArgb(29,37,46).ToArgb())throw new Exception("Residuo en último fotograma");}
                        if(result){int x=winner%4*320,y=winner/4*340;rg.DrawString(Names[winner],font,Brushes.White,x+10,y+3);rg.DrawImage(b,new Rectangle(x+5,y+30,310,310));}
                    }
                }
                results.Save(Path.Combine(root,"resultados-ocho-colores.png"),ImageFormat.Png);
            } finally {foreach(var b in textures)if(b!=null)b.Dispose();}
        }
    }
}
