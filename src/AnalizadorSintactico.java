// AnalizadorSintactico.java - Parser descendente recursivo para MiLeng
import java.util.*;

public class AnalizadorSintactico {
    private final List<Token> tokens;
    private int pos = 0;
    private final List<String> errores = new ArrayList<>();

    public AnalizadorSintactico(List<Token> tokens) {
        this.tokens = tokens;
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    private Token actual() {
        return pos < tokens.size() ? tokens.get(pos) : new Token("EOF", "", -1);
    }

    private Token consumir() {
        Token t = actual();
        pos++;
        return t;
    }

    private Token consumir(String tipo) {
        Token t = actual();
        if (t.tipo.equals(tipo)) {
            pos++;
            return t;
        }
        errores.add("Error sintáctico línea " + t.linea + ": se esperaba '" + tipo + "' pero se encontró '" + t.tipo + "' (\"" + t.lexema + "\")");
        return t;
    }

    private boolean esActual(String tipo) { return actual().tipo.equals(tipo); }

    private boolean esTipo() {
        String t = actual().tipo;
        return t.equals("TIPO_ENTERO") || t.equals("TIPO_DECIMAL") || t.equals("TIPO_CADENA") || t.equals("TIPO_LOGICO");
    }

    // ─── Programa ─────────────────────────────────────────────────────────────

    public NodoBloque parsear() {
        List<NodoAST> sentencias = new ArrayList<>();
        while (!esActual("EOF")) {
            NodoAST s = sentencia();
            if (s != null) sentencias.add(s);
        }
        return new NodoBloque(sentencias, 0);
    }

    // ─── Sentencias ───────────────────────────────────────────────────────────

    private NodoAST sentencia() {
        Token t = actual();
        switch (t.tipo) {
            case "TIPO_ENTERO": case "TIPO_DECIMAL": case "TIPO_CADENA": case "TIPO_LOGICO":
                return declaracion();
            case "ID":
                return asignacionOExpr();
            case "KEY_SI":
                return sentenciaSi();
            case "KEY_MIENTRAS":
                return sentenciaMientras();
            case "KEY_HACER":
                return sentenciaHacerMientras();
            case "KEY_PARA":
                return sentenciaPara();
            case "KEY_SEGUN":
                return sentenciaSegun();
            case "KEY_IMPRIMIR":
                return sentenciaImprimir();
            case "KEY_LEER":
                return sentenciaLeer();
            case "KEY_ROMPER":
                consumir();
                consumir("PUNTO_COMA");
                return new NodoRomper(t.linea);
            case "LLAVE_AB":
                return bloque();
            case "PUNTO_COMA":
                consumir();
                return null;
            default:
                errores.add("Error sintáctico línea " + t.linea + ": sentencia inesperada '" + t.lexema + "'");
                consumir();
                return null;
        }
    }

    private NodoDeclaracion declaracion() {
        String tipo = actual().tipo.replace("TIPO_", "").toLowerCase();
        int linea = actual().linea;
        consumir(); // tipo
        Token idTok = consumir("ID");
        NodoAST init = null;
        if (esActual("OP_ASIG")) {
            consumir(); // =
            init = expresion();
        }
        consumir("PUNTO_COMA");
        return new NodoDeclaracion(tipo, idTok.lexema, init, linea);
    }

    private NodoAST asignacionOExpr() {
        Token idTok = actual();
        // Anticipar: es asignación si siguiente es '='
        if (pos + 1 < tokens.size() && tokens.get(pos + 1).tipo.equals("OP_ASIG")) {
            consumir(); // ID
            consumir(); // =
            NodoAST val = expresion();
            consumir("PUNTO_COMA");
            return new NodoAsignacion(idTok.lexema, val, idTok.linea);
        }
        // Expresión como sentencia (raro pero válido)
        NodoAST expr = expresion();
        consumir("PUNTO_COMA");
        return expr;
    }

    private NodoSi sentenciaSi() {
        int linea = actual().linea;
        consumir("KEY_SI");
        consumir("PAREN_AB");
        NodoAST cond = expresion();
        consumir("PAREN_CE");
        NodoAST entonces = bloqueOSentencia();
        NodoAST sino = null;
        if (esActual("KEY_SINO")) {
            consumir();
            sino = bloqueOSentencia();
        }
        return new NodoSi(cond, entonces, sino, linea);
    }

    private NodoMientras sentenciaMientras() {
        int linea = actual().linea;
        consumir("KEY_MIENTRAS");
        consumir("PAREN_AB");
        NodoAST cond = expresion();
        consumir("PAREN_CE");
        NodoAST cuerpo = bloqueOSentencia();
        return new NodoMientras(cond, cuerpo, linea);
    }

    private NodoHacerMientras sentenciaHacerMientras() {
        int linea = actual().linea;
        consumir("KEY_HACER");
        NodoAST cuerpo = bloque();
        consumir("KEY_MIENTRAS");
        consumir("PAREN_AB");
        NodoAST cond = expresion();
        consumir("PAREN_CE");
        consumir("PUNTO_COMA");
        return new NodoHacerMientras(cuerpo, cond, linea);
    }

    private NodoPara sentenciaPara() {
        int linea = actual().linea;
        consumir("KEY_PARA");
        consumir("PAREN_AB");

        // Inicio: puede ser declaración o asignación
        NodoAST inicio;
        if (esTipo()) {
            inicio = declaracionSinPuntoComa();
            consumir("PUNTO_COMA");
        } else {
            Token idTok = consumir("ID");
            consumir("OP_ASIG");
            NodoAST val = expresion();
            consumir("PUNTO_COMA");
            inicio = new NodoAsignacion(idTok.lexema, val, idTok.linea);
        }

        NodoAST cond = expresion();
        consumir("PUNTO_COMA");
        NodoAST inc = incremento();
        consumir("PAREN_CE");
        NodoAST cuerpo = bloqueOSentencia();
        return new NodoPara(inicio, cond, inc, cuerpo, linea);
    }

    private NodoDeclaracion declaracionSinPuntoComa() {
        String tipo = actual().tipo.replace("TIPO_", "").toLowerCase();
        int linea = actual().linea;
        consumir();
        Token idTok = consumir("ID");
        NodoAST init = null;
        if (esActual("OP_ASIG")) { consumir(); init = expresion(); }
        return new NodoDeclaracion(tipo, idTok.lexema, init, linea);
    }

    private NodoAST incremento() {
        // ID++ / ID-- / ID = expr
        Token idTok = consumir("ID");
        if (esActual("OP_INC")) {
            consumir();
            return new NodoAsignacion(idTok.lexema,
                new NodoBinario("+", new NodoID(idTok.lexema, idTok.linea), new NodoNumero(1, idTok.linea), idTok.linea), idTok.linea);
        } else if (esActual("OP_DEC")) {
            consumir();
            return new NodoAsignacion(idTok.lexema,
                new NodoBinario("-", new NodoID(idTok.lexema, idTok.linea), new NodoNumero(1, idTok.linea), idTok.linea), idTok.linea);
        } else if (esActual("OP_ASIG")) {
            consumir();
            NodoAST val = expresion();
            return new NodoAsignacion(idTok.lexema, val, idTok.linea);
        }
        return new NodoID(idTok.lexema, idTok.linea);
    }

    private NodoSegun sentenciaSegun() {
        int linea = actual().linea;
        consumir("KEY_SEGUN");
        consumir("PAREN_AB");
        NodoAST expr = expresion();
        consumir("PAREN_CE");
        consumir("LLAVE_AB");
        List<NodoCaso> casos = new ArrayList<>();
        NodoBloque defecto = null;
        while (!esActual("LLAVE_CE") && !esActual("EOF")) {
            if (esActual("KEY_CASO")) {
                int lc = actual().linea;
                consumir();
                NodoAST val = expresion();
                consumir("DOS_PUNTOS");
                List<NodoAST> stmts = new ArrayList<>();
                while (!esActual("KEY_CASO") && !esActual("KEY_DEFECTO") && !esActual("LLAVE_CE") && !esActual("EOF")) {
                    NodoAST s = sentencia();
                    if (s != null) stmts.add(s);
                }
                casos.add(new NodoCaso(val, new NodoBloque(stmts, lc), lc));
            } else if (esActual("KEY_DEFECTO")) {
                int ld = actual().linea;
                consumir();
                consumir("DOS_PUNTOS");
                List<NodoAST> stmts = new ArrayList<>();
                while (!esActual("LLAVE_CE") && !esActual("EOF")) {
                    NodoAST s = sentencia();
                    if (s != null) stmts.add(s);
                }
                defecto = new NodoBloque(stmts, ld);
            } else {
                consumir();
            }
        }
        consumir("LLAVE_CE");
        return new NodoSegun(expr, casos, defecto, linea);
    }

    private NodoImprimir sentenciaImprimir() {
        int linea = actual().linea;
        consumir("KEY_IMPRIMIR");
        consumir("PAREN_AB");
        NodoAST expr = expresion();
        consumir("PAREN_CE");
        consumir("PUNTO_COMA");
        return new NodoImprimir(expr, linea);
    }

    private NodoLeer sentenciaLeer() {
        int linea = actual().linea;
        consumir("KEY_LEER");
        consumir("PAREN_AB");
        Token idTok = consumir("ID");
        consumir("PAREN_CE");
        consumir("PUNTO_COMA");
        return new NodoLeer(idTok.lexema, linea);
    }

    private NodoBloque bloque() {
        int linea = actual().linea;
        consumir("LLAVE_AB");
        List<NodoAST> stmts = new ArrayList<>();
        while (!esActual("LLAVE_CE") && !esActual("EOF")) {
            NodoAST s = sentencia();
            if (s != null) stmts.add(s);
        }
        consumir("LLAVE_CE");
        return new NodoBloque(stmts, linea);
    }

    private NodoAST bloqueOSentencia() {
        if (esActual("LLAVE_AB")) return bloque();
        NodoAST s = sentencia();
        List<NodoAST> lista = new ArrayList<>();
        if (s != null) lista.add(s);
        return new NodoBloque(lista, s != null ? s.linea : 0);
    }

    // ─── Expresiones (con precedencia) ───────────────────────────────────────

    public NodoAST expresion() {
        return expOr();
    }

    private NodoAST expOr() {
        NodoAST n = expAnd();
        while (esActual("OP_O")) {
            int linea = actual().linea; String op = consumir().lexema;
            n = new NodoBinario(op, n, expAnd(), linea);
        }
        return n;
    }

    private NodoAST expAnd() {
        NodoAST n = expIgualdad();
        while (esActual("OP_Y")) {
            int linea = actual().linea; String op = consumir().lexema;
            n = new NodoBinario(op, n, expIgualdad(), linea);
        }
        return n;
    }

    private NodoAST expIgualdad() {
        NodoAST n = expRelacional();
        while (esActual("OP_IGUAL") || esActual("OP_DISTINTO")) {
            int linea = actual().linea; String op = consumir().lexema;
            n = new NodoBinario(op, n, expRelacional(), linea);
        }
        return n;
    }

    private NodoAST expRelacional() {
        NodoAST n = expSuma();
        while (esActual("OP_MENOR") || esActual("OP_MAYOR") || esActual("OP_MENOR_IGUAL") || esActual("OP_MAYOR_IGUAL")) {
            int linea = actual().linea; String op = consumir().lexema;
            n = new NodoBinario(op, n, expSuma(), linea);
        }
        return n;
    }

    private NodoAST expSuma() {
        NodoAST n = expMult();
        while (esActual("OP_SUMA") || esActual("OP_RESTA")) {
            int linea = actual().linea; String op = consumir().lexema;
            n = new NodoBinario(op, n, expMult(), linea);
        }
        return n;
    }

    private NodoAST expMult() {
        NodoAST n = expUnario();
        while (esActual("OP_MULT") || esActual("OP_DIV") || esActual("OP_MOD")) {
            int linea = actual().linea; String op = consumir().lexema;
            n = new NodoBinario(op, n, expUnario(), linea);
        }
        return n;
    }

    private NodoAST expUnario() {
        if (esActual("OP_NO")) {
            int linea = actual().linea; String op = consumir().lexema;
            return new NodoUnario(op, expUnario(), linea);
        }
        if (esActual("OP_RESTA")) {
            int linea = actual().linea; consumir();
            return new NodoUnario("-", expUnario(), linea);
        }
        return primario();
    }

    private NodoAST primario() {
        Token t = actual();
        switch (t.tipo) {
            case "LIT_ENTERO":
                consumir();
                return new NodoNumero(Double.parseDouble(t.lexema), t.linea);
            case "LIT_DECIMAL":
                consumir();
                return new NodoNumero(Double.parseDouble(t.lexema), t.linea);
            case "LIT_CADENA":
                consumir();
                return new NodoCadena(t.lexema, t.linea);
            case "LIT_BOOL":
                consumir();
                return new NodoBooleano(t.lexema.equals("verdadero"), t.linea);
            case "ID":
                consumir();
                return new NodoID(t.lexema, t.linea);
            case "PAREN_AB":
                consumir();
                NodoAST expr = expresion();
                consumir("PAREN_CE");
                return expr;
            default:
                errores.add("Error sintáctico línea " + t.linea + ": expresión inesperada '" + t.lexema + "'");
                consumir();
                return new NodoNumero(0, t.linea);
        }
    }

    public List<String> getErrores() { return errores; }

    public void imprimirAST(NodoAST raiz) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║             ÁRBOL SINTÁCTICO ABSTRACTO (AST)                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        raiz.imprimir("", false);
    }
}
