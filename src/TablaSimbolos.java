// TablaSimbolos.java - Tabla de símbolos del compilador
import java.util.*;

public class TablaSimbolos {
    public static class Simbolo {
        public String nombre;
        public String tipo;        // entero, decimal, cadena, booleano
        public String categoria;   // variable, constante, funcion
        public Object valor;
        public int linea;

        public Simbolo(String nombre, String tipo, String categoria, int linea) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.categoria = categoria;
            this.linea = linea;
            this.valor = null;
        }

        @Override
        public String toString() {
            return String.format("%-15s | %-10s | %-10s | %-15s | Línea %d",
                nombre, tipo != null ? tipo : "?", categoria, valor != null ? valor.toString() : "null", linea);
        }
    }

    private final LinkedHashMap<String, Simbolo> tabla = new LinkedHashMap<>();

    // Palabras reservadas del lenguaje "MiLeng" (en español)
    public TablaSimbolos() {
        String[][] reservadas = {
            // Tipos
            {"entero",    "TIPO_ENTERO"},
            {"decimal",   "TIPO_DECIMAL"},
            {"cadena",    "TIPO_CADENA"},
            {"logico",    "TIPO_LOGICO"},
            // Control de flujo
            {"si",        "KEY_SI"},
            {"sino",      "KEY_SINO"},
            {"mientras",  "KEY_MIENTRAS"},
            {"hacer",     "KEY_HACER"},
            {"para",      "KEY_PARA"},
            {"segun",     "KEY_SEGUN"},
            {"caso",      "KEY_CASO"},
            {"defecto",   "KEY_DEFECTO"},
            {"romper",    "KEY_ROMPER"},
            // E/S
            {"leer",      "KEY_LEER"},
            {"imprimir",  "KEY_IMPRIMIR"},
            // Valores literales
            {"verdadero", "LIT_BOOL"},
            {"falso",     "LIT_BOOL"},
            // Operadores lógicos
            {"y",         "OP_Y"},
            {"o",         "OP_O"},
            {"no",        "OP_NO"},
        };
        for (String[] r : reservadas) {
            tabla.put(r[0], new Simbolo(r[0], "reservada", "palabra_reservada", 0));
        }
    }

    public boolean esReservada(String lex) {
        Simbolo s = tabla.get(lex);
        return s != null && s.categoria.equals("palabra_reservada");
    }

    public String getTipoReservada(String lex) {
        Simbolo s = tabla.get(lex);
        if (s != null && s.categoria.equals("palabra_reservada")) return s.nombre.toUpperCase();
        return null;
    }

    public void declarar(String nombre, String tipo, String categoria, int linea) {
        if (!tabla.containsKey(nombre)) {
            tabla.put(nombre, new Simbolo(nombre, tipo, categoria, linea));
        }
    }

    public boolean existe(String nombre) {
        return tabla.containsKey(nombre) && !tabla.get(nombre).categoria.equals("palabra_reservada");
    }

    public Simbolo obtener(String nombre) {
        return tabla.get(nombre);
    }

    public void setValor(String nombre, Object valor) {
        if (tabla.containsKey(nombre)) tabla.get(nombre).valor = valor;
    }

    public void mostrar() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     TABLA DE SÍMBOLOS                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("  %-15s | %-10s | %-10s | %-15s | %s%n", "Nombre", "Tipo", "Categoría", "Valor", "Línea");
        System.out.println("  " + "-".repeat(70));
        for (Simbolo s : tabla.values()) {
            if (!s.categoria.equals("palabra_reservada")) {
                System.out.println("  " + s);
            }
        }
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }
}
