# -*- coding: utf-8 -*-
"""
Genera el icono de la app "Aura" (PNG + los XML de fondo).

El logo de la app se dibuja ACÁ, en un solo lugar: de este script salen
todos los archivos (los PNG de cada densidad y el vector del fondo), así que
el icono adaptativo y el icono antiguo nunca se contradicen entre sí.

Aura tiene UN solo logo, el "Aureola": una moneda con su aureola. Antes
había cuatro (Aureola, Billete, Cascada y Destello) y se elegía en Ajustes;
se quitó esa opción, así que acá solo se dibuja el que se usa.

Uso:
    python tools/generar-iconos-aura.py

Salida:
    01-proyectos/ahorros-app/app/src/main/res/mipmap-<densidad>/ic_launcher_v1*.png
    01-proyectos/ahorros-app/app/src/main/res/drawable/ic_launcher_fondo_v1.xml
    _iconos-aura/contacto.png   (hoja de contactos con el logo)
"""

import math
import os
from PIL import Image, ImageDraw, ImageChops

# ----------------------------------------------------------------------
# Rutas
# ----------------------------------------------------------------------

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(AQUI)
RES = os.path.join(RAIZ, "01-proyectos", "ahorros-app", "app", "src", "main", "res")
SALIDA = os.path.join(RAIZ, "_iconos-aura")

# Lienzo maestro. Se dibuja a 2x y se reduce: así los bordes salen lisos.
S = 1024
SS = 2                      # supermuestreo
M = S * SS

# El glifo vive dentro del 56% central del lienzo: ese es el "safe zone"
# del icono adaptativo (66/108 del lienzo es lo que siempre se ve).
SEGURO = 0.28

# Densidades: (carpeta, dp del lienzo)
DENSIDADES = [
    ("mdpi", 1.0),
    ("hdpi", 1.5),
    ("xhdpi", 2.0),
    ("xxhdpi", 3.0),
    ("xxxhdpi", 4.0),
]

# El único icono de la app. "fondo" es el degradado (de arriba-izquierda a
# abajo-derecha en 135 grados) y "glifo" el color de la figura.
# Habla de DINERO (que es lo que la app maneja): una moneda con su aureola.
VARIANTES = [
    {
        "id": "v1",
        "nombre": "Aureola",
        "fondo1": (30, 27, 75),      # #1E1B4B
        "fondo2": (124, 58, 237),    # #7C3AED
        "glifo": (255, 255, 255),
        "brillo": (167, 243, 252),
    },
]


# ----------------------------------------------------------------------
# Utilidades de dibujo
# ----------------------------------------------------------------------

def degrade(size, c1, c2, angulo=135):
    """Degradado lineal de c1 a c2 en el ángulo dado (0 = hacia la derecha)."""
    # Se calcula en baja resolución y se agranda: es más rápido y más suave.
    p = 96
    img = Image.new("RGB", (p, p))
    pix = img.load()
    rad = math.radians(angulo)
    dx, dy = math.cos(rad), math.sin(rad)
    # Proyeccion normalizada de cada pixel sobre la direccion del degrade.
    proys = []
    for y in range(p):
        for x in range(p):
            proys.append(((x / (p - 1)) - 0.5) * dx + ((y / (p - 1)) - 0.5) * dy)
    mn, mx = min(proys), max(proys)
    for y in range(p):
        for x in range(p):
            t = (proys[y * p + x] - mn) / (mx - mn)
            pix[x, y] = (
                int(c1[0] + (c2[0] - c1[0]) * t),
                int(c1[1] + (c2[1] - c1[1]) * t),
                int(c1[2] + (c2[2] - c1[2]) * t),
            )
    return img.resize((size, size), Image.LANCZOS).convert("RGBA")


def bezier2(p0, p1, p2, pasos=28):
    """Puntos de una curva cuadrática de Bézier."""
    pts = []
    for i in range(pasos + 1):
        t = i / pasos
        u = 1 - t
        x = u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0]
        y = u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1]
        pts.append((x, y))
    return pts


