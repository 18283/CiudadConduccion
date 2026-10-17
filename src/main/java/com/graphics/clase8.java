//añadiendo personas

package com.graphics; // Sigue la serie junto a las siete clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Tecla P para pausar a los peatones.

/**
 * CLASE 8: PEATONES CAMINANDO.
 * Hereda clase7, que hereda clase6, clase5, clase4, clase3, clase2 y clase1;
 * por eso reutiliza la ciudad, la conduccion, la iluminacion, las entregas,
 * el minimapa, el trafico, el panel y el menu sin copiarlos.
 * Anade tres personas que van de un lugar a otro: cada una recorre un
 * circuito largo por varias manzanas, con tramos de acera y cruces de calle
 * por los pasos peatonales, y con balanceo de piernas y brazos para simular
 * el paso.
 * Antes de avanzar miran a los autos (el rojo del jugador y los blancos del
 * trafico): si un auto esta encima o viene muy cerca del siguiente paso, la
 * persona espera quieta hasta que se despeje, sobre todo al cruzar.
 * Orden de lectura: estado, rutas, controles, actualizacion, dibujo y punto
 * de entrada.
 */
public class clase8 extends clase7 {

    // ==================== 1. ESTADO DE CADA PERSONA ====================

    /** Estado minimo de un peaton: donde esta, hacia donde mira y como camina. */
    protected static class Persona {
        protected float x = 0; // Posicion horizontal en el mundo.
        protected float z = 0; // Posicion en profundidad en el mundo.
        protected float angulo = 0; // Orientacion en radianes; cero mira hacia -Z.
        protected float velocidad = 2; // Unidades por segundo al caminar.
        protected float fase = 0; // Paso de la caminata; mueve piernas y brazos.
        protected float[] ruta = new float[0]; // Pares X,Z con las esquinas del recorrido.
        protected int indiceRuta = 1; // Esquina que se busca ahora; avanza al llegar.
        protected float torsoR = 0.2f; // Color de la camisa: componente roja.
        protected float torsoG = 0.4f; // Componente verde de la camisa.
        protected float torsoB = 0.9f; // Componente azul de la camisa.
    }

    protected Persona[] personas = crearPersonas(); // Las tres personas caminando.
    protected boolean peatonesActivos = true; // Permite pausar y reanudar con la tecla P.

    private static final float LLEGADA_PERSONA = 0.4f; // Distancia a la esquina que cuenta como llegada.
    private static final float FRECUENCIA_PASO = 6.0f; // Oscilaciones del paso por unidad recorrida.
    private static final float RADIO_ACERA = 2.8f; // Hueco que se pide a los autos al caminar por la acera.
    private static final float RADIO_CRUCE = 5.0f; // Hueco mayor para entrar a la calle: se cruza con mas margen.
    private static final float RADIO_PASO = 2.0f; // Ya cruzando solo se frena si el auto va a tocar a la persona.

    // ==================== 2. RUTAS DE UN LUGAR A OTRO ====================

    /*
     * Las aceras rodean cada manzana a 4.2 unidades de su centro (la manzana
     * mide 10 y el edificio 7, asi que 4.2 pisa la acera sin meterse al
     * edificio). Las calles miden 10 entre manzana y manzana; el punto medio
     * del cruce (por ejemplo x = -10 o z = 10) es el paso peatonal.
     * Cada ruta va por la acera norte hasta el otro extremo, cruza las calles
     * que encuentra, y vuelve por la acera sur: asi visita varias manzanas en
     * vez de quedarse en una sola. El ultimo par repite el primero para cerrar
     * el circuito.
     */

