// NodoAST.java - Nodos del Árbol Sintáctico Abstracto
import java.util.*;

// Nodo base abstracto
abstract class NodoAST {
    public int linea;
    abstract void imprimir(String prefijo, boolean esIzquierdo);
}

// ─── Expresiones ─────────────────────────────────────────────────────────────

class NodoNumero extends NodoAST {
    public final double valor;
    public NodoNumero(double valor, int linea) { this.valor = valor; this.linea = linea; }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + valor);
    }
}

class NodoCadena extends NodoAST {
    public final String valor;
    public NodoCadena(String valor, int linea) { this.valor = valor; this.linea = linea; }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "\"" + valor + "\"");
    }
}

class NodoBooleano extends NodoAST {
    public final boolean valor;
    public NodoBooleano(boolean valor, int linea) { this.valor = valor; this.linea = linea; }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + valor);
    }
}

class NodoID extends NodoAST {
    public final String nombre;
    public NodoID(String nombre, int linea) { this.nombre = nombre; this.linea = linea; }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "ID(" + nombre + ")");
    }
}

class NodoBinario extends NodoAST {
    public final String operador;
    public final NodoAST izq, der;
    public NodoBinario(String op, NodoAST izq, NodoAST der, int linea) {
        this.operador = op; this.izq = izq; this.der = der; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "[" + operador + "]");
        izq.imprimir(p + (esIz ? "│   " : "    "), true);
        der.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoUnario extends NodoAST {
    public final String operador;
    public final NodoAST operando;
    public NodoUnario(String op, NodoAST operando, int linea) {
        this.operador = op; this.operando = operando; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "[" + operador + "]");
        operando.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

// ─── Sentencias ───────────────────────────────────────────────────────────────

class NodoDeclaracion extends NodoAST {
    public final String tipo;
    public final String nombre;
    public final NodoAST inicializador; // puede ser null
    public NodoDeclaracion(String tipo, String nombre, NodoAST init, int linea) {
        this.tipo = tipo; this.nombre = nombre; this.inicializador = init; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "DECL(" + tipo + " " + nombre + ")");
        if (inicializador != null) inicializador.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoAsignacion extends NodoAST {
    public final String nombre;
    public final NodoAST valor;
    public NodoAsignacion(String nombre, NodoAST valor, int linea) {
        this.nombre = nombre; this.valor = valor; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "ASIG(" + nombre + " =)");
        valor.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoImprimir extends NodoAST {
    public final NodoAST expresion;
    public NodoImprimir(NodoAST expr, int linea) { this.expresion = expr; this.linea = linea; }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "IMPRIMIR");
        expresion.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoLeer extends NodoAST {
    public final String variable;
    public NodoLeer(String variable, int linea) { this.variable = variable; this.linea = linea; }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "LEER(" + variable + ")");
    }
}

class NodoBloque extends NodoAST {
    public final List<NodoAST> sentencias;
    public NodoBloque(List<NodoAST> sentencias, int linea) { this.sentencias = sentencias; this.linea = linea; }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "BLOQUE");
        for (int i = 0; i < sentencias.size(); i++)
            sentencias.get(i).imprimir(p + (esIz ? "│   " : "    "), i < sentencias.size() - 1);
    }
}

class NodoSi extends NodoAST {
    public final NodoAST condicion;
    public final NodoAST entonces;
    public final NodoAST sino; // puede ser null
    public NodoSi(NodoAST cond, NodoAST entonces, NodoAST sino, int linea) {
        this.condicion = cond; this.entonces = entonces; this.sino = sino; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "SI");
        condicion.imprimir(p + (esIz ? "│   " : "    "), true);
        entonces.imprimir(p + (esIz ? "│   " : "    "), sino != null);
        if (sino != null) sino.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoMientras extends NodoAST {
    public final NodoAST condicion;
    public final NodoAST cuerpo;
    public NodoMientras(NodoAST cond, NodoAST cuerpo, int linea) {
        this.condicion = cond; this.cuerpo = cuerpo; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "MIENTRAS");
        condicion.imprimir(p + (esIz ? "│   " : "    "), true);
        cuerpo.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoHacerMientras extends NodoAST {
    public final NodoAST cuerpo;
    public final NodoAST condicion;
    public NodoHacerMientras(NodoAST cuerpo, NodoAST cond, int linea) {
        this.cuerpo = cuerpo; this.condicion = cond; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "HACER-MIENTRAS");
        cuerpo.imprimir(p + (esIz ? "│   " : "    "), true);
        condicion.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoPara extends NodoAST {
    public final NodoAST inicio;
    public final NodoAST condicion;
    public final NodoAST incremento;
    public final NodoAST cuerpo;
    public NodoPara(NodoAST inicio, NodoAST cond, NodoAST inc, NodoAST cuerpo, int linea) {
        this.inicio = inicio; this.condicion = cond; this.incremento = inc; this.cuerpo = cuerpo; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "PARA");
        inicio.imprimir(p + (esIz ? "│   " : "    "), true);
        condicion.imprimir(p + (esIz ? "│   " : "    "), true);
        incremento.imprimir(p + (esIz ? "│   " : "    "), true);
        cuerpo.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoSegun extends NodoAST {
    public final NodoAST expresion;
    public final List<NodoCaso> casos;
    public final NodoBloque defecto; // puede ser null
    public NodoSegun(NodoAST expr, List<NodoCaso> casos, NodoBloque def, int linea) {
        this.expresion = expr; this.casos = casos; this.defecto = def; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "SEGUN");
        expresion.imprimir(p + (esIz ? "│   " : "    "), true);
        for (NodoCaso c : casos) c.imprimir(p + (esIz ? "│   " : "    "), true);
        if (defecto != null) defecto.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoCaso extends NodoAST {
    public final NodoAST valor;
    public final NodoBloque cuerpo;
    public NodoCaso(NodoAST valor, NodoBloque cuerpo, int linea) {
        this.valor = valor; this.cuerpo = cuerpo; this.linea = linea;
    }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "CASO");
        valor.imprimir(p + (esIz ? "│   " : "    "), true);
        cuerpo.imprimir(p + (esIz ? "│   " : "    "), false);
    }
}

class NodoRomper extends NodoAST {
    public NodoRomper(int linea) { this.linea = linea; }
    @Override public void imprimir(String p, boolean esIz) {
        System.out.println(p + (esIz ? "├── " : "└── ") + "ROMPER");
    }
}