def chispa_4_puntas(cx, cy, r_largo, r_corto, angulo=0.0, cintura=0.32):
    """
    Destello de 4 puntas: punta larga arriba, a la derecha, abajo e izquierda,
    con las curvas Entradas entre punta y punta (es el "spark" de cuatro hojas).
    """
    largo = [90.0, 0.0, -90.0, 180.0]
    corto = [45.0, -45.0, -135.0, 135.0]
    puntos = []
    for i in range(4):
        a_l = math.radians(largo[i] + angulo)
        a_c = math.radians(corto[i] + angulo)
        p_l = (cx + r_largo * math.cos(a_l), cy - r_largo * math.sin(a_l))
        p_c = (cx + r_corto * math.cos(a_c), cy - r_corto * math.sin(a_c))
        # El control va hacia el centro: eso hace que el lado quede "hundido".
        ctrl = (
            p_l[0] + (p_c[0] - p_l[0]) * 0.5,
            p_l[1] + (p_c[1] - p_l[1]) * 0.5,
        )
        d = math.hypot(ctrl[0] - cx, ctrl[1] - cy)
        f = cintura
        ctrl = (cx + (ctrl[0] - cx) * f, cy + (ctrl[1] - cy) * f)
        puntos.extend(bezier2(p_l, ctrl, p_c))
    return puntos


def circulo(draw, cx, cy, r, relleno=255):
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=relleno)


def moneda(draw, cx, cy, r, relleno=255, hueco=0.42):
    """Moneda: disco con un agujero en el centro (como una arandela de dinero)."""
    circulo(draw, cx, cy, r, relleno)
    if hueco > 0:
        circulo(draw, cx, cy, r * hueco, 0)


# ----------------------------------------------------------------------
# Los 4 glifos (se dibujan sobre una mascara en blanco y negro)
# ----------------------------------------------------------------------

def glifo_orbita(d, v):
    """Moneda al centro con un anillo inclinado pasando por detras (un planeta)."""
    cx = cy = M / 2
    r_moneda = M * 0.165
    # El anillo se dibuja aparte y se inclina: es lo que da la "orbita".
    capa = Image.new("L", (M, M), 0)
    dc = ImageDraw.Draw(capa)
    ancho = int(M * 0.062)
    dc.ellipse(
        [cx - M * 0.47, cy - M * 0.145, cx + M * 0.47, cy + M * 0.145],
        outline=255,
        width=ancho,
    )
    capa = capa.rotate(-28, resample=Image.BICUBIC, center=(cx, cy))
    # El anillo pasa POR DETRAS de la moneda: se recorta justo donde va la
    # moneda (un anillo, no un disco: por el hueco de la moneda se ve el
    # anillo del fondo, que es justo como se ve un planeta de verdad).
    moneda = Image.new("L", (M, M), 0)
    dm = ImageDraw.Draw(moneda)
    circulo(dm, cx, cy, r_moneda)
    hueco = Image.new("L", (M, M), 0)
    dh = ImageDraw.Draw(hueco)
    circulo(dh, cx, cy, r_moneda * 0.34)
    tapa = ImageChops.subtract(moneda, hueco)
    capa = ImageChops.subtract(capa, tapa)
    d.bitmap((0, 0), capa, fill=255)
    d.bitmap((0, 0), moneda, fill=255)
    # Un puntito en la orbita: da movimiento y la hace reconocible.
    ang = math.radians(-28 + 52)
    px = cx + M * 0.455 * math.cos(ang)
    py = cy + M * 0.455 * math.sin(ang) * 0.36
    circulo(d, px, py, M * 0.042)


def glifo_chispa(d, v):
    """Destello de 4 puntas, con dos chispitas chicas al lado."""
    cx = cy = M / 2
    d.polygon(chispa_4_puntas(cx, cy - M * 0.03, M * 0.30, M * 0.075, 0.0), fill=255)
    d.polygon(
        chispa_4_puntas(cx - M * 0.30, cy + M * 0.20, M * 0.115, M * 0.030, 0.0),
        fill=255,
    )
    d.polygon(
        chispa_4_puntas(cx + M * 0.30, cy - M * 0.20, M * 0.095, M * 0.026, 0.0),
        fill=255,
    )