    /**
     * Ruta norte: recorre tres manzanas de la fila z = -20, de oeste a este y de
     * vuelta.
     */
    private static float[] rutaNorte() {
        return new float[] {
                -24.2f, -24.2f, // Acera norte de la manzana oeste.
                -15.8f, -24.2f, // Esquina este de esa acera.
                -10.0f, -24.2f, // Cruce de la calle x = -10.
                -4.2f, -24.2f, // Acera norte de la manzana central.
                4.2f, -24.2f, // Otro extremo de la misma acera.
                10.0f, -24.2f, // Cruce de la calle x = 10.
                15.8f, -24.2f, // Acera norte de la manzana este.
                24.2f, -24.2f, // Extremo este del recorrido.
                24.2f, -15.8f, // Baja por el costado este.
                15.8f, -15.8f, // Acera sur de la manzana este.
                10.0f, -15.8f, // Cruce de vuelta.
                4.2f, -15.8f, // Acera sur de la manzana central.
                -4.2f, -15.8f, // Otro extremo de la misma acera.
                -10.0f, -15.8f, // Cruce de vuelta.
                -15.8f, -15.8f, // Acera sur de la manzana oeste.
                -24.2f, -15.8f, // Extremo oeste del recorrido.
                -24.2f, -24.2f // Cierra el circuito subiendo por el costado oeste.
        };
    }

    /** Ruta sur: igual que la norte pero en la fila z = 20, con otros bloques. */
    private static float[] rutaSur() {
        return new float[] {
                -24.2f, 15.8f, // Acera norte de la manzana oeste.
                -15.8f, 15.8f, // Esquina este de esa acera.
                -10.0f, 15.8f, // Cruce de la calle x = -10.
                -4.2f, 15.8f, // Acera norte de la manzana central.
                4.2f, 15.8f, // Otro extremo de la misma acera.
                10.0f, 15.8f, // Cruce de la calle x = 10.
                15.8f, 15.8f, // Acera norte de la manzana este.
                24.2f, 15.8f, // Extremo este del recorrido.
                24.2f, 24.2f, // Baja por el costado este.
                15.8f, 24.2f, // Acera sur de la manzana este.
                10.0f, 24.2f, // Cruce de vuelta.
                4.2f, 24.2f, // Acera sur de la manzana central.
                -4.2f, 24.2f, // Otro extremo de la misma acera.
                -10.0f, 24.2f, // Cruce de vuelta.
                -15.8f, 24.2f, // Acera sur de la manzana oeste.
                -24.2f, 24.2f, // Extremo oeste del recorrido.
                -24.2f, 15.8f // Cierra el circuito subiendo por el costado oeste.
        };
    }

    /**
     * Ruta centro: baja por la columna x = 0 desde el norte hasta el sur y vuelve.
     */
    private static float[] rutaCentro() {
        return new float[] {
                -4.2f, -24.2f, // Acera oeste de la manzana norte.
                -4.2f, -15.8f, // Otro extremo de la misma acera.
                -4.2f, -10.0f, // Cruce de la calle z = -10.
                -4.2f, -4.2f, // Acera oeste de la manzana central.
                -4.2f, 4.2f, // Otro extremo de la misma acera.
                -4.2f, 10.0f, // Cruce de la calle z = 10.
                -4.2f, 15.8f, // Acera oeste de la manzana sur.
                -4.2f, 24.2f, // Extremo sur del recorrido.
                4.2f, 24.2f, // Cruza al costado este.
                4.2f, 15.8f, // Acera este de la manzana sur.
                4.2f, 10.0f, // Cruce de vuelta.
                4.2f, 4.2f, // Acera este de la manzana central.
                4.2f, -4.2f, // Otro extremo de la misma acera.
                4.2f, -10.0f, // Cruce de vuelta.
                4.2f, -15.8f, // Acera este de la manzana norte.
                4.2f, -24.2f, // Extremo norte del recorrido.
                -4.2f, -24.2f // Cierra el circuito volviendo al costado oeste.
        };
    }

    /**
     * Crea las tres personas, cada una con su ruta larga y camisa de otro
     * color para distinguirlas.
     */
    protected Persona[] crearPersonas() {
        Persona primera = nuevaPersona(rutaNorte(), 2.2f, 0.20f, 0.45f, 0.90f); // Camisa azul, recorre el norte.
        Persona segunda = nuevaPersona(rutaSur(), 1.7f, 0.90f, 0.30f, 0.20f); // Camisa naranja, recorre el sur.
        Persona tercera = nuevaPersona(rutaCentro(), 2.6f, 0.25f, 0.70f, 0.30f); // Camisa verde, baja por el centro.
        return new Persona[] { primera, segunda, tercera }; // La lista completa de peatones.
    }

