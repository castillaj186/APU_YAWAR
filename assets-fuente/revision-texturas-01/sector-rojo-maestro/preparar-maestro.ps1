param([Parameter(Mandatory=$true)][string]$Arte)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
using System.IO;
public static class RedMaster {
 const int S=512;
 static bool Inside(double x,double y) {
  double r=Math.Sqrt(x*x+y*y), a=Math.Atan2(y,x)*180/Math.PI;
  return r>=22 && r<169 && a>=-90 && a<-45;
 }
 static Color Sample(Bitmap b,double x,double y) {
  x=Math.Max(0,Math.Min(b.Width-1,x));y=Math.Max(0,Math.Min(b.Height-1,y));
  int ix=(int)x,iy=(int)y; double fx=x-ix,fy=y-iy;
  double aa=0,rr=0,gg=0,bb=0;
  for(int j=0;j<2;j++) for(int i=0;i<2;i++) {
   Color c=b.GetPixel(Math.Min(ix+i,b.Width-1),Math.Min(iy+j,b.Height-1));
   double w=(i==0?1-fx:fx)*(j==0?1-fy:fy), aw=w*c.A/255.0;
   aa+=aw;rr+=aw*c.R;gg+=aw*c.G;bb+=aw*c.B;
  }
  if(aa==0)return Color.Transparent;
  return Color.FromArgb((int)Math.Round(aa*255),(int)Math.Round(rr/aa),(int)Math.Round(gg/aa),(int)Math.Round(bb/aa));
 }
 static void Save(Bitmap b,string dir,string name) {
  using(var stream=new FileStream(Path.Combine(dir,name),FileMode.CreateNew))b.Save(stream,ImageFormat.Png);
 }
 public static void Run(string input,string dir) {
  using(var src=new Bitmap(input)) using(var layer=new Bitmap(S,S,PixelFormat.Format32bppArgb)) {
   double angle=-22.5*Math.PI/180,c=Math.Cos(angle),s=Math.Sin(angle);
   for(int y=0;y<S;y++)for(int x=0;x<S;x++) {
    double aa=0,rr=0,gg=0,bb=0;
    for(int j=0;j<4;j++)for(int i=0;i<4;i++) {
     double dx=x+(i+.5)/4-256,dy=y+(j+.5)/4-256;
     if(!Inside(dx,dy))continue;
     double sx=(256+dx*c-dy*s)*src.Width/S-.5;
     double sy=(256+dx*s+dy*c)*src.Height/S-.5;
     Color p=Sample(src,sx,sy);double a=p.A/255.0;
     aa+=a;rr+=a*p.R;gg+=a*p.G;bb+=a*p.B;
    }
    if(aa>0)layer.SetPixel(x,y,Color.FromArgb((int)Math.Round(aa/16*255),(int)Math.Round(rr/aa),(int)Math.Round(gg/aa),(int)Math.Round(bb/aa)));
   }
   Save(layer,dir,"sector_rojo.png");
   // La plantilla comparte exactamente la misma cobertura subpíxel.
   using(var mask=new Bitmap(S,S,PixelFormat.Format32bppArgb)) {
    for(int y=0;y<S;y++)for(int x=0;x<S;x++) {
     int count=0;for(int j=0;j<4;j++)for(int i=0;i<4;i++)if(Inside(x+(i+.5)/4-256,y+(j+.5)/4-256))count++;
     if(count>0)mask.SetPixel(x,y,Color.FromArgb((int)Math.Round(count*255.0/16),255,255,255));
    }
    Save(mask,dir,"plantilla_45.png");
   }
   using(var solo=new Bitmap(1024,1024,PixelFormat.Format32bppArgb)) using(var g=Graphics.FromImage(solo)) {
    Checker(g,1024);g.InterpolationMode=InterpolationMode.HighQualityBicubic;
    g.DrawImage(layer,new Rectangle(0,0,1024,1024)); Save(solo,dir,"vista_sector_solo.png");
   }
   using(var wheel=new Bitmap(1024,1024,PixelFormat.Format32bppArgb)) using(var g=Graphics.FromImage(wheel)) {
    g.Clear(Color.FromArgb(24,27,35));g.SmoothingMode=SmoothingMode.AntiAlias;
    g.InterpolationMode=InterpolationMode.HighQualityBicubic;
    // Siete celdas grises de prueba: no son otros sectores artísticos.
    for(int n=1;n<8;n++) {
     using(var path=new GraphicsPath()) {
      path.AddArc(174,174,676,676,-90+n*45,45);
      double a=(-45+n*45)*Math.PI/180;
      path.AddLine((float)(512+338*Math.Cos(a)),(float)(512+338*Math.Sin(a)),(float)(512+44*Math.Cos(a)),(float)(512+44*Math.Sin(a)));
      path.AddArc(468,468,88,88,-45+n*45,-45);path.CloseFigure();
      using(var brush=new SolidBrush(n%2==0?Color.FromArgb(69,75,86):Color.FromArgb(54,59,70)))g.FillPath(brush,path);
     }
    }
    g.DrawImage(layer,new Rectangle(0,0,1024,1024));
    using(var pen=new Pen(Color.FromArgb(100,181,193,207),1)) {
     g.DrawEllipse(pen,174,174,676,676);g.DrawEllipse(pen,468,468,88,88);
     for(int n=0;n<8;n++) {double a=(-90+n*45)*Math.PI/180;g.DrawLine(pen,(float)(512+44*Math.Cos(a)),(float)(512+44*Math.Sin(a)),(float)(512+338*Math.Cos(a)),(float)(512+338*Math.Sin(a)));}
    }
    using(var font=new Font("Segoe UI",16))using(var brush=new SolidBrush(Color.White))g.DrawString("ROJO + 7 CELDAS GEOMETRICAS DE PRUEBA",font,brush,205,930);
    Save(wheel,dir,"vista_rueda_prueba.png");
   }
  }
 }
 static void Checker(Graphics g,int size) {
  g.Clear(Color.FromArgb(42,45,52));using(var brush=new SolidBrush(Color.FromArgb(55,59,67)))for(int y=0;y<size;y+=32)for(int x=0;x<size;x+=32)if((x/32+y/32)%2==0)g.FillRectangle(brush,x,y,32,32);
 }
}
"@
[RedMaster]::Run((Resolve-Path -LiteralPath $Arte).Path,$PSScriptRoot)