def glifo_corona(d, v):
    """Una corona: el "aura" de lo que se maneja con realeza."""
    cx = M / 2
    cy = M * 0.52
    u = M  # unidad

    # Las puntas de la corona: pico, valle, pico mas alto, valle, pico.
    puntos = [
        (cx - 0.27 * u, cy + 0.03 * u),
        (cx - 0.225 * u, cy - 0.175 * u),
        (cx - 0.105 * u, cy - 0.015 * u),
        (cx, cy - 0.255 * u),
        (cx + 0.105 * u, cy - 0.015 * u),
        (cx + 0.225 * u, cy - 0.175 * u),
        (cx + 0.27 * u, cy + 0.03 * u),
    ]
    # Los lados se redondean un poco para que no se vea "tijera".
    suave = []
    for i in range(len(puntos) - 1):
        suave.extend(bezier2(puntos[i], puntos[i], puntos[i + 1], 10))
    suave.append(puntos[-1])
    d.polygon(suave, fill=255)
    # La base de la corona.
    d.rounded_rectangle(
        [cx - 0.27 * u, cy + 0.01 * u, cx + 0.27 * u, cy + 0.145 * u],
        radius=int(M * 0.035),
        fill=255,
    )


def glifo_creciente(d, v):
    """Luna con un planetachiquito en su hueco."""
    cx = cy = M / 2
    r = M * 0.295
    # La luna: un disco menos el disco corrido que abre el hueco.
    luna = Image.new("L", (M, M), 0)
    dl = ImageDraw.Draw(luna)
    circulo(dl, cx - M * 0.03, cy, r)
    hueco = Image.new("L", (M, M), 0)
    dh = ImageDraw.Draw(hueco)
    circulo(dh, cx + M * 0.145, cy - M * 0.03, r * 0.86)
    d.bitmap((0, 0), ImageChops.subtract(luna, hueco), fill=255)
    # El planetachiquito vive en la concavidad de la luna.
    circulo(d, cx + M * 0.165, cy - M * 0.075, M * 0.082)


def simbolo_dolar(d, cx, cy, alto, grosor):
    """
    El signo **$** calado en la máscara: la S de los dos arcos y la barra.

    Se dibuja en una capa aparte y se "pasa" con fill=0, que en una máscara
    significa QUITAR (queda el fondo). Por eso el $ sale calado en la moneda
    o en el billete, y no pegado encima.

    La S son dos arcos de círculo que se tocan en el medio (el de arriba abre
    a la izquierda y el de abajo a la derecha). `alto` es la altura de la S.
    """
    capa = Image.new("L", (M, M), 0)
    dc = ImageDraw.Draw(capa)
    r = alto / 4.0
    # El arco de arriba: del centro (abajo de su círculo) por la izquierda y
    # el remate hasta arriba a la derecha.
    dc.arc([cx - r, cy - 2 * r, cx + r, cy], 90, 315, fill=255, width=grosor)
    # El arco de abajo: del centro por la derecha, el fondo y el remate.
    dc.arc([cx - r, cy, cx + r, cy + 2 * r], 270, 560, fill=255, width=grosor)
    # La barra vertical que atraviesa la S.
    dc.line([(cx, cy - alto * 0.62), (cx, cy + alto * 0.62)], fill=255, width=grosor)
    d.bitmap((0, 0), capa, fill=0)


def moneda_con_dolar(d, cx, cy, r):
    """Moneda con el $ calado: un disco con el signo adentro."""
    circulo(d, cx, cy, r)
    simbolo_dolar(d, cx, cy, r * 1.28, max(2, int(r * 0.135)))


def glifo_aureola(d, v):
    """Una moneda con su aureola: el 'aura' de Aura, pero del dinero."""
    cx = cy = M / 2
    # La aureola: un anillo redondo alrededor de la moneda.
    radio = M * 0.30
    d.ellipse(
        [cx - radio, cy - radio, cx + radio, cy + radio],
        outline=255,
        width=int(M * 0.038)
    )
    moneda_con_dolar(d, cx, cy, M * 0.185)


def glifo_billete(d, v):
    """Un billete con el $ calado y un destello en la esquina."""
    ancho, alto = M * 0.56, M * 0.325
    capa = Image.new("L", (M, M), 0)
    dc = ImageDraw.Draw(capa)
    dc.rounded_rectangle(
        [M / 2 - ancho / 2, M / 2 - alto / 2, M / 2 + ancho / 2, M / 2 + alto / 2],
        radius=int(M * 0.05),
        fill=255
    )
    # El billete va un poco inclinado: parece más vivo que recto.
    capa = capa.rotate(-10, resample=Image.BICUBIC, center=(M / 2, M / 2))
    d.bitmap((0, 0), capa, fill=255)
    # El $ calado en el centro.
    simbolo_dolar(d, M / 2, M / 2, M * 0.235, int(M * 0.026))
    # El destello de la esquina.
    d.polygon(chispa_4_puntas(M * 0.715, M * 0.285, M * 0.115, M * 0.030), fill=255)


