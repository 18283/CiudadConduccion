package com.graphics; // Permite probar los métodos protegidos del juego desde el mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las reglas de conducción sin abrir una ventana OpenGL. */
public class ConduccionTest extends TestCase {

    /** Comprueba que las calles del recorrido conectan el inicio y las entregas. */
    public void testCallesYDestinosTransitables() {
        clase2 juego = new clase2(); // Crea el estado del juego sin ejecutar run().
        for (float z = -30; z <= 30; z += 0.25f) { // Recorre posiciones a lo largo de la calle oeste.
            assertTrue(juego.puedeCircular(-30, z)); // Exige que cada punto del tramo sea transitable.
        }
        for (float x = -30; x <= 30; x += 0.25f) { // Recorre la avenida del borde norte.
            assertTrue(juego.puedeCircular(x, -30)); // Comprueba la conexión entre las dos primeras entregas.
        }
        assertTrue(juego.puedeCircular(-10, -10)); // Verifica que un cruce interior permite circular.
        assertFalse(juego.puedeCircular(0, 0)); // Verifica que la manzana del origen bloquea al vehículo.
    }

    /** Comprueba edificios, parques, aceras y bordes del mapa. */
    public void testObstaculosYLimites() {
        clase2 juego = new clase2(); // Crea un vehículo con su radio de colisión inicial.
        assertFalse(juego.puedeCircular(-20, -20)); // Rechaza el centro de un edificio.
        assertFalse(juego.puedeCircular(20, -20)); // Rechaza el centro de un parque.
        assertFalse(juego.puedeCircular(-25.5f, -20)); // Rechaza una posición cuyo círculo invade la acera.
        assertFalse(juego.puedeCircular(34, 0)); // Rechaza una posición demasiado cercana al borde derecho.
        assertFalse(juego.puedeCircular(0, -36)); // Rechaza una posición exterior al borde norte.
        assertTrue(juego.puedeCircular(-27, -20)); // Acepta una posición con separación suficiente de la acera.
    }

    /** Comprueba que el reinicio restaura la posición y detiene el vehículo. */
    public void testReinicio() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        juego.autoX = 12; // Simula que el auto se desplazó horizontalmente.
        juego.autoZ = 4; // Simula un desplazamiento en profundidad.
        juego.velocidad = 10; // Simula que el auto está avanzando.
        juego.angulo = 2; // Simula que el vehículo cambió de orientación.
        juego.reiniciar(); // Ejecuta el mismo reinicio que se activa con R.
        assertEquals(-30f, juego.autoX, 0f); // Exige recuperar la coordenada X inicial.
        assertEquals(30f, juego.autoZ, 0f); // Exige recuperar la coordenada Z inicial.
        assertEquals(0f, juego.velocidad, 0f); // Exige que el vehículo quede detenido.
        assertEquals(0f, juego.angulo, 0f); // Exige recuperar la dirección frontal inicial.
    }

    /**
     * Comprueba que el reloj de la ciudad y el cronómetro de las entregas son dos relojes
     * distintos. El primero nunca se detiene porque es el que cambia el color de los semáforos,
     * así que tiene que seguir corriendo aunque la partida ya esté ganada; el segundo se
     * congela al entregar la última parada, que es lo que se anuncia en el título.
     */
    public void testElRelojSigueCorriendoDespuesDeGanar() {
        final float PASO = 1f / 60f; // Duración de un cuadro, la misma que usa el ciclo de dibujo.
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        for (int cuadro = 0; cuadro < 600; cuadro++) { // Simula diez segundos antes de entregar nada.
            juego.avanzarReloj(PASO); // Mueve el reloj de la ciudad cuadro a cuadro.
        }
        assertTrue("el reloj de la ciudad no avanza con la partida en curso", juego.tiempo > 9f); // Exige que sume
                                                                                                  // segundos.
        juego.autoX = 50; // Coloca al jugador sobre la marca de la primera entrega.
        juego.autoZ = -50; // La primera parada está en la esquina sureste del mapa.
        juego.velocidad = 0; // Exige llegar frenado para que la entrega cuente.
        juego.avanzarReloj(PASO); // Completa la primera entrega.
        juego.autoX = -50; // Coloca al jugador sobre la marca de la segunda entrega.
        juego.autoZ = -50; // La segunda parada está en la esquina suroeste del mapa.
        juego.avanzarReloj(PASO); // Completa la segunda entrega.
        juego.autoX = -50; // Coloca al jugador sobre la marca de la tercera entrega.
        juego.autoZ = 50; // La tercera parada está en la esquina noroeste del mapa.
        juego.avanzarReloj(PASO); // Completa la tercera entrega y gana la partida.
        float cronometro = juego.tiempoFinal; // El cronómetro queda congelado en el tiempo de la victoria.
        assertTrue("la partida no se completó: " + juego.estadoExtra(), cronometro > 0f); // Exige un tiempo final real.
        assertTrue(juego.estadoExtra().indexOf("GANASTE") >= 0); // Exige el anuncio de victoria en el título.
        for (int cuadro = 0; cuadro < 3600; cuadro++) { // Simula un minuto más con la partida ya terminada.
            juego.avanzarReloj(PASO); // El reloj de la ciudad debe seguir moviéndose.
        }
        assertTrue("el reloj de la ciudad se detuvo al ganar: " + juego.tiempo,
                juego.tiempo > cronometro + 50f); // Exige que los semáforos sigan cambiando de color.
        assertEquals("el cronómetro siguió corriendo después de ganar", cronometro, juego.tiempoFinal, 0f); // Exige que
                                                                                                          // siga
                                                                                                          // congelado.
        assertTrue("el título no conserva el tiempo final",
                juego.estadoExtra().indexOf("GANASTE en " + (int) cronometro + " s") >= 0); // Exige el tiempo empleado.
    }
}