    /**
     * Coloca una persona en el primer punto de su ruta, ya mirando al siguiente.
     */
    private Persona nuevaPersona(float[] ruta, float velocidad, float r, float g, float b) {
        Persona persona = new Persona(); // Peaton vacio que se completa con su recorrido.
        persona.ruta = ruta; // El circuito largo que visitara siempre.
        persona.x = ruta[0]; // Parte del primer punto en X.
        persona.z = ruta[1]; // Parte del primer punto en Z.
        persona.indiceRuta = 1; // Su primer objetivo es el segundo punto.
        persona.velocidad = velocidad; // Ritmo propio para que no caminen igual.
        persona.angulo = anguloHacia(persona.x, persona.z, ruta[2], ruta[3]); // Mira a su objetivo.
        persona.torsoR = r; // Guarda el color de su camisa.
        persona.torsoG = g;
        persona.torsoB = b;
        return persona; // Devuelve el peaton listo para caminar.
    }

    // ==================== 2. CONTROLES, REINICIO E INDICADORES
    // ====================

    /** Anade la tecla P para pausar o reanudar a los peatones. */
    @Override // Amplia las teclas de clase7 (W, ESC, C, R, N, F, M y T).
    protected void tecla(int key) {
        super.tecla(key); // Conserva menu, camara, reinicio, luces, mapa y trafico.
        if (key == GLFW_KEY_P) { // Comprueba si se pulso la tecla de peatones.
            peatonesActivos = !peatonesActivos; // Pausa o reanuda a las tres personas.
        }
    }