def glifo_cascada(d, v):
    """Tres monedas cayendo en diagonal: el dinero que entra."""
    for cx, cy, r in (
        (M * 0.375, M * 0.360, M * 0.125),
        (M * 0.545, M * 0.485, M * 0.108),
        (M * 0.645, M * 0.605, M * 0.088)
    ):
        moneda_con_dolar(d, cx, cy, r)


def glifo_destello(d, v):
    """Una moneda en medio de un destello de cuatro puntas."""
    cx = cy = M / 2
    d.polygon(chispa_4_puntas(cx, cy, M * 0.315, M * 0.082), fill=255)
    moneda_con_dolar(d, cx, cy, M * 0.155)


GLIFOS = {
    "v1": glifo_aureola,
    "v2": glifo_billete,
    "v3": glifo_cascada,
    "v4": glifo_destello,
}


# ----------------------------------------------------------------------
# Armado de las imagenes
# ----------------------------------------------------------------------

def imagenes(v):
    """Devuelve (compuesto, glifo RGBA, glifo mono RGBA) de una variante."""
    mascara = Image.new("L", (M, M), 0)
    d = ImageDraw.Draw(mascara)
    GLIFOS[v["id"]](d, v)

    glifo = Image.new("RGBA", (M, M), v["glifo"] + (0,))
    glifo.putalpha(mascara)

    mono = Image.new("RGBA", (M, M), (0, 0, 0, 0))
    mono.putalpha(mascara)

    fondo = degrade(M, v["fondo1"], v["fondo2"], 135)
    compuesto = fondo.copy()
    compuesto.alpha_composite(glifo)
    return compuesto, glifo, mono


def recorte(imagen, forma):
    """Aplica la forma final: cuadrado con esquinas redondeadas o circulo."""
    mask = Image.new("L", (M, M), 0)
    dm = ImageDraw.Draw(mask)
    if forma == "circulo":
        circulo(dm, M / 2, M / 2, M / 2)
    else:
        r = M * 0.22
        dm.rounded_rectangle([0, 0, M - 1, M - 1], radius=r, fill=255)
    salida = imagen.copy()
    alfa = salida.getchannel("A")
    salida.putalpha(ImageChops.multiply(alfa, mask))
    return salida


def escribe(imagen, ruta, tam):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    salida = imagen.resize((tam, tam), Image.LANCZOS)
    salida.save(ruta, "PNG", optimize=True)


def fondo_vector(v, ruta):
    """El fondo del icono adaptativo, como vector con degradado."""
    # 135 grados = de la esquina superior izquierda a la inferior derecha.
    x0, y0, x1, y1 = 0.0, 0.0, 108.0, 108.0
    d = math.hypot(x1 - x0, y1 - y0)
    ang = math.radians(135)
    cx = cy = 54.0
    dx, dy = math.cos(ang), math.sin(ang)
    mitad = d / 2
    sx, sy = cx - dx * mitad, cy - dy * mitad
    ex, ey = cx + dx * mitad, cy + dy * mitad
    c1 = "#%02X%02X%02X" % v["fondo1"]
    c2 = "#%02X%02X%02X" % v["fondo2"]
    xml = f"""<?xml version="1.0" encoding="utf-8"?>
<!--
    Fondo del icono "{v['nombre']}" ({v['id']}).

    Se genera con tools/generar-iconos-aura.py: el degradado tiene que ser
    el MISMO que el del PNG del icono antiguo, o se ven distintos.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="{sx:.2f}"
                android:startY="{sy:.2f}"
                android:endX="{ex:.2f}"
                android:endY="{ey:.2f}">
                <item android:offset="0" android:color="{c1}" />
                <item android:offset="1" android:color="{c2}" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
"""
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        f.write(xml)


