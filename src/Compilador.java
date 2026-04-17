// Compilador.java - Punto de entrada principal del compilador MiLeng
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Compilador {

    public static void main(String[] args) throws Exception {
        String archivoFuente;
        if (args.length > 0) {
            archivoFuente = args[0];
        } else {
            archivoFuente = "programa3.ñs";
        }

        // Leer archivo fuente
        String codigo;
        try {
            codigo = new String(Files.readAllBytes(Paths.get(archivoFuente)));
        } catch (IOException e) {
            System.err.println("Error: No se pudo abrir el archivo '" + archivoFuente + "'");
            return;
        }

        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║     COMPILADOR Ñava Script  v1.0  –  Universidad Politécnica     ║");
        System.out.println("║                Asignatura: Compiladores e Intérpretes            ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║  Archivo: " + padRight(archivoFuente, 55) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println("\nCÓDIGO FUENTE:");
        System.out.println("─".repeat(68));
        String[] lineas = codigo.split("\n");
        for (int i = 0; i < lineas.length; i++) {
            System.out.printf("  %3d │ %s%n", i + 1, lineas[i]);
        }
        System.out.println("─".repeat(68));

        boolean hayErrores = false;

        // ── ETAPA 1: Análisis Léxico ──────────────────────────────────────────
        System.out.println("\n\n══════════════════════════════════════════════════════════════════");
        System.out.println("  ETAPA 1: ANÁLISIS LÉXICO");
        System.out.println("══════════════════════════════════════════════════════════════════");

        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexico lexer = new AnalizadorLexico(tabla);
        List<Token> tokens = lexer.escanear(codigo);
        lexer.imprimirTokens(tokens);

        if (!lexer.getErrores().isEmpty()) {
            hayErrores = true;
            System.out.println("\n  ✘ Errores léxicos:");
            lexer.getErrores().forEach(e -> System.out.println("  " + e));
        } else {
            System.out.println("\n  ✔ Análisis léxico completado sin errores.");
        }

        if (hayErrores) { finalizarConErrores(); return; }

        // ── ETAPA 2: Análisis Sintáctico ──────────────────────────────────────
        System.out.println("\n\n══════════════════════════════════════════════════════════════════");
        System.out.println("  ETAPA 2: ANÁLISIS SINTÁCTICO");
        System.out.println("══════════════════════════════════════════════════════════════════");

        AnalizadorSintactico parser = new AnalizadorSintactico(tokens);
        NodoBloque ast = parser.parsear();
        parser.imprimirAST(ast);

        if (!parser.getErrores().isEmpty()) {
            hayErrores = true;
            System.out.println("\n  ✘ Errores sintácticos:");
            parser.getErrores().forEach(e -> System.out.println("  " + e));
        } else {
            System.out.println("\n  ✔ Análisis sintáctico completado sin errores.");
        }

        if (hayErrores) { finalizarConErrores(); return; }

        // ── ETAPA 3: Análisis Semántico ───────────────────────────────────────
        System.out.println("\n\n══════════════════════════════════════════════════════════════════");
        System.out.println("  ETAPA 3: ANÁLISIS SEMÁNTICO");
        System.out.println("══════════════════════════════════════════════════════════════════");

        AnalizadorSemantico semantico = new AnalizadorSemantico(tabla);
        semantico.analizar(ast);
        semantico.imprimirResultado();

        if (!semantico.getErrores().isEmpty()) {
            hayErrores = true;
        }

        if (hayErrores) { finalizarConErrores(); return; }

        // ── ETAPA 4: Generación de Código Intermedio ──────────────────────────
        System.out.println("\n\n══════════════════════════════════════════════════════════════════");
        System.out.println("  ETAPA 4: CÓDIGO INTERMEDIO");
        System.out.println("══════════════════════════════════════════════════════════════════");

        // Reiniciar tabla para ejecución (limpiar valores previos del semántico)
        TablaSimbolos tablaEjecucion = new TablaSimbolos();
        GeneradorCodigoIntermedio genCI = new GeneradorCodigoIntermedio();
        genCI.generar(ast);
        genCI.imprimir();

        // Guardar código intermedio en archivo
        String archCI = archivoFuente.replace(".mileng", "") + ".ci";
        try (PrintWriter pw = new PrintWriter(archCI)) {
            genCI.getInstrucciones().forEach(pw::println);
        }
        System.out.println("\n  ✔ Código intermedio guardado en: " + archCI);

        // ── ETAPA 5: Ejecución (Código Objeto Interpretado) ───────────────────
        System.out.println("\n\n══════════════════════════════════════════════════════════════════");
        System.out.println("  ETAPA 5: EJECUCIÓN DEL CÓDIGO OBJETO");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("\n  SALIDA DEL PROGRAMA:");
        System.out.println("  " + "─".repeat(50));

        Interprete interprete = new Interprete(tablaEjecucion);
        interprete.ejecutar(ast);

        System.out.println("  " + "─".repeat(50));

        if (!interprete.getErrores().isEmpty()) {
            System.out.println("\n  ✘ Errores en ejecución:");
            interprete.getErrores().forEach(e -> System.out.println("  " + e));
        } else {
            System.out.println("\n  ✔ Programa ejecutado correctamente.");
        }

        // ── Tabla de Símbolos Final ───────────────────────────────────────────
        System.out.println("\n\n══════════════════════════════════════════════════════════════════");
        System.out.println("  TABLA DE SÍMBOLOS (VALORES FINALES)");
        System.out.println("══════════════════════════════════════════════════════════════════");
        tablaEjecucion.mostrar();

        System.out.println("\n\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║  ✔ Compilación y ejecución completadas exitosamente.             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    private static void finalizarConErrores() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║  ✘ Compilación interrumpida por errores.                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
