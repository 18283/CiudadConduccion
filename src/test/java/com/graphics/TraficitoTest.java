package com.graphics; // Permite comprobar los métodos protegidos del tráfico desde el mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/**
 * Comprueba el tráfico autónomo de clase5 sin abrir una ventana OpenGL: los tres
 * autos recorren sus circuitos durante minutos simulados y deben permanecer en
 * las calles, dentro del mapa y sin solaparse con otro vehículo.
 */
public class TraficitoTest extends TestCase {

    private static final float PASO = 1f / 60f; // Duración de un cuadro, la misma que usa el ciclo de dibujo.
    private static final int CUADROS = 3600; // Un minuto de simulación.
    private static final int AUTOS = 3; // Número de vehículos autónomos que se crean.
    /*
     * La carrocería se mide con el largo y el ancho completos del modelo que dibuja clase2,
     * incluyendo los faros y las ruedas, que son las partes que más sobresalen: son las mismas
     * medidas que usa el tráfico para saber si dos vehículos se pisan, de modo que esta prueba
     * no perdona ni un centímetro de lo que la aplicación ya no perdona.
     */
    private static final float LARGO_AUTO = 2.72f; // Largo de la carrocería con sus faros.
    private static final float ANCHO_AUTO = 2f; // Ancho de la carrocería con sus ruedas.

    /** Comprueba que cada circuito está formado únicamente por calles transitables. */
    public void testCircuitosSobreCalles() {
        clase5 juego = new clase5(); // Crea el tráfico sin ejecutar run().
        assertEquals(AUTOS + 1, juego.VEHICULOS.length); // Exige un espejo del jugador y tres autos autónomos.
        for (int indice = 1; indice < juego.VEHICULOS.length; indice++) {
            clase5.AutoIA auto = juego.VEHICULOS[indice]; // Auto autónomo que se está comprobando.
            int esquinas = auto.ruta.length / 2 - 1; // El último par repite el primero, así que no es una esquina nueva.
            assertTrue(esquinas >= 4); // Exige al menos un circuito de cuatro esquinas.
            for (int esquina = 0; esquina < esquinas; esquina++) { // Recorre cada esquina del circuito.
                float desdeX = auto.ruta[esquina * 2]; // X de la esquina actual.
                float desdeZ = auto.ruta[esquina * 2 + 1]; // Z de la esquina actual.
                int siguiente = (esquina + 1) % esquinas; // La esquina que se visita después.
                float hastaX = auto.ruta[siguiente * 2]; // X de la esquina siguiente.
                float hastaZ = auto.ruta[siguiente * 2 + 1]; // Z de la esquina siguiente.
                assertTrue("esquina " + esquina + " bloqueada", juego.puedeCircular(desdeX, desdeZ)); // Exige que cada
                                                                                                          // esquina
                                                                                                          // esté
                                                                                                          // libre.
                for (float avance = 0.1f; avance < 1f; avance += 0.1f) { // Recorre el tramo entre esquinas.
                    float x = desdeX + (hastaX - desdeX) * avance; // Interpola la X dentro de la calle.
                    float z = desdeZ + (hastaZ - desdeZ) * avance; // Interpola la Z dentro de la calle.
                    assertTrue("calle bloqueada", juego.puedeCircular(x, z)); // Exige que todo el tramo sea transitable.
                    float derechaX = (float) Math.cos(anguloHacia(desdeX, desdeZ, hastaX, hastaZ)); // Costado derecho en X.
                    float derechaZ = -(float) Math.sin(anguloHacia(desdeX, desdeZ, hastaX, hastaZ)); // Costado derecho en Z.
                    assertTrue("carril derecho bloqueado", juego.puedeCircular(x + derechaX * juego.CARRIL,
                            z + derechaZ * juego.CARRIL)); // Exige que el carril por el que circula también sea calle.
                }
            }
        }
    }