def icono_adaptativo(v, con_monocromo):
    """
    El XML del icono adaptativo (Android 8 en adelante).

    - Fondo: el vector con el degradado.
    - Adelante: el PNG del dibujo, que ya viene con el margen que necesita el
      recorte que le pone el launcher.
    - Monocroma (Android 13+): la silueta, que el launcher tiñe con el color
      del tema del sistema.
    """
    capas = [
        '    <background android:drawable="@drawable/ic_launcher_fondo_%s" />' % v["id"],
        '    <foreground android:drawable="@mipmap/ic_launcher_%s_fg" />' % v["id"],
    ]
    if con_monocromo:
        capas.append('    <monochrome android:drawable="@mipmap/ic_launcher_%s_mono" />' % v["id"])
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!--\n"
        "    Icono adaptativo \"%s\" (%s).\n"
        "\n"
        "    Se genera con tools/generar-iconos-aura.py: el fondo, el dibujo y\n"
        "    la silueta tienen que salir de la MISMA fuente, o el icono se ve\n"
        "    partido entre versiones de Android.\n"
        "-->\n"
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        + "\n".join(capas)
        + "\n</adaptive-icon>\n"
    )
    return xml


def hoja_contacto():
    """Una imagen con el icono (grande y chiquito) para mirarlo de un jalon."""
    grande, chico = 220, 64
    pad = 24
    ancho = pad * 2 + len(VARIANTES) * (grande + pad)
    alto = pad * 2 + grande + 24 + chico + 40
    hoja = Image.new("RGB", (ancho, alto), (18, 18, 22))
    dd = ImageDraw.Draw(hoja)
    x = pad
    for v in VARIANTES:
        compuesto, _, _ = imagenes(v)
        grande_img = recorte(compuesto, "cuadrado").resize((grande, grande), Image.LANCZOS)
        hoja.paste(grande_img, (x, pad), grande_img)
        chico_img = recorte(compuesto, "cuadrado").resize((chico, chico), Image.LANCZOS)
        hoja.paste(chico_img, (x + 6, pad + grande + 24), chico_img)
        dd.text((x, pad + grande + 24 + chico + 8), v["nombre"], fill=(230, 230, 235))
        x += grande + pad
    ruta = os.path.join(SALIDA, "contacto.png")
    os.makedirs(SALIDA, exist_ok=True)
    hoja.save(ruta)
    return ruta


def main():
    print("Generando iconos de Aura...")
    for v in VARIANTES:
        compuesto, glifo, mono = imagenes(v)
        cuadrado = recorte(compuesto, "cuadrado")
        circulo = recorte(compuesto, "circulo")

        for carpeta, factor in DENSIDADES:
            base = os.path.join(RES, "mipmap-" + carpeta)
            # Icono adaptativo: el glifo va aparte (el fondo es el vector).
            escribe(glifo, os.path.join(base, f"ic_launcher_{v['id']}_fg.png"), int(108 * factor))
            escribe(mono, os.path.join(base, f"ic_launcher_{v['id']}_mono.png"), int(108 * factor))
            # Icono antiguo (Android 7): el diseño completo, recortado.
            escribe(cuadrado, os.path.join(base, f"ic_launcher_{v['id']}.png"), int(48 * factor))
            escribe(circulo, os.path.join(base, f"ic_launcher_{v['id']}_round.png"), int(48 * factor))

        fondo_vector(v, os.path.join(RES, "drawable", f"ic_launcher_fondo_{v['id']}.xml"))

        # El icono adaptativo: el de Android 8+ y el de Android 13+ (con la
        # capa monocroma para los iconos con tema del sistema).
        for carpeta, con_monocromo in (("mipmap-anydpi-v26", False), ("mipmap-anydpi-v33", True)):
            carpeta_xml = os.path.join(RES, carpeta)
            os.makedirs(carpeta_xml, exist_ok=True)
            with open(os.path.join(carpeta_xml, f"ic_launcher_{v['id']}.xml"), "w", encoding="utf-8") as f:
                f.write(icono_adaptativo(v, con_monocromo))
            with open(os.path.join(carpeta_xml, f"ic_launcher_{v['id']}_round.xml"), "w", encoding="utf-8") as f:
                f.write(icono_adaptativo(v, con_monocromo))

        print(f"  {v['id']}  {v['nombre']:10s} listo")

    print("Hoja de contactos:", hoja_contacto())
    print("Listo.")


if __name__ == "__main__":
    main()
