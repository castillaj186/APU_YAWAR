package pe.apuyawar.systems.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import pe.apuyawar.systems.roulette.RoulettePalette;
import pe.apuyawar.systems.roulette.RouletteTimeline;

/** Luz aditiva localizada: solo bandas, arcos y chispas; nunca un disco relleno. */
final class RouletteLighting {
    /** Radio real del borde exterior del aro dorado, en el mismo espacio de textura 512
     * que usa RouletteRenderer — única fuente de verdad, no un valor fijo aparte. */
    private static final double ART_EDGE = RouletteRenderer.WHEEL_DIAMETER/2.0;
    // Halo principal: ~15% del radio exterior más allá del aro. HALO_OUTER es el
    // alcance normal de la corona (~25%); solo la explosión final llega más lejos,
    // usando BURST_REACH (~45%).
    private static final double HALO_CENTER = ART_EDGE*1.15;
    private static final double HALO_OUTER = ART_EDGE*1.25;
    private static final double BURST_REACH = ART_EDGE*1.45;
    private static final Identifier SECTOR=RouletteDraw.texture("light_sector");
    private static final Identifier GEM=RouletteDraw.texture("gem_light");
    private static final Identifier SPOT=RouletteDraw.texture("light_soft");
    private static final Identifier RIM=RouletteDraw.texture("ring_light");
    /**
     * Capa de dibujo propia sobre el mismo mecanismo que usa {@code DrawContext}
     * (VertexConsumerProvider.Immediate): mismo shader de HUD 2D ({@code GUI_PROGRAM},
     * el que envuelve {@code GameRenderer.getRenderTypeGuiProgram()}), misma
     * transparencia y depth test que la capa nativa "gui". No tocar sin repetir el
     * diagnóstico A/B/C documentado en docs/10_DESTELLOS_RULETA_ESTADO.md — un orden
     * de vértices equivocado aquí hace que todo el brillo sea invisible sin error.
     */
    private static final RenderLayer GLOW = RenderLayer.of(
            "apuyawar_roulette_glow",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            1536,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.GUI_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .build(false));
    private RouletteLighting() {}
    static void sector(DrawContext context,float rotation,int color,float strength,float scale) {
        RouletteDraw.layer(context,SECTOR,rotation,scale,color,strength,true);
    }
    static void gem(DrawContext context,double tick,int winner,float opacity) {
        int color=RouletteGem.color(tick,winner);
        // El velo sigue el relieve del cristal; el cuerpo dorado no participa.
        RouletteDraw.layer(context,GEM,0,1,color,opacity*(float)RouletteGem.overlayStrength(tick,winner),false);
        RouletteDraw.layer(context,GEM,0,1,color,
                opacity*.35f*(float)RouletteTimeline.gemStrength(tick,winner),true);
    }
    static void rim(DrawContext context,float rotation,int winner,float strength) {
        RouletteDraw.layer(context,RIM,rotation,1,RoulettePalette.RGB[winner],strength,true);
    }
    static void spinRim(DrawContext context,double tick,int winner,float rotation,float opacity) {
        float strength=(float)(RouletteTimeline.energy(tick,winner)*(.08+.04*Math.sin(tick*.35)));
        if(strength>0)RouletteDraw.layer(context,RIM,rotation,1,RouletteGem.color(tick,winner),opacity*strength,true);
    }
    static void energy(DrawContext context,double tick,int winner,double rotation,float opacity) {
        if(opacity<=0)return;
        double speed=RouletteTimeline.energy(tick,winner);
        double celebration=RouletteTimeline.celebration(tick);
        double exit=RouletteTimeline.exitPulse(tick);
        int color=RoulettePalette.RGB[winner];
        Matrix4f matrix=context.getMatrices().peek().getPositionMatrix();
        VertexConsumer buffer=context.getVertexConsumers().getBuffer(GLOW);
        fullEffects(buffer,matrix,tick,winner,rotation,opacity,speed,celebration,exit,color);
        // Destello central pequeño y fugaz; no blanquea la rueda completa.
        double central=RouletteTimeline.pulse(tick,RouletteTimeline.FILL_END,6);
        if(central>0)RouletteDraw.layer(context,SPOT,0,.07f,0xFFFFFF,(float)(central*.60*opacity),true);
    }
    /** Corona mágica base, arcoíris de giro, construcción/transformación por sector y explosión final. */
    private static void fullEffects(VertexConsumer buffer,Matrix4f matrix,double tick,int winner,double rotation,
                                     float opacity,double speed,double celebration,double exit,int color) {
        // Aura dorada permanente, ceñida al aro (no un anillo grueso).
        ring(buffer,matrix,ART_EDGE+9,10,360,0,0xFFD38B,.13*opacity*RouletteTimeline.frameAlpha(tick));
        ring(buffer,matrix,ART_EDGE-5,3,360,0,0xFFD38B,.06*opacity*RouletteTimeline.frameAlpha(tick));
        // Corona principal: un grupo de destellos por sector. El color de cada grupo
        // sigue EXACTAMENTE el mismo progreso 0..1 que usa RouletteRenderer para
        // transformar el sector físico (fillBlend/RouletteFillSequence) — no hay un
        // temporizador aparte, así que cada grupo cambia al color ganador al mismo
        // ritmo que su sector, nunca los ocho a la vez.
        for(int i=0;i<8;i++) {
            double t=RouletteTimeline.fillBlend(tick,i,winner);
            int sectorColor=lerpColor(RoulettePalette.RGB[i],RoulettePalette.RGB[winner],t);
            // Antes de la mitad de su transformación, el destello conserva el "estilo"
            // de su propio color original (para no delatar al ganador antes de tiempo);
            // pasada la mitad adopta el estilo del color ganador.
            RouletteTheme theme=t<.5?RouletteTheme.of(i):RouletteTheme.of(winner);
            double a=rotation+i*45-67.5;
            // Ondulación suave: da vida sin parpadeo errático ni movimiento aleatorio.
            double wobble=.5+.5*Math.sin(tick*.6+i*1.9);
            double len=HALO_CENTER+(i%2==0?9:2)+9*speed+5*wobble;
            double strength=(.38+.5*speed)*opacity;
            themedBurst(buffer,matrix,tick,a,ART_EDGE-8,len,sectorColor,strength,theme);
            // Estela corta que acompaña el giro real de ESTE sector: copias rezagadas y
            // cortas, no un anillo sólido. Se intensifica al acelerar y se desvanece al frenar.
            if(speed>.05) {
                for(int k=1;k<=3;k++) {
                    double lag=k*speed*7.0;
                    double trailAlpha=strength*.42*(1-k/4.0);
                    ring(buffer,matrix,HALO_CENTER-3,6,15,a-lag-6,sectorColor,trailAlpha);
                }
            }
            // Construcción inicial del sector (antes de empezar a transformarse).
            double entry=RouletteTimeline.buildPulse(tick,i);
            double sectorAngle=rotation+i*45-89;
            if(entry>.01) {
                ring(buffer,matrix,154+14*entry,2.8,40,sectorAngle,RoulettePalette.RGB[i],.48*entry*opacity);
                double ea=Math.toRadians(rotation+i*45-67.5);
                star(buffer,matrix,Math.cos(ea)*(160+14*entry),Math.sin(ea)*(160+14*entry),2.8*entry,RoulettePalette.RGB[i],.7*entry*opacity);
            }
            // Pulso pequeño y elegante justo cuando ESTE sector termina su transformación:
            // color ganador mezclado con dorado, chispas suaves saliendo del aro, y un
            // pequeño estallido de partículas — la energía conquistando la ruleta
            // sector por sector, no de golpe. El rebote del ícono vive en RouletteRenderer
            // (misma fillPulse, sin temporizador aparte).
            double pulse=RouletteTimeline.fillPulse(tick,i,winner);
            if(pulse>.01) {
                int pulseColor=lerpColor(color,0xFFE9A8,.35);
                ring(buffer,matrix,159+16*pulse,3.6,40,sectorAngle,pulseColor,.68*pulse*opacity);
                double pa=Math.toRadians(rotation+i*45-67.5);
                star(buffer,matrix,Math.cos(pa)*(168+14*pulse),Math.sin(pa)*(168+14*pulse),3.2*pulse,pulseColor,.85*pulse*opacity);
                for(int p=0;p<3;p++) {
                    double pAng=Math.toRadians(rotation+i*45-67.5+(p-1)*14);
                    double pr=150+30*pulse;
                    diamond(buffer,matrix,Math.cos(pAng)*pr,Math.sin(pAng)*pr,2.0*pulse,pulseColor,.7*pulse*opacity);
                }
            }
        }
        for(var particle:RouletteParticles.active()) {
            double travel=particle.travel(tick),fade=particle.alpha(tick);
            diamond(buffer,matrix,particle.x()+particle.vx()*travel,particle.y()+particle.vy()*travel,
                    particle.size()*(.5+.5*fade),particle.color(),.8*fade*opacity);
        }
        expanding(buffer,matrix,tick,RouletteTimeline.SPIN_START-2,10,0xFFE0A0,opacity*.26);
        expanding(buffer,matrix,tick,RouletteTimeline.FILL_END,16,color,opacity*.55);
        expanding(buffer,matrix,tick,RouletteTimeline.HOLD_END-2,18,color,opacity*.34);
        // Explosión final: SOLO arranca en FILL_END, cuando los ocho sectores ya
        // terminaron de transformarse (celebration()/exit() son cero antes de eso),
        // así que nunca aparece la animación temática antes de tiempo. Con el ganador
        // ya revelado en los ocho sectores, elegir el tema aquí no delata nada.
        double flare=Math.max(celebration,exit);
        if(flare>.001) drawFinale(buffer,matrix,tick,rotation,winner,color,flare,opacity);
    }
    /** Ráfaga temática continua de un sector (durante el giro y la transformación):
     * variación ligera por estilo, pensada para sostenerse muchos frames sin saturar
     * — la identidad fuerte de cada color vive en {@link #drawFinale}. */
    private static void themedBurst(VertexConsumer b,Matrix4f m,double tick,double a,double from,double len,
                                     int color,double strength,RouletteTheme theme) {
        double tip=Math.toRadians(a);
        switch(theme.style) {
            case SANCTION -> { // azul: rígido y geométrico, sin ondulación ni grietas
                glowRay(b,m,a,from,len,color,strength);
                ray(b,m,a,from+(len-from)*.55,from+(len-from)*.80,theme.accent,strength*.5,.020);
                star(b,m,Math.cos(tip)*len,Math.sin(tip)*len,1.8+1.6*strength,theme.accent,strength*.7);
            }
            case DANGER -> { // rojo: grieta intermitente, nunca aleatoria (depende del tick)
                glowRay(b,m,a,from,len,color,strength);
                if(Math.sin(tick*1.8+a*1.3)>.55) ray(b,m,a+7,from+5,from+(len-from)*.65,theme.accent,strength*.55,.018);
                star(b,m,Math.cos(tip)*len,Math.sin(tip)*len,2.2+2.2*strength,theme.accent,strength*.85);
            }
            case INFERNO -> { // naranja: ondula y la chispa de la punta sube, como brasa
                double wave=Math.sin(tick*2.4+a*.35)*6;
                double x=Math.cos(tip)*len+wave*.3,y=Math.sin(tip)*len-theme.drift*.35;
                glowRay(b,m,a+wave*.09,from,len-3,color,strength);
                star(b,m,x,y,2.0+2.2*strength,theme.accent,strength*.8);
            }
            case MISSION -> { // turquesa: anillo de acento a mitad de camino, como un marcador
                glowRay(b,m,a,from,len,color,strength*.9);
                ring(b,m,from+(len-from)*.62,2.4,9,a-4.5,theme.accent,strength*.5);
                star(b,m,Math.cos(tip)*len,Math.sin(tip)*len,1.8+1.8*strength,theme.accent,strength*.7);
            }
            case FESTIVAL -> { // rosa: la chispa de la punta rebota, juguetona
                double bounce=Math.abs(Math.sin(tick*2.2+a*.3))*3;
                glowRay(b,m,a,from,len,color,strength*.85);
                star(b,m,Math.cos(tip)*(len+bounce),Math.sin(tip)*(len+bounce),2.0+2.2*strength,theme.accent,strength*.8);
            }
            default -> { // VORTEX, BLESSING, UNLOCK: halo suave estándar
                glowRay(b,m,a,from,len,color,strength);
                star(b,m,Math.cos(tip)*len,Math.sin(tip)*len,1.8+2.0*strength,theme.accent,strength*.65);
            }
        }
    }
    /** Despacha la celebración única de cada resultado — ocho animaciones distintas,
     * no un mismo diseño recoloreado. Solo se llama cuando los ocho sectores ya
     * terminaron de transformarse (ver flare en fullEffects). */
    private static void drawFinale(VertexConsumer b,Matrix4f m,double tick,double rotation,int winner,int color,
                                    double flare,double opacity) {
        RouletteTheme theme=RouletteTheme.of(winner);
        double reach=ART_EDGE+36+(BURST_REACH-ART_EDGE-36)*flare;
        switch(theme.style) {
            case DANGER   -> finaleDanger(b,m,tick,rotation,color,theme,flare,opacity,reach);
            case FESTIVAL -> finaleFestival(b,m,tick,rotation,color,theme,flare,opacity,reach);
            case VORTEX   -> finaleVortex(b,m,tick,rotation,color,theme,flare,opacity,reach);
            case SANCTION -> finaleSanction(b,m,tick,rotation,color,theme,flare,opacity,reach);
            case MISSION  -> finaleMission(b,m,tick,rotation,color,theme,flare,opacity,reach);
            case BLESSING -> finaleBlessing(b,m,tick,rotation,color,theme,flare,opacity,reach);
            case UNLOCK   -> finaleUnlock(b,m,tick,rotation,color,theme,flare,opacity,reach);
            case INFERNO  -> finaleInferno(b,m,tick,rotation,color,theme,flare,opacity,reach);
        }
    }
    /** Rojo — Momento Reviil: tormenta de peligro, rayos que se ramifican, fracturas y
     * destellos blancos intensos, con un pulso de advertencia rojo oscuro. */
    private static void finaleDanger(VertexConsumer b,Matrix4f m,double tick,double rotation,int color,
                                      RouletteTheme theme,double flare,double opacity,double reach) {
        int dark=lerpColor(color,0xFF000000,.4);
        ring(b,m,HALO_OUTER+10*flare,9,360,0,dark,.30*flare*opacity);
        for(int i=0;i<10;i++) {
            double a=i*36+rotation*.3;
            double len=reach*(i%2==0?1:.72);
            double mid=ART_EDGE-8+(len-(ART_EDGE-8))*.52;
            glowRay(b,m,a,ART_EDGE-8,mid,color,.65*flare*opacity);
            double branchA=a+theme.jitter*22*Math.sin(tick*3+i);
            glowRay(b,m,branchA,mid,len,color,.5*flare*opacity);
            if(i%3==0) star(b,m,Math.cos(Math.toRadians(a))*len,Math.sin(Math.toRadians(a))*len,4.5*flare,0xFFFFFFFF,.9*flare*opacity);
        }
        for(int i=0;i<14;i++) {
            double a=i*25.7+tick*.6;
            ray(b,m,a,ART_EDGE-4,ART_EDGE+16+10*flare,theme.accent,.5*flare*opacity,.015);
        }
        // Estela curva de advertencia, oscura, barriendo lentamente alrededor del aro.
        tiltedArc(b,m,HALO_OUTER+14*flare,6,.08,0,160,rotation*.4+tick*18,dark,.4*flare*opacity);
    }
    /** Rosa — Momento Cuy: portal circular, confeti pixelado rebotando y burbujas
     * mágicas que suben; alegre y juguetón, nunca amenazante. */
    private static void finaleFestival(VertexConsumer b,Matrix4f m,double tick,double rotation,int color,
                                        RouletteTheme theme,double flare,double opacity,double reach) {
        double portalR=ART_EDGE*.5+6*Math.sin(tick*2);
        ring(b,m,portalR,5,360,0,color,.5*flare*opacity);
        ring(b,m,portalR-9,3,360,0,0xFFFFFFFF,.35*flare*opacity);
        for(int i=0;i<16;i++) {
            double a=i*22.5+tick*1.1,r=HALO_CENTER+(18+40*((i*.37)%1.0))*flare;
            double bounce=Math.abs(Math.sin(tick*3+i))*10*flare;
            double x=Math.cos(Math.toRadians(a))*r,y=Math.sin(Math.toRadians(a))*r-bounce;
            int c=i%3==0?color:(i%3==1?0xFFFFFFFF:theme.accent);
            diamond(b,m,x,y,(i%2==0?3.4:2.2)*flare,c,.8*flare*opacity);
        }
        for(int i=0;i<10;i++) {
            double a=i*36+tick*.5,r=HALO_OUTER+(6+26*((i*.21)%1.0))*flare;
            double rise=(tick*1.4+i*2.6)%22;
            double x=Math.cos(Math.toRadians(a))*r,y=Math.sin(Math.toRadians(a))*r-rise*flare;
            star(b,m,x,y,2.1*flare,0xFFFFFFFF,.55*flare*opacity*(1-rise/22.0));
        }
    }
    /** Morado — reinicio de la ruleta: dos espirales de energía girando en sentidos
     * opuestos y la energía concentrándose de nuevo en el centro. */
    private static void finaleVortex(VertexConsumer b,Matrix4f m,double tick,double rotation,int color,
                                      RouletteTheme theme,double flare,double opacity,double reach) {
        double baseR=ART_EDGE+6,growR=baseR+(reach-baseR)*flare;
        spiral(b,m,baseR,growR,rotation+tick*6,300,7,color,.6*flare*opacity);
        spiral(b,m,baseR,growR,-rotation-tick*6,-300,7,theme.accent,.5*flare*opacity);
        double spinA=tick*6;
        arrow(b,m,Math.cos(Math.toRadians(spinA))*42,Math.sin(Math.toRadians(spinA))*42,spinA+90,10*flare,theme.accent,.8*flare*opacity);
        arrow(b,m,Math.cos(Math.toRadians(spinA+180))*42,Math.sin(Math.toRadians(spinA+180))*42,spinA+270,10*flare,color,.8*flare*opacity);
        star(b,m,0,0,4*flare,0xFFFFFFFF,.5*flare*opacity);
    }
    /** Azul — sanción administrativa: sello holográfico geométrico (anillos
     * poligonales, líneas rectas como un sistema de seguridad, pulso que recorre el
     * círculo e impacto final contundente). Deliberadamente rígido, no eléctrico. */
    private static void finaleSanction(VertexConsumer b,Matrix4f m,double tick,double rotation,int color,
                                        RouletteTheme theme,double flare,double opacity,double reach) {
        polyRing(b,m,HALO_CENTER+10*flare,5,6,rotation*.2,color,.55*flare*opacity);
        polyRing(b,m,HALO_OUTER+6*flare,3,8,-rotation*.15,theme.accent,.4*flare*opacity);
        for(int i=0;i<6;i++) {
            double a=i*60+rotation*.2;
            ray(b,m,a,ART_EDGE-6,HALO_CENTER+16*flare,theme.accent,.5*flare*opacity,.012);
        }
        double sweepA=(tick*55)%360;
        ring(b,m,HALO_CENTER+10*flare,6,24,sweepA,0xFFFFFFFF,.85*flare*opacity);
        double central=Math.min(1,flare*1.2);
        star(b,m,0,0,6*central,0xFFFFFFFF,.5*central*opacity);
        if(flare>.7) star(b,m,0,0,9*(flare-.7)/.3,theme.accent,.7*flare*opacity);
    }
    /** Turquesa/celeste — misión activada: anillos concéntricos tipo radar, marcadores
     * en forma de flecha apuntando hacia el centro y chispas de confirmación. */
    private static void finaleMission(VertexConsumer b,Matrix4f m,double tick,double rotation,int color,
                                       RouletteTheme theme,double flare,double opacity,double reach) {
        for(int k=0;k<3;k++) {
            double rr=ART_EDGE+16+k*16+flare*18;
            double a=flare-k*.22;
            if(a>0) ring(b,m,rr,3,360,0,color,Math.min(1,a)*.4*opacity);
        }
        for(int i=0;i<8;i++) {
            double a=i*45+rotation*.1,r=HALO_CENTER+18*flare;
            double x=Math.cos(Math.toRadians(a))*r,y=Math.sin(Math.toRadians(a))*r;
            arrow(b,m,x,y,a+180,8*flare,theme.accent,.8*flare*opacity);
        }
        for(int i=0;i<10;i++) {
            double a=i*137.5+tick*.5,r=HALO_OUTER+20*((i*.31)%1.0)*flare;
            star(b,m,Math.cos(Math.toRadians(a))*r,Math.sin(Math.toRadians(a))*r,2.4*flare,0xFFFFFFFF,.6*flare*opacity);
        }
    }
    /** Verde — beneficios: onda esmeralda, hojas luminosas ascendentes con destellos
     * dorados y un halo protector que aparece y se desvanece. */
    private static void finaleBlessing(VertexConsumer b,Matrix4f m,double tick,double rotation,int color,
                                        RouletteTheme theme,double flare,double opacity,double reach) {
        ring(b,m,HALO_CENTER+14*flare,10,360,0,color,.4*flare*opacity);
        for(int i=0;i<theme.sparkles;i++) {
            double a=i*137.508+tick*.3,r=HALO_OUTER+(10+40*((i*.19)%1.0))*flare;
            double rise=(tick*1.1+i*2.3)%20;
            double x=Math.cos(Math.toRadians(a))*r,y=Math.sin(Math.toRadians(a))*r-rise*flare;
            diamond(b,m,x,y,(i%2==0?3.2:2.0)*flare,i%3==0?theme.accent:color,.7*flare*opacity*(1-rise/20.0));
        }
        ring(b,m,HALO_OUTER+6*flare,4,360,0,theme.accent,.3*Math.sin(Math.PI*Math.min(1,flare))*opacity);
    }
    /** Amarillo — desbloqueo de recursos: candado luminoso abriéndose, rayos dorados
     * y fragmentos minerales dispersándose, como liberar un tesoro. */
    private static void finaleUnlock(VertexConsumer b,Matrix4f m,double tick,double rotation,int color,
                                      RouletteTheme theme,double flare,double opacity,double reach) {
        double openT=Math.min(1,flare*1.3);
        double glyphR=HALO_CENTER-4;
        ring(b,m,glyphR,3,150,80+openT*40,0xFFFFFFFF,.7*flare*opacity);
        ring(b,m,glyphR-16,9,44,68,color,.65*flare*opacity);
        for(int i=0;i<14;i++) {
            double a=i*25.7+rotation*.2;
            glowRay(b,m,a,ART_EDGE-6,reach*(i%2==0?1:.7),color,.55*flare*opacity);
        }
        for(int i=0;i<theme.sparkles;i++) {
            double a=i*137.508+tick*.4,r=HALO_OUTER+(10+45*((i*.27)%1.0))*flare;
            double x=Math.cos(Math.toRadians(a))*r,y=Math.sin(Math.toRadians(a))*r;
            diamond(b,m,x,y,2.6*flare,i%2==0?0xFFFFFFFF:theme.accent,.75*flare*opacity);
        }
    }
    /** Naranja — aumento de dificultad: llamas pegadas al aro, brasas que ascienden,
     * ondas de calor y pulsos que se acumulan, como una advertencia creciente. */
    private static void finaleInferno(VertexConsumer b,Matrix4f m,double tick,double rotation,int color,
                                       RouletteTheme theme,double flare,double opacity,double reach) {
        for(int i=0;i<12;i++) {
            double a=i*30+rotation*.15;
            double wave=Math.sin(tick*2.6+a*.4)*8;
            double len=reach*(i%2==0?1:.75);
            glowRay(b,m,a+wave*.1,ART_EDGE-6,len,color,.6*flare*opacity);
        }
        for(int i=0;i<theme.sparkles;i++) {
            double a=i*137.508+tick*.5,r=HALO_OUTER+(10+45*((i*.23)%1.0))*flare;
            double rise=(tick*1.6+i*1.7)%18;
            double x=Math.cos(Math.toRadians(a))*r,y=Math.sin(Math.toRadians(a))*r-rise*flare;
            star(b,m,x,y,2.8*flare,i%2==0?theme.accent:color,.7*flare*opacity*(1-rise/18.0));
        }
        for(int k=0;k<3;k++) {
            double rr=HALO_OUTER+(10+18*k)*flare;
            ring(b,m,rr,3,360,0,lerpColor(color,0xFFFFCC66,.4),(.24-.05*k)*flare*opacity);
        }
        // Onda de calor curva, ondulante, como aire caliente ascendiendo junto al aro.
        tiltedArc(b,m,HALO_OUTER+20*flare,6,.30,15+tick*10,170,rotation*.3+tick*26,theme.accent,.35*flare*opacity);
    }
    /** Mezcla lineal de color RGB; reutilizado por RouletteRenderer para que el brillo
     * del ícono siga el mismo progreso por sector que el resto de los destellos. */
    static int lerpColor(int c1,int c2,double t) {
        t=Math.max(0,Math.min(1,t));
        int r=(int)Math.round(((c1>>16)&255)*(1-t)+((c2>>16)&255)*t);
        int g=(int)Math.round(((c1>>8)&255)*(1-t)+((c2>>8)&255)*t);
        int b=(int)Math.round((c1&255)*(1-t)+(c2&255)*t);
        return (r<<16)|(g<<8)|b;
    }
    /** Espiral: banda que gira con el ángulo mientras el radio crece/decrece entre
     * rStart y rEnd a lo largo del barrido — usada por el vórtice morado. */
    private static void spiral(VertexConsumer b,Matrix4f m,double rStart,double rEnd,double angleStartDeg,
                                double sweepDeg,double width,int color,double alpha) {
        if(alpha<=0)return;
        int segments=Math.max(10,(int)(Math.abs(sweepDeg)/8));
        for(int i=0;i<segments;i++) {
            double t0=(double)i/segments,t1=(double)(i+1)/segments;
            double a0=Math.toRadians(angleStartDeg+sweepDeg*t0),a1=Math.toRadians(angleStartDeg+sweepDeg*t1);
            double r0=rStart+(rEnd-rStart)*t0,r1=rStart+(rEnd-rStart)*t1;
            double fade=Math.sin(Math.PI*(i+.5)/segments);
            double ix=(r0-width/2)*Math.cos(a0),iy=(r0-width/2)*Math.sin(a0);
            double jx=(r1-width/2)*Math.cos(a1),jy=(r1-width/2)*Math.sin(a1);
            double ox=(r0+width/2)*Math.cos(a0),oy=(r0+width/2)*Math.sin(a0);
            double px=(r1+width/2)*Math.cos(a1),py=(r1+width/2)*Math.sin(a1);
            vertex(b,m,ix,iy,color,alpha*fade);vertex(b,m,jx,jy,color,alpha*fade);
            vertex(b,m,px,py,color,alpha*fade);vertex(b,m,ox,oy,color,alpha*fade);
        }
    }
    /** Anillo poligonal (facetado, no suave) — usado por el sello azul para una
     * sensación geométrica/tecnológica en vez de un círculo orgánico. */
    private static void polyRing(VertexConsumer b,Matrix4f m,double radius,double width,int sides,
                                  double rotationDeg,int color,double alpha) {
        if(alpha<=0)return;
        double sweep=360.0/sides;
        for(int i=0;i<sides;i++) {
            double a=Math.toRadians(rotationDeg+i*sweep),z=Math.toRadians(rotationDeg+(i+1)*sweep);
            band(b,m,radius-width,radius,a,z,color,alpha,alpha);
        }
    }
    /** Flecha triangular en coordenadas cartesianas arbitrarias (no relativa al
     * centro de la rueda) — marcadores de misión, símbolo de repetición. */
    private static void arrow(VertexConsumer b,Matrix4f m,double x,double y,double dirDeg,double size,int color,double alpha) {
        if(alpha<=0||size<=0)return;
        double d=Math.toRadians(dirDeg);
        double tipX=x+size*Math.cos(d),tipY=y+size*Math.sin(d);
        double leftX=x+size*.55*Math.cos(d+2.5),leftY=y+size*.55*Math.sin(d+2.5);
        double rightX=x+size*.55*Math.cos(d-2.5),rightY=y+size*.55*Math.sin(d-2.5);
        vertex(b,m,tipX,tipY,color,alpha);
        vertex(b,m,leftX,leftY,color,alpha*.15);
        vertex(b,m,rightX,rightY,color,alpha*.15);
        vertex(b,m,rightX,rightY,color,alpha*.15);
    }
    /** Tilted, squashed ring arc: the "comet swirl" sweeping past the burst, à la Saturn's rings. */
    private static void tiltedArc(VertexConsumer b,Matrix4f m,double radius,double width,double squash,
                                   double tiltDeg,double sweep,double angleDeg,int color,double alpha) {
        if(alpha<=0)return;
        double tilt=Math.toRadians(tiltDeg);
        double ct=Math.cos(tilt),st=Math.sin(tilt);
        int segments=Math.max(6,(int)(sweep/5));
        for(int i=0;i<segments;i++) {
            double a=Math.toRadians(angleDeg+sweep*i/segments),z=Math.toRadians(angleDeg+sweep*(i+1)/segments);
            double fade=Math.sin(Math.PI*(i+.5)/segments);
            ellipseBand(b,m,radius-width,radius,a,z,ct,st,squash,color,0,alpha*fade);
            ellipseBand(b,m,radius,radius+width,a,z,ct,st,squash,color,alpha*fade,0);
        }
    }
    private static void ellipseBand(VertexConsumer b,Matrix4f m,double inner,double outer,double a,double z,
                                     double ct,double st,double squash,int color,double ia,double oa) {
        double ix=ellX(inner,a,ct,st,squash),iy=ellY(inner,a,ct,st,squash);
        double jx=ellX(inner,z,ct,st,squash),jy=ellY(inner,z,ct,st,squash);
        double ox=ellX(outer,a,ct,st,squash),oy=ellY(outer,a,ct,st,squash);
        double px=ellX(outer,z,ct,st,squash),py=ellY(outer,z,ct,st,squash);
        // Orden igual al de DrawContext.fill(): (u1,v1)->(u1,v2)->(u2,v2)->(u2,v1).
        vertex(b,m,ix,iy,color,ia);vertex(b,m,jx,jy,color,ia);vertex(b,m,px,py,color,oa);vertex(b,m,ox,oy,color,oa);
    }
    private static double ellX(double r,double a,double ct,double st,double squash) {
        double x=r*Math.cos(a),y=r*Math.sin(a)*squash;
        return x*ct-y*st;
    }
    private static double ellY(double r,double a,double ct,double st,double squash) {
        double x=r*Math.cos(a),y=r*Math.sin(a)*squash;
        return x*st+y*ct;
    }
    private static void expanding(VertexConsumer b,Matrix4f m,double tick,double start,double duration,int color,double opacity) {
        double u=(tick-start)/duration;if(u<=0||u>=1)return;
        ring(b,m,ART_EDGE+u*64,6*(1-u)+1,360,0,color,opacity*Math.sin(Math.PI*u)*(1-u));
    }
    private static void ring(VertexConsumer b,Matrix4f m,double radius,double width,double sweep,double angle,int color,double alpha) {
        if(alpha<=0)return;
        int segments=Math.max(4,(int)(sweep/6));
        for(int i=0;i<segments;i++) {
            double a=Math.toRadians(angle+sweep*i/segments),z=Math.toRadians(angle+sweep*(i+1)/segments);
            double fade=sweep<360?Math.sin(Math.PI*(i+.5)/segments):1;
            band(b,m,radius-width,radius,a,z,color,0,alpha*fade);
            band(b,m,radius,radius+width,a,z,color,alpha*fade,0);
        }
    }
    // Orden de vértices igual al de DrawContext.fill(): (u1,v1)->(u1,v2)->(u2,v2)->(u2,v1).
    // Con el orden anterior (u1,v1)->(u2,v1)->(u2,v2)->(u1,v2) el cuadrilátero quedaba
    // orientado al revés y OpenGL lo descartaba por backface culling: nunca se veía.
    private static void band(VertexConsumer b,Matrix4f m,double inner,double outer,double a,double z,int color,double ia,double oa) {
        vertex(b,m,inner*Math.cos(a),inner*Math.sin(a),color,ia);
        vertex(b,m,inner*Math.cos(z),inner*Math.sin(z),color,ia);
        vertex(b,m,outer*Math.cos(z),outer*Math.sin(z),color,oa);
        vertex(b,m,outer*Math.cos(a),outer*Math.sin(a),color,oa);
    }
    private static void diamond(VertexConsumer b,Matrix4f m,double x,double y,double size,int color,double alpha) {
        if(size<=0)return;
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2,z=(i+1)*Math.PI/2;
            double ex=x+size*Math.cos(a),ey=y+size*Math.sin(a);
            vertex(b,m,x,y,color,alpha);
            vertex(b,m,x+size*Math.cos(z),y+size*Math.sin(z),color,0);
            vertex(b,m,ex,ey,color,0);
            vertex(b,m,ex,ey,color,0);
        }
    }
    /** Eight compact rays; bounded count and no object allocation per frame. */
    private static void star(VertexConsumer b,Matrix4f m,double x,double y,double size,int color,double alpha) {
        if(size<=0||alpha<=0)return;
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4, z=a+Math.PI/4;
            double outer=(i%2==0?1.8:1.05)*size;
            double ex=x+size*Math.cos(z),ey=y+size*Math.sin(z);
            vertex(b,m,x,y,color,alpha);
            vertex(b,m,ex,ey,color,0);
            vertex(b,m,x+outer*Math.cos(a),y+outer*Math.sin(a),color,0);
            vertex(b,m,x+outer*Math.cos(a),y+outer*Math.sin(a),color,0);
        }
    }
    /** Rayo mágico de tres capas: halo exterior ancho y suave, cuerpo principal más
     * grueso y brillante con el color real, y un núcleo estrecho casi blanco — dos o
     * tres veces más grueso que un rayo simple, con sensación de volumen real en vez
     * de una línea plana. */
    private static void glowRay(VertexConsumer b,Matrix4f m,double angle,double from,double to,int color,double alpha) {
        ray(b,m,angle,from-7,to,color,alpha*.38,.075);
        ray(b,m,angle,from-3,from+(to-from)*.90,color,Math.min(1,alpha*.95),.034);
        int core=lerpColor(color,0xFFFFFFFF,.72);
        ray(b,m,angle,from,from+(to-from)*.72,core,Math.min(1,alpha*1.1),.013);
    }
    private static void ray(VertexConsumer b,Matrix4f m,double angle,double from,double to,int color,double alpha) {
        ray(b,m,angle,from,to,color,alpha,.012);
    }
    private static void ray(VertexConsumer b,Matrix4f m,double angle,double from,double to,int color,double alpha,double halfWidth) {
        double a=Math.toRadians(angle);
        double tx=to*Math.cos(a),ty=to*Math.sin(a);
        vertex(b,m,from*Math.cos(a+halfWidth),from*Math.sin(a+halfWidth),color,alpha);
        vertex(b,m,tx,ty,color,0);
        vertex(b,m,tx,ty,color,0);
        vertex(b,m,from*Math.cos(a-halfWidth),from*Math.sin(a-halfWidth),color,alpha);
    }
    private static void vertex(VertexConsumer b,Matrix4f m,double x,double y,int color,double alpha) {
        b.vertex(m,(float)x,(float)y,0).color(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,(float)alpha);
    }
}
