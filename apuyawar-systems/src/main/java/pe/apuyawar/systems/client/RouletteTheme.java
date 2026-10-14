package pe.apuyawar.systems.client;

/**
 * Identidad visual por resultado oficial de APU YAWAR — no un recolor del mismo
 * efecto, sino una animación propia ligada al significado real de cada color dentro
 * del evento. Indexado igual que {@code RoulettePalette.NAMES}/{@code RGB} (mismo
 * orden, 8 entradas: rojo, rosa, morado, azul, turquesa, verde, amarillo, naranja).
 */
final class RouletteTheme {
    /** Un estilo por resultado; cada uno tiene su propio dibujo en RouletteLighting,
     * tanto para el destello continuo por sector como para la celebración final. */
    enum Style {
        /** Rojo — Momento Reviil: tormenta de peligro, rayos que se ramifican. */
        DANGER,
        /** Rosa — Momento Cuy: portal y festival mágico, juguetón. */
        FESTIVAL,
        /** Morado — reinicio de la ruleta: vórtice de dos espirales. */
        VORTEX,
        /** Azul — sanción administrativa: sello holográfico geométrico. */
        SANCTION,
        /** Turquesa/celeste — misiones: anillos concéntricos y marcadores convergentes. */
        MISSION,
        /** Verde — beneficios: bendición esmeralda con hojas y destellos dorados. */
        BLESSING,
        /** Amarillo — desbloqueo de recursos: candado luminoso y fragmentos minerales. */
        UNLOCK,
        /** Naranja — aumento de dificultad: llamas, brasas y ondas de calor. */
        INFERNO
    }
    final Style style;
    /** Color secundario de acento (chispas, bordes, glifo), distinto del color oficial. */
    final int accent;
    /** 0..1: cuánto se desvían/ramifican los rayos de una línea recta. */
    final double jitter;
    /** Deriva vertical de las partículas (positivo = suben, como brasas/burbujas). */
    final double drift;
    /** 0..1: cuán difuminado/amplio es el halo frente a cuán nítido/nítido-geométrico. */
    final double softness;
    /** Cantidad base de chispas/partículas en la celebración final de este tema. */
    final int sparkles;
    private RouletteTheme(Style style,int accent,double jitter,double drift,double softness,int sparkles) {
        this.style=style;this.accent=accent;this.jitter=jitter;this.drift=drift;this.softness=softness;this.sparkles=sparkles;
    }
    /** Orden real de RoulettePalette.NAMES: rojo, rosa, morado, azul, turquesa, verde, amarillo, naranja. */
    static final RouletteTheme[] BY_INDEX = {
        new RouletteTheme(Style.DANGER,   0xFFE6B0, .60, 4,  .20, 10), // rojo: peligro, rayos ramificados
        new RouletteTheme(Style.FESTIVAL, 0xFFF3FA, .08, -6, .85, 20), // rosa: portal, confeti, burbujas
        new RouletteTheme(Style.VORTEX,   0xE6D1FF, .10, 0,  .55, 10), // morado: vórtice de repetición
        new RouletteTheme(Style.SANCTION, 0xEAF6FF, .04, 0,  .10, 9),  // azul: sello geométrico de sanción
        new RouletteTheme(Style.MISSION,  0xE0FFFB, .04, 0,  .30, 12), // turquesa: misión activada
        new RouletteTheme(Style.BLESSING, 0xFFE9A8, .10, -10,.60, 14), // verde: bendición, hojas + oro
        new RouletteTheme(Style.UNLOCK,   0xFFFFFF, .06, -5, .55, 16), // amarillo: desbloqueo, candado
        new RouletteTheme(Style.INFERNO,  0xFFCC66, .35, 18, .35, 15), // naranja: dificultad, fuego
    };
    static RouletteTheme of(int paletteIndex) { return BY_INDEX[paletteIndex]; }
}
