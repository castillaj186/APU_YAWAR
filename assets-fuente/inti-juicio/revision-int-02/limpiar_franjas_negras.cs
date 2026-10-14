using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;

// Repairs only long, opaque, near-black horizontal artifact runs in processed active copies.
// Original 1254px source PNGs are never opened for writing.
public class LimpiarFranjasNegras {
  static bool Dark(byte[] p,int o) { return p[o+3]>96 && p[o]<36 && p[o+1]<30 && p[o+2]<30; }
  public static string[] Apply(string dir) {
    var files=Directory.GetFiles(dir,"frame_*.png"); Array.Sort(files,StringComparer.Ordinal);
    var report=new List<string>(); report.Add("frame,segments_repaired,pixels_repaired");
    foreach(var path in files) {
      int segments=0,pixels=0; string temp=path+".clean.png";
      using(var bitmap=new Bitmap(path)) {
        var rect=new Rectangle(0,0,bitmap.Width,bitmap.Height);
        var data=bitmap.LockBits(rect,ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);
        try {
          int stride=Math.Abs(data.Stride); byte[] p=new byte[stride*bitmap.Height]; Marshal.Copy(data.Scan0,p,0,p.Length);
          byte[] source=(byte[])p.Clone();
          for(int y=1;y<bitmap.Height-1;y++) {
            int x=0;
            while(x<bitmap.Width) {
              if(!Dark(source,y*stride+x*4)) { x++; continue; }
              int begin=x; while(x<bitmap.Width && Dark(source,y*stride+x*4))x++;
              int end=x;
              if(end-begin<18) continue;
              int repaired=0;
              for(int px=begin;px<end;px++) {
                int o=y*stride+px*4, above=o-stride, below=o+stride;
                if(source[above+3]<32 || source[below+3]<32) continue;
                for(int c=0;c<4;c++) p[o+c]=(byte)((source[above+c]+source[below+c])/2);
                repaired++;
              }
              if(repaired>0){segments++;pixels+=repaired;}
            }
          }
          Marshal.Copy(p,0,data.Scan0,p.Length);
        } finally { bitmap.UnlockBits(data); }
        if(segments>0) bitmap.Save(temp,ImageFormat.Png);
      }
      if(segments>0) { File.Copy(temp,path,true); File.Delete(temp); report.Add(Path.GetFileName(path)+","+segments+","+pixels); }
    }
    return report.ToArray();
  }
}