    /** Comprueba que el tráfico avanza, respeta las calles y no se sale del mapa con el jugador estacionado. */
    public void testTraficoConJugadorEstacionado() {
        clase5 juego = new clase5(); // Crea el tráfico sin ejecutar run().
        juego.autoX = 50; // Estaciona al jugador en la calle x = 50 para obligar a los autos a rodearlo.
        juego.autoZ = 0; // Lo deja en el tramo recto que conecta dos esquinas.
        juego.angulo = 0; // Orientado hacia -Z, como cualquier vehículo parado.
        juego.velocidad = 0; // Detenido, sin marcha atrás.
        float[] recorridos = new float[AUTOS]; // Distancia acumulada por cada auto autónomo.
        float[] separaciones = new float[AUTOS]; // Menor hueco visto entre dos vehículos.
        for (int i = 0; i < AUTOS; i++) { // Todavía no se ha medido ningún hueco en esta prueba.
            separaciones[i] = Float.MAX_VALUE; // Un valor enorme para que el mínimo posterior sea el real.
        }
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) { // Simula un minuto completo.
            juego.actualizarTrafico(PASO); // Avanza el tráfico autónomo un cuadro.
            for (int i = 0; i < AUTOS; i++) { // Revisa cada auto autónomo.
                clase5.AutoIA auto = juego.VEHICULOS[i + 1]; // Auto que se está comprobando.
                assertTrue("el auto " + auto.id + " invade una manzana o sale del mapa",
                        juego.puedeCircular(auto.x, auto.z)); // Exige una posición siempre transitable.
                recorridos[i] += Math.abs(auto.velocidad) * PASO; // Suma el camino recorrido de este cuadro.
                assertTrue("el auto " + auto.id + " se sale de su circuito",
                        distanciaAlCircuito(auto) < 4.5f); // Exige que siga la calle, no las manzanas.
                assertEquals("objetivo fuera del circuito", 1, indiceValido(auto)); // Exige un objetivo real.
            }
            comprobarSolapes(juego, separaciones); // Ningún auto puede pisar a otro ni al jugador.
        }
        for (int i = 0; i < AUTOS; i++) { // Revisa el resultado de cada auto.
            assertTrue("el auto " + i + " apenas avanza", recorridos[i] > 150f); // Exige que circule sin quedarse
                                                                                 // atascado.
            assertTrue("dos vehículos acaban rozándose", separaciones[i] > 0f); // Exige un hueco real entre carros.
        }
    }

    /** Comprueba el tráfico con un jugador que recorre la misma calle en sentido contrario. */
    public void testTraficoConJugadorEnMarcha() {
        clase5 juego = new clase5(); // Crea el tráfico sin ejecutar run().
        boolean subiendo = true; // El jugador recorre la calle x = 50 de -30 a 30 y vuelve.
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) { // Simula un minuto con el jugador en movimiento.
            if (subiendo) { // Primera mitad del vaivén.
                juego.autoZ += 5 * PASO; // Sube por la calle a cinco unidades por segundo.
                juego.angulo = (float) Math.PI; // Con este ángulo el frente apunta hacia +Z.
                if (juego.autoZ > 30) { // Ya llegó al extremo norte de la calle.
                    subiendo = false; // Cambia de sentido en la siguiente iteración.
                }
            } else { // Segunda mitad del vaivén.
                juego.autoZ -= 5 * PASO; // Baja por la misma calle.
                juego.angulo = 0; // Con este ángulo el frente apunta hacia -Z.
                if (juego.autoZ < -30) { // Ya llegó al extremo sur de la calle.
                    subiendo = true; // Vuelve a subir en la siguiente iteración.
                }
            }
            juego.autoX = 50; // El jugador no abandona la calle vertical.
            juego.velocidad = 5; // Velocidad constante, sin Frenos ni aceleraciones del usuario.
            juego.actualizarTrafico(PASO); // Avanza el tráfico autónomo un cuadro.
            comprobarSolapes(juego, new float[AUTOS]); // Ningún auto puede pisar al jugador ni a otro auto.
            for (int i = 1; i < juego.VEHICULOS.length; i++) { // Revisa cada auto autónomo.
                assertTrue("el auto " + i + " se sale del mapa",
                        juego.puedeCircular(juego.VEHICULOS[i].x, juego.VEHICULOS[i].z)); // Exige una posición libre.
            }
        }
    }

    /** Comprueba que el reinicio devuelve los tres autos a su esquina de salida. */
    public void testReinicioDelTrafico() {
        clase5 juego = new clase5(); // Crea el tráfico sin ejecutar run().
        for (int cuadro = 0; cuadro < 600; cuadro++) { // Simula diez segundos de marcha.
            juego.actualizarTrafico(PASO); // Los autos se alejan de su punto de partida.
        }
        juego.reiniciar(); // Ejecuta el mismo reinicio que activa la tecla R.
        assertEquals(AUTOS + 1, juego.VEHICULOS.length); // El tráfico vuelve a tener tres autos.
        for (int indice = 1; indice < juego.VEHICULOS.length; indice++) {
            clase5.AutoIA auto = juego.VEHICULOS[indice]; // Auto que se está comprobando.
            assertTrue("el auto no vuelve a una esquina", juego.puedeCircular(auto.x, auto.z)); // Exige una esquina
                                                                                                // válida.
            assertEquals(auto.velocidadCrucero, auto.velocidad, 0.001f); // Exige que vuelva a circular en marcha.
            assertEquals(0f, auto.desvio, 0.001f); // Exige que arranque centrado en su calle.
        }
    }

    /**
     * Comprueba que esperar al jugador no es una espera eterna: con el jugador aparcado atravesado
     * en el carril del auto 1 ese auto tiene que frenar y quedarse quieto, pero en cuanto el
     * jugador arranca y se aleja los tres autos vuelven a recorrer sus circuitos a buen ritmo.
     */
    public void testElTraficoSeRecuperaCuandoElJugadorSeVa() {
        final int ESPERA = 2400; // Cuarenta segundos con el jugador atravesado en el carril del auto 1.
        final int PARADA = 300; // Los últimos cinco segundos de esa espera, cuando ya nada debería cambiar.
        final int MARCHA = 2400; // Cuarenta segundos con el jugador alejándose por su misma calle.
        clase5 juego = new clase5(); // Crea el tráfico sin ejecutar run().
        juego.autoX = 10; // Estaciona al jugador en la calle z = 30, la que usa el auto 1.
        juego.autoZ = 27.6f; // El carril derecho de esa calle está a CARRIL unidades de su eje.
        juego.angulo = (float) Math.PI; // Con este ángulo el frente apunta hacia +Z, es decir de costado.
        juego.velocidad = 0; // Detenido, sin marcha atrás.
        float[] caminos = new float[AUTOS * 3]; // Camino en espera, en parada y ya en marcha, auto por auto.
        float[] anteriorX = new float[AUTOS]; // Posición anterior de cada auto, para medir de verdad.
        float[] anteriorZ = new float[AUTOS]; // La misma posición anterior en profundidad.
        for (int cuadro = 0; cuadro < ESPERA + MARCHA; cuadro++) { // Simula ochenta segundos de tráfico.
            if (cuadro == ESPERA) { // El jugador por fin arranca, así que se empieza a medir la recuperación.
                for (int i = 0; i < AUTOS; i++) { // Recoge la posición de partida de cada auto autónomo.
                    anteriorX[i] = juego.VEHICULOS[i + 1].x; // X en el momento en que arranca el jugador.
                    anteriorZ[i] = juego.VEHICULOS[i + 1].z; // Profundidad en ese mismo momento.
                }
            }
            if (cuadro >= ESPERA) { // Segunda mitad: el jugador recorre su calle hacia -X a seis unidades por segundo.
                juego.autoX -= 6 * PASO; // Sale de la ciudad por el extremo opuesto al que approached el auto.
                juego.angulo = (float) Math.PI / 2; // Con este ángulo el frente del jugador apunta hacia -X.
                juego.velocidad = 6; // Velocidad constante, sin frenos ni aceleraciones del usuario.
            }
            juego.actualizarTrafico(PASO); // Avanza el tráfico autónomo un cuadro.
            for (int i = 0; i < AUTOS; i++) { // Revisa cada auto autónomo del cuadro nuevo.
                clase5.AutoIA auto = juego.VEHICULOS[i + 1]; // Auto que se está comprobando.
                assertTrue("el auto " + auto.id + " invade una manzana o sale del mapa",
                        juego.puedeCircular(auto.x, auto.z)); // Exige una posición siempre transitable.
                assertTrue("el auto " + auto.id + " se sale de su circuito", distanciaAlCircuito(auto) < 4.5f); // Exige
                                                                                                                    // que siga
                                                                                                                    // su
                                                                                                                    // calle.
                assertEquals("objetivo fuera del circuito", 1, indiceValido(auto)); // Exige un objetivo real.
                float movidoX = auto.x - anteriorX[i]; // Desplazamiento horizontal real de este cuadro.
                float movidoZ = auto.z - anteriorZ[i]; // El mismo desplazamiento medido en profundidad.
                int tramo = cuadro < ESPERA - PARADA ? i : cuadro < ESPERA ? i + AUTOS : i + AUTOS * 2; // Ventana medida.
                caminos[tramo] += (float) Math.sqrt(movidoX * movidoX + movidoZ * movidoZ); // Suma el camino real.
                anteriorX[i] = auto.x; // Guarda la posición para el siguiente cuadro.
                anteriorZ[i] = auto.z; // Guarda también la profundidad.
            }
            comprobarSolapes(juego, new float[AUTOS]); // Ningún auto puede pisar a otro ni al jugador.
        }
        assertTrue("el auto 1 no llegó a acercarse al jugador: " + caminos[0], caminos[0] > 20f); // Tuvo que frenarse.
        assertTrue("el auto 1 no respetó la espera: " + caminos[AUTOS], caminos[AUTOS] < 3f); // Se quedó parado detrás.
        assertTrue("el auto 2 se detuvo sin motivo: " + caminos[1], caminos[1] > 100f); // Los otros no se ven afectados.
        assertTrue("el auto 3 se detuvo sin motivo: " + caminos[2], caminos[2] > 100f); // Tampoco el del centro.
        for (int i = 0; i < AUTOS; i++) { // Revisa que los tres autos vuelvan a circular de verdad.
            assertTrue("el auto " + (i + 1) + " no recupera el ritmo al irse el jugador: " + caminos[i + AUTOS * 2],
                    caminos[i + AUTOS * 2] > 100f); // Exige un camino largo medido con su posición real.
        }
    }

    /** Devuelve 1 si el objetivo del circuito es válido y 0 si el índice se salió del arreglo. */
    private int indiceValido(clase5.AutoIA auto) {
        int esquinas = auto.ruta.length / 2 - 1;
        if (auto.indiceRuta < 0 || auto.indiceRuta >= esquinas) {
            return 0;
        }
        return 1;
    }