    /** Devuelve tambien a las tres personas a su esquina de salida. */
    @Override // Amplia el reinicio de clase5/clase4/clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura auto, entregas, trafico y cronometro.
        personas = crearPersonas(); // Recrea los recorridos desde su esquina inicial.
    }

    /** Anade el estado de los peatones al titulo de la ventana. */
    @Override // Amplia los indicadores de clase5 (trafico) y anteriores.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera luces, mapa, entregas y trafico.
        mensaje += "P: peatones " + (peatonesActivos ? "ON" : "OFF"); // Muestra la tecla y el estado.
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 4. CUIDADO CON LOS AUTOS ====================

    /*
     * La persona solo avanza si el paso esta libre: se mira el auto rojo del
     * jugador (autoX, autoZ) y los blancos del trafico (VEHICULOS[1..]). En la
     * acera basta un hueco chico; al pisar o entrar a la calle se pide un
     * hueco grande, porque el auto viene rapido y el peaton va despacio. Si el
     * paso esta ocupado, la persona espera quieta mirando su objetivo.
     */

    /** Dice si un punto del mundo cae sobre la calle (celda 0 del MAPA). */
    private boolean esCalle(float x, float z) {
        int columna = (int) Math.floor((x + LIMITE) / CELDA); // Columna de la celda que pisa.
        int fila = (int) Math.floor((z + LIMITE) / CELDA); // Fila de la celda que pisa.
        if (fila < 0 || fila >= MAPA.length || columna < 0 || columna >= MAPA[0].length) {
            return true; // Fuera del mapa se trata como calle por precaucion.
        }
        return MAPA[fila][columna] == 0; // Solo el cero es calle; lo demas es manzana.
    }

    /** Distancia al cuadrado entre dos puntos, sin raiz para comparar rapido. */
    private static float distanciaCuadrada(float ax, float az, float bx, float bz) {
        float dx = ax - bx; // Separacion horizontal.
        float dz = az - bz; // Separacion en profundidad.
        return dx * dx + dz * dz; // Pitagoras sin raiz.
    }

    /**
     * Dice si la persona debe esperar: hay un auto encima o tan cerca del
     * siguiente paso que lo alcanzaria. En la acera no se acerca a los autos;
     * para entrar a la calle pide un hueco grande y espera en la acera; una
     * vez cruzando sigue hasta la otra acera y solo frena si el auto va a
     * tocarla, para no quedarse parada a mitad de la calle.
     */
    private boolean pasoBloqueado(float x, float z, float sigX, float sigZ) {
        boolean enCalle = esCalle(x, z); // La persona ya esta pisando la calle.
        boolean entraCalle = esCalle(sigX, sigZ); // El siguiente paso pisa la calle.
        if (bloqueaAuto(autoX, autoZ, x, z, sigX, sigZ, enCalle, entraCalle)) { // El rojo estorba.
            return true; // Espera a que pase.
        }
        for (int indice = 1; indice < VEHICULOS.length; indice++) { // Recorre solo el trafico autonomo.
            if (bloqueaAuto(VEHICULOS[indice].x, VEHICULOS[indice].z, x, z, sigX, sigZ, enCalle, entraCalle)) {
                return true; // El blanco estorba: espera a que pase.
            }
        }
        return false; // Ningun auto estorba: se puede caminar.
    }

    /** Decide si un auto concreto obliga a esperar segun donde pisa la persona. */
    private boolean bloqueaAuto(float autoX, float autoZ, float x, float z, float sigX, float sigZ, boolean enCalle,
            boolean entraCalle) {
        if (distanciaCuadrada(x, z, autoX, autoZ) < RADIO_ACERA * RADIO_ACERA) { // El auto esta encima.
            return true; // No se camina dentro de un auto.
        }
        if (!enCalle && entraCalle) { // Quiere entrar a la calle desde la acera.
            return distanciaCuadrada(sigX, sigZ, autoX, autoZ) < RADIO_CRUCE * RADIO_CRUCE; // Cruza solo con hueco
                                                                                            // grande.
        }
        if (!enCalle) { // Camina por la acera.
            return distanciaCuadrada(sigX, sigZ, autoX, autoZ) < RADIO_ACERA * RADIO_ACERA; // No se acerca al auto.
        }
        return distanciaCuadrada(sigX, sigZ, autoX, autoZ) < RADIO_PASO * RADIO_PASO; // Cruzando: sigue salvo toque
                                                                                      // inminente.
    }

    // ==================== 5. UN CUADRO DE PEATONES ====================

    /** Actualiza el juego heredado y despues a las tres personas. */
    @Override // Amplia la actualizacion de clase7/clase5/clase4.
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Procesa menu, conduccion, entregas, trafico y titulo.
        actualizarPersonas(deltaTime); // Mueve a los peatones con el mismo paso de tiempo.
    }

    /**
     * Avanza las tres personas un cuadro. Se declara protegido para que las
     * pruebas puedan simular caminatas sin abrir una ventana OpenGL.
     * Camina incluso con el menu abierto para que el fondo se vea vivo.
     */
    protected void actualizarPersonas(float deltaTime) {
        if (!peatonesActivos) { // Con los peatones pausados se quedan quietos.
            return; // Conserva la ultima posicion de cada uno.
        }
        for (Persona persona : personas) { // Recorre las tres personas.
            avanzarPersona(persona, deltaTime); // Mueve posicion, orientacion y paso.
        }
    }

    /**
     * Mueve una persona hacia su objetivo; espera quieta si un auto ocupa el paso.
     */
    private void avanzarPersona(Persona persona, float deltaTime) {
        int puntos = persona.ruta.length / 2; // Puntos del recorrido; el ultimo repite el primero.
        float destinoX = persona.ruta[persona.indiceRuta * 2]; // X del punto buscado.
        float destinoZ = persona.ruta[persona.indiceRuta * 2 + 1]; // Z del punto buscado.
        float haciaX = destinoX - persona.x; // Separacion horizontal hasta el objetivo.
        float haciaZ = destinoZ - persona.z; // Separacion en profundidad hasta el objetivo.
        float distancia = (float) Math.sqrt(haciaX * haciaX + haciaZ * haciaZ); // Distancia real.
        if (distancia < LLEGADA_PERSONA) { // Ya esta dentro del radio de llegada.
            persona.indiceRuta++; // Salta al siguiente punto del recorrido.
            if (persona.indiceRuta >= puntos) { // Se alcanzo el cierre, que es la salida.
                persona.indiceRuta = 1; // Vuelve al segundo punto y sigue el circuito.
            }
            return; // Este cuadro solo cambia de objetivo, sin desplazarse.
        }
        persona.angulo = anguloHacia(persona.x, persona.z, destinoX, destinoZ); // Mira al objetivo.
        float frenteX = componenteFrenteX(persona.angulo); // Frente de la persona en X.
        float frenteZ = componenteFrenteZ(persona.angulo); // El mismo frente en profundidad.
        float paso = Math.min(persona.velocidad * deltaTime, distancia); // No se pasa del punto.
        float sigX = persona.x + frenteX * paso; // Siguiente posicion horizontal.
        float sigZ = persona.z + frenteZ * paso; // Siguiente posicion en profundidad.
        if (pasoBloqueado(persona.x, persona.z, sigX, sigZ)) { // Un auto ocupa el paso.
            return; // Espera quieta, sin mover fase para que no patine en el sitio.
        }
        persona.x = sigX; // Acepta el desplazamiento horizontal.
        persona.z = sigZ; // Acepta el desplazamiento en profundidad.
        persona.fase += paso * FRECUENCIA_PASO; // El balanceo depende de lo caminado.
    }

    // ==================== 6. DIBUJO DE LAS PERSONAS ====================

    /** Anade las tres personas a la ciudad y sus marcas al minimapa. */
    @Override // Amplia el dibujo acumulado de las siete etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja ciudad, auto, farolas, decoracion, destino y trafico.
        for (Persona persona : personas) { // Recorre las tres personas.
            dibujarPersona(persona); // Anade una figura con su posicion y color.
        }
        if (vistaMapa) { // El minimapa necesita marcas altas para verse sobre los edificios.
            dibujarMarcasPersonas(); // Coloca un punto por cada peaton.
        }
    }

    /**
     * Construye una persona con cajas: dos piernas que se alternan, torso de
     * color propio, dos brazos que se balancean al contrario y cabeza.
     * Reutiliza piezaEn() de clase2, que coloca cada pieza segun la posicion y
     * la orientacion de la persona.
     */
    private void dibujarPersona(Persona persona) {
        float balanceo = (float) Math.sin(persona.fase) * 0.25f; // Desplazamiento del paso; piernas opuestas.
        // Piernas: pantalon oscuro, una adelante y otra atras segun el paso.
        piezaEn(persona.x, persona.z, persona.angulo, -0.15f, 0.45f, balanceo, 0.22f, 0.9f, 0.22f, 0.16f, 0.18f, 0.25f);
        piezaEn(persona.x, persona.z, persona.angulo, 0.15f, 0.45f, -balanceo, 0.22f, 0.9f, 0.22f, 0.16f, 0.18f, 0.25f);
        // Torso: camisa del color propio de cada persona.
        piezaEn(persona.x, persona.z, persona.angulo, 0, 1.2f, 0, 0.55f, 0.7f, 0.32f, persona.torsoR, persona.torsoG,
                persona.torsoB);
        // Brazos: al contrario de las piernas del mismo lado.
        piezaEn(persona.x, persona.z, persona.angulo, -0.38f, 1.2f, -balanceo, 0.15f, 0.6f, 0.15f,
                persona.torsoR * 0.8f, persona.torsoG * 0.8f, persona.torsoB * 0.8f);
        piezaEn(persona.x, persona.z, persona.angulo, 0.38f, 1.2f, balanceo, 0.15f, 0.6f, 0.15f, persona.torsoR * 0.8f,
                persona.torsoG * 0.8f, persona.torsoB * 0.8f);
        // Cabeza: tono piel.
        piezaEn(persona.x, persona.z, persona.angulo, 0, 1.78f, 0, 0.35f, 0.35f, 0.35f, 0.95f, 0.76f, 0.60f);
    }

    /** Dibuja un punto por cada peaton para localizarlos en el minimapa. */
    private void dibujarMarcasPersonas() {
        for (Persona persona : personas) { // Recorre las tres personas.
            caja(persona.x, 24, persona.z, 0.9f, 0.1f, 0.9f, 1, 0.5f, 0.2f); // Punto naranja sobre la acera.
        }
    }

    /** Punto de entrada de la octava leccion. */
    public static void main(String[] args) {
        clase8 aplicacion = new clase8(); // Crea la version con tres peatones.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicacion.
    }
}
