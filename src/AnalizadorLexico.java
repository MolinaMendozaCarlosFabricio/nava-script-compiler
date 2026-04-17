// AnalizadorLexico.java - Análisis léxico del compilador MiLeng
import java.util.*;

public class AnalizadorLexico {
    private final TablaSimbolos tabla;
    private final List<String> errores = new ArrayList<>();
    private int linea = 1;

    public AnalizadorLexico(TablaSimbolos tabla) {
        this.tabla = tabla;
    }

    public List<Token> escanear(String entrada) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = entrada.length();

        while (i < n) {
            char c = entrada.charAt(i);

            // Saltos de línea
            if (c == '\n') { linea++; i++; continue; }
            // Espacios en blanco
            if (Character.isWhitespace(c)) { i++; continue; }

            // Comentarios // ...
            if (c == '/' && i + 1 < n && entrada.charAt(i + 1) == '/') {
                while (i < n && entrada.charAt(i) != '\n') i++;
                continue;
            }
            // Comentarios /* ... */
            if (c == '/' && i + 1 < n && entrada.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(entrada.charAt(i) == '*' && entrada.charAt(i + 1) == '/')) {
                    if (entrada.charAt(i) == '\n') linea++;
                    i++;
                }
                i += 2;
                continue;
            }

            // Cadenas de texto "..."
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                while (i < n && entrada.charAt(i) != '"') {
                    if (entrada.charAt(i) == '\n') { errores.add("Error léxico línea " + linea + ": cadena sin cerrar"); break; }
                    sb.append(entrada.charAt(i)); i++;
                }
                i++; // cerrar "
                tokens.add(new Token("LIT_CADENA", sb.toString(), linea));
                continue;
            }

            // Números enteros y decimales
            if (Character.isDigit(c)) {
                StringBuilder num = new StringBuilder();
                boolean esDecimal = false;
                while (i < n && (Character.isDigit(entrada.charAt(i)) || entrada.charAt(i) == '.')) {
                    if (entrada.charAt(i) == '.') {
                        if (esDecimal) { errores.add("Error léxico línea " + linea + ": número decimal mal formado"); break; }
                        esDecimal = true;
                    }
                    num.append(entrada.charAt(i)); i++;
                }
                tokens.add(new Token(esDecimal ? "LIT_DECIMAL" : "LIT_ENTERO", num.toString(), linea));
                continue;
            }

            // Identificadores y palabras reservadas
            if (Character.isLetter(c) || c == '_') {
                StringBuilder id = new StringBuilder();
                while (i < n && (Character.isLetterOrDigit(entrada.charAt(i)) || entrada.charAt(i) == '_')) {
                    id.append(entrada.charAt(i)); i++;
                }
                String lex = id.toString();
                if (tabla.esReservada(lex)) {
                    // Mapear a su tipo de token
                    tokens.add(new Token(mapearReservada(lex), lex, linea));
                } else {
                    tokens.add(new Token("ID", lex, linea));
                }
                continue;
            }

            // Operadores de dos caracteres
            if (i + 1 < n) {
                String dos = "" + c + entrada.charAt(i + 1);
                String tipo = operadorDoble(dos);
                if (tipo != null) {
                    tokens.add(new Token(tipo, dos, linea));
                    i += 2; continue;
                }
            }

            // Operadores y símbolos simples
            String tipoSimple = operadorSimple(c);
            if (tipoSimple != null) {
                tokens.add(new Token(tipoSimple, String.valueOf(c), linea));
                i++; continue;
            }

            errores.add("Error léxico línea " + linea + ": carácter desconocido '" + c + "'");
            i++;
        }

        tokens.add(new Token("EOF", "", linea));
        return tokens;
    }

    private String mapearReservada(String lex) {
        switch (lex) {
            case "entero":    return "TIPO_ENTERO";
            case "decimal":   return "TIPO_DECIMAL";
            case "cadena":    return "TIPO_CADENA";
            case "logico":    return "TIPO_LOGICO";
            case "si":        return "KEY_SI";
            case "sino":      return "KEY_SINO";
            case "mientras":  return "KEY_MIENTRAS";
            case "hacer":     return "KEY_HACER";
            case "para":      return "KEY_PARA";
            case "segun":     return "KEY_SEGUN";
            case "caso":      return "KEY_CASO";
            case "defecto":   return "KEY_DEFECTO";
            case "romper":    return "KEY_ROMPER";
            case "leer":      return "KEY_LEER";
            case "imprimir":  return "KEY_IMPRIMIR";
            case "verdadero": case "falso": return "LIT_BOOL";
            case "y":         return "OP_Y";
            case "o":         return "OP_O";
            case "no":        return "OP_NO";
            default:          return lex.toUpperCase();
        }
    }

    private String operadorDoble(String s) {
        switch (s) {
            case "==": return "OP_IGUAL";
            case "!=": return "OP_DISTINTO";
            case "<=": return "OP_MENOR_IGUAL";
            case ">=": return "OP_MAYOR_IGUAL";
            case "++": return "OP_INC";
            case "--": return "OP_DEC";
            default:   return null;
        }
    }

    private String operadorSimple(char c) {
        switch (c) {
            case '=': return "OP_ASIG";
            case '+': return "OP_SUMA";
            case '-': return "OP_RESTA";
            case '*': return "OP_MULT";
            case '/': return "OP_DIV";
            case '%': return "OP_MOD";
            case '<': return "OP_MENOR";
            case '>': return "OP_MAYOR";
            case '(': return "PAREN_AB";
            case ')': return "PAREN_CE";
            case '{': return "LLAVE_AB";
            case '}': return "LLAVE_CE";
            case '[': return "CORCH_AB";
            case ']': return "CORCH_CE";
            case ';': return "PUNTO_COMA";
            case ':': return "DOS_PUNTOS";
            case ',': return "COMA";
            default:  return null;
        }
    }

    public List<String> getErrores() { return errores; }

    public void imprimirTokens(List<Token> tokens) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    ANÁLISIS LÉXICO - TOKENS                      ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("  %-25s | %-20s | %s%n", "TIPO", "LEXEMA", "LÍNEA");
        System.out.println("  " + "-".repeat(60));
        for (Token t : tokens) {
            if (!t.tipo.equals("EOF"))
                System.out.println("  " + t);
        }
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }
}