/** Comprueba que ningún vehículo solape a otro y anota la mayor cercanía vista. */
    private void comprobarSolapes(clase5 juego, float[] separaciones) {
        for (int i = 0; i < juego.VEHICULOS.length; i++) { // Recorre todos los vehículos, incluido el jugador.
            for (int j = i + 1; j < juego.VEHICULOS.length; j++) { // Compara cada pareja una sola vez.
                clase5.Vehiculo a = juego.VEHICULOS[i]; // Primer vehículo de la pareja.
                clase5.Vehiculo b = juego.VEHICULOS[j]; // Segundo vehículo de la pareja.
                float haciaX = b.x - a.x; // Separación horizontal entre centros.
                float haciaZ = b.z - a.z; // Separación en profundidad entre centros.
                float hueco = separacion(a, b, haciaX, haciaZ); // Espacio libre real entre las dos carrocerías.
                if (j - 1 < separaciones.length) { // Solo se anotan las separaciones entre los vehículos.
                    separaciones[j - 1] = Math.min(separaciones[j - 1], hueco); // Guarda el menor hueco visto.
                }
                assertTrue("los vehículos " + i + " y " + j + " se solapan", hueco > 0f); // Exige separación real.
            }
        }
    }

    /**
     * Espacio libre entre dos carrocerías rectangulares con el teorema del eje separador:
     * las dos cajas quedan separadas si en alguno de los cuatro ejes, el de frente y el de
     * costado de cada auto, sus proyecciones no se pisan. Basta un solo eje para que los
     * vehículos no se toquen, así que se devuelve el mayor hueco de los cuatro: si sale
     * negativo es que las carrocerías se solapan de verdad.
     */
    private float separacion(clase5.Vehiculo a, clase5.Vehiculo b, float haciaX, float haciaZ) {
        float[][] ejes = { // Los cuatro ejes que hay que comprobar.
                { -(float) Math.sin(a.angulo), -(float) Math.cos(a.angulo) }, // Frente del primer vehículo.
                { (float) Math.cos(a.angulo), -(float) Math.sin(a.angulo) }, // Costado del primer vehículo.
                { -(float) Math.sin(b.angulo), -(float) Math.cos(b.angulo) }, // Frente del segundo vehículo.
                { (float) Math.cos(b.angulo), -(float) Math.sin(b.angulo) } // Costado del segundo vehículo.
        };
        float mejor = -Float.MAX_VALUE; // Empieza sin ninguna medición.
        for (int indice = 0; indice < ejes.length; indice++) { // Recorre los cuatro ejes.
            float ejeX = ejes[indice][0]; // Componente X del eje que se está midiendo.
            float ejeZ = ejes[indice][1]; // Componente Z del mismo eje.
            float distancia = Math.abs(haciaX * ejeX + haciaZ * ejeZ); // Distancia entre centros en ese eje.
            float alcance = alcance(a.angulo, ejeX, ejeZ) + alcance(b.angulo, ejeX, ejeZ); // Lo que ocupan las cajas.
            mejor = Math.max(mejor, distancia - alcance); // El eje que más separa es el que decide el contacto.
        }
        return mejor; // Positivo si las carrocerías no se tocan.
    }

    /** Distancia a la que el centro de un vehículo se sale ya de su propia carrocería, en un eje dado. */
    private float alcance(float angulo, float ejeX, float ejeZ) {
        float frenteX = -(float) Math.sin(angulo); // Frente del vehículo en X.
        float frenteZ = -(float) Math.cos(angulo); // Frente del vehículo en Z.
        float costadoX = (float) Math.cos(angulo); // Costado derecho del vehículo en X.
        float costadoZ = -(float) Math.sin(angulo); // Costado derecho del vehículo en Z.
        float largo = LARGO_AUTO / 2 * Math.abs(frenteX * ejeX + frenteZ * ejeZ); // Largo proyectado sobre el eje.
        float ancho = ANCHO_AUTO / 2 * Math.abs(costadoX * ejeX + costadoZ * ejeZ); // Ancho proyectado sobre el eje.
        return largo + ancho; // Es el radio de la carrocería medido sobre ese eje.
    }

    /** Distancia más corta entre el auto y cualquier tramo de su propio circuito. */
    private float distanciaAlCircuito(clase5.AutoIA auto) {
        int esquinas = auto.ruta.length / 2 - 1; // El último par repite el primero.
        float mejor = Float.MAX_VALUE; // Empieza sin ninguna medición.
        for (int esquina = 0; esquina < esquinas; esquina++) { // Recorre cada tramo del circuito.
            float desdeX = auto.ruta[esquina * 2]; // X del origen del tramo.
            float desdeZ = auto.ruta[esquina * 2 + 1]; // Z del origen del tramo.
            int siguiente = (esquina + 1) % esquinas; // La esquina que cierra el tramo.
            float hastaX = auto.ruta[siguiente * 2]; // X del final del tramo.
            float hastaZ = auto.ruta[siguiente * 2 + 1]; // Z del final del tramo.
            float dx = hastaX - desdeX; // Longitud del tramo en X.
            float dz = hastaZ - desdeZ; // Longitud del tramo en Z.
            float largo = dx * dx + dz * dz; // Longitud al cuadrado del tramo.
            float t = 0; // Empieza en el origen del tramo.
            if (largo > 0) { // Un tramo de longitud cero no tiene dirección definida.
                t = ((auto.x - desdeX) * dx + (auto.z - desdeZ) * dz) / largo; // Proyecta el auto sobre el tramo.
                t = Math.max(0f, Math.min(1f, t)); // Limita la proyección al tramo, sin extenderse a los lados.
            }
            float cercaX = desdeX + dx * t; // Punto del tramo más cercano en X.
            float cercaZ = desdeZ + dz * t; // Punto del tramo más cercano en Z.
            float distancia = (float) Math.sqrt((auto.x - cercaX) * (auto.x - cercaX) + (auto.z - cercaZ) * (auto.z - cercaZ)); // Distancia
                                                                                                                                    // real
                                                                                                                                    // al
                                                                                                                                    // tramo.
            mejor = Math.min(mejor, distancia); // Conserva la menor de todas las distancias.
        }
        return mejor; // Devuelve la distancia al eje del circuito.
    }

    /** Ángulo con el que un vehículo debe mirar de un punto a otro, como en clase5. */
    private float anguloHacia(float desdeX, float desdeZ, float hastaX, float hastaZ) {
        return (float) Math.atan2(-(hastaX - desdeX), -(hastaZ - desdeZ)); // Invierte el vector y usa su dirección.
    }
}
