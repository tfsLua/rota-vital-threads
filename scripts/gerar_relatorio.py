#!/usr/bin/env python3
"""
Gera o relatório em PDF da atividade Threads II a partir das medições feitas
pelo benchmark.py (tabela, gráficos, speedup e análise com os números reais).

    pip install matplotlib reportlab
    python3 scripts/gerar_relatorio.py --entrada resultados --pdf docs/Relatorio_Threads_II_RotaVital.pdf
"""
import argparse, csv, json, os
from datetime import date

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from reportlab.lib import colors
from reportlab.lib.enums import TA_JUSTIFY, TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import (Image, KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table,
                                TableStyle)

EQUIPE = ["João Lucas Santos Lira", "Mariana Ferreira", "Thaissa Fernandes", "Priscila Maciel de Lima"]
PROFESSOR = "Raoni Monteiro de Oliveira"
REPO = "https://github.com/joaosanli/rota-vital-threads"   # troque pelo link do repositório do grupo

AZUL = colors.HexColor("#1F4E79")
CINZA = colors.HexColor("#F2F4F7")


# ----------------------------------------------------------------------------- dados
def ler(entrada):
    with open(os.path.join(entrada, "resumo.csv")) as f:
        resumo = list(csv.DictReader(f))
    for r in resumo:
        r["requisicoes"] = int(r["requisicoes"]); r["threads"] = int(r["threads"])
        for k in ("tempo_resposta_mediana_ms", "tempo_processamento_mediana_ms", "speedup", "eficiencia"):
            r[k] = float(r[k])
    with open(os.path.join(entrada, "meta.json")) as f:
        meta = json.load(f)
    return resumo, meta


def get(resumo, n, modo, t):
    for r in resumo:
        if r["requisicoes"] == n and r["modo"] == modo and r["threads"] == t:
            return r
    return None


def fmt_n(n):
    return f"{n:,}".replace(",", ".")


def fmt_ms(v):
    return f"{v:,.1f}".replace(",", "X").replace(".", ",").replace("X", ".")


def fx(v, casas=2):
    return f"{v:.{casas}f}".replace(".", ",")


# ----------------------------------------------------------------------------- gráficos
def graficos(resumo, pasta):
    tamanhos = sorted({r["requisicoes"] for r in resumo})
    threads = sorted({r["threads"] for r in resumo if r["modo"] != "sequencial"})
    modos = [m for m in ("plataforma", "virtual") if any(r["modo"] == m for r in resumo)]

    # 1) tempo de resposta por versão, um painel por tamanho
    fig, axs = plt.subplots(1, len(tamanhos), figsize=(5.2 * len(tamanhos), 3.6))
    axs = axs if len(tamanhos) > 1 else [axs]
    for ax, n in zip(axs, tamanhos):
        rot, val, cor = ["Seq."], [get(resumo, n, "sequencial", 1)["tempo_resposta_mediana_ms"] / 1000], ["#7F7F7F"]
        for m, c in zip(modos, ("#1F77B4", "#FF7F0E")):
            for t in threads:
                r = get(resumo, n, m, t)
                if r:
                    rot.append(f"{'P' if m == 'plataforma' else 'V'}{t}"); val.append(r["tempo_resposta_mediana_ms"] / 1000); cor.append(c)
        b = ax.bar(rot, val, color=cor)
        ax.bar_label(b, fmt="%.2f", fontsize=7)
        ax.set_title(f"{fmt_n(n)} requisições", fontsize=10)
        ax.set_ylabel("tempo de resposta (s)")
        ax.grid(axis="y", alpha=.3)
    fig.suptitle("Tempo de resposta do endpoint (mediana) — P = threads de plataforma, V = virtual threads", fontsize=9)
    fig.tight_layout()
    p1 = os.path.join(pasta, "grafico_tempo.png"); fig.savefig(p1, dpi=170); plt.close(fig)

    # 2) speedup x threads, com a reta ideal
    fig, ax = plt.subplots(figsize=(6.4, 3.8))
    xs = [1] + threads
    ax.plot(xs, xs, "k--", lw=1, label="ideal (linear)")
    estilos = {"plataforma": "-o", "virtual": ":s"}
    for m in modos:
        for n in tamanhos:
            ys = [1.0] + [get(resumo, n, m, t)["speedup"] for t in threads]
            ax.plot(xs, ys, estilos[m], label=f"{m} — {fmt_n(n)}")
    ax.set_xticks(xs); ax.set_xlabel("número de threads"); ax.set_ylabel("speedup (T_seq / T_p)")
    ax.set_title("Speedup em relação à versão sequencial", fontsize=10)
    ax.grid(alpha=.3); ax.legend(fontsize=7)
    fig.tight_layout()
    p2 = os.path.join(pasta, "grafico_speedup.png"); fig.savefig(p2, dpi=170); plt.close(fig)
    return p1, p2


# ----------------------------------------------------------------------------- texto da análise
def textos_analise(resumo, meta):
    nucleos = meta["info"]["nucleosDisponiveis"]
    tamanhos = sorted({r["requisicoes"] for r in resumo})
    grande, pequeno = tamanhos[-1], tamanhos[0]
    threads = sorted({r["threads"] for r in resumo if r["modo"] == "plataforma"})
    tmax = threads[-1]
    sp = {t: get(resumo, grande, "plataforma", t)["speedup"] for t in threads}
    sp_peq = get(resumo, pequeno, "plataforma", tmax)["speedup"]
    melhor_t = max(sp, key=sp.get)
    v = get(resumo, grande, "virtual", tmax)
    p = get(resumo, grande, "plataforma", tmax)

    A = []
    serie = ", ".join(f"{fx(sp[t])}× ({t})" for t in threads)
    razao = (get(resumo, grande, "sequencial", 1)["tempo_resposta_mediana_ms"]
             / get(resumo, pequeno, "sequencial", 1)["tempo_resposta_mediana_ms"])
    if nucleos == 1:
        desvio = max(abs(r["speedup"] - 1) for r in resumo) * 100
        A.append(
            f"<b>O ganho foi linear?</b> Não — foi nulo: speedup de {serie} com {fmt_n(grande)} requisições "
            f"(variações de até {fx(desvio, 0)}%, dentro do ruído). A máquina tinha <b>1 núcleo</b>: as threads "
            f"existiam, mas o SO só executava uma por vez, revezando-as sobre o mesmo trabalho total. Com N núcleos o "
            f"teto seria N×, e ainda abaixo disso pela Lei de Amdahl: receber o HTTP, fatiar, agregar e serializar o "
            f"JSON seguem sequenciais, e há custo de coordenar e esperar a fatia mais lenta.")
    else:
        extra = (f" Acima de {nucleos} threads não há núcleo livre e o ganho estaciona." if tmax > nucleos else "")
        if sp_peq < sp[tmax]:
            menor = f" Com {fmt_n(pequeno)} requisições foi {fx(sp_peq)}× em {tmax} threads: o custo fixo pesa mais."
        else:
            menor = (f" Com {fmt_n(pequeno)} requisições o speedup foi maior ({fx(sp_peq)}× em {tmax} threads): "
                     f"esses dados cabem quase todos no cache do processador e cada execução termina antes de o "
                     f"turbo cair; com {fmt_n(grande)} as threads disputam a memória RAM por segundos seguidos.")
        A.append(
            f"<b>O ganho foi linear?</b> Não: speedup de {serie} com {fmt_n(grande)} requisições, em {nucleos} "
            f"núcleos lógicos. Pela Lei de Amdahl, receber o HTTP, fatiar, agregar e serializar o JSON seguem "
            f"sequenciais; há custo de coordenação (submeter e esperar todas); as fatias têm custo desigual (insumos "
            f"raros têm menos candidatos) e a resposta espera a mais lenta; núcleos lógicos (hyper-threading) "
            f"dividem unidades de cálculo e o turbo cai com todos ocupados.{extra}{menor}")
    fator_tamanho = grande / pequeno if pequeno else 0
    A.append(
        f"<b>A Big-O mudou?</b> Não. O trabalho segue O(R·H) — os mesmos pares são avaliados, só repartidos. O tempo "
        f"ideal vira O(R·H/p) + O(p) da agregação, mas p é constante: threads dividem o tempo por uma constante, não "
        f"mudam a classe. Ao aumentar as requisições de {fmt_n(pequeno)} para {fmt_n(grande)} ({fx(fator_tamanho, 1)}×), "
        f"o tempo sequencial medido aumentou {fx(razao, 1)}×.")
    A.append(
        "<b>Concorrência × paralelismo.</b> Na Mesa DJ as threads passavam o tempo dormindo e reagindo a eventos: "
        "<i>concorrência</i>, lidar com várias coisas ao mesmo tempo — rodaria igual num único núcleo. Aqui elas "
        "dividem um volume de cálculo sem esperar nada: <i>paralelismo</i>, fazer várias coisas ao mesmo tempo, que "
        "só acelera com núcleos livres para executá-las simultaneamente."
        + (" Em 1 núcleo, paralelismo vira só concorrência — e não há ganho." if nucleos == 1 else ""))
    A.append(
        "<b>Quando nem 8 threads bastarem.</b> Uma máquina para no número de núcleos dela; o passo seguinte é escalar "
        "<i>horizontalmente</i>: tornar o cruzamento um job assíncrono (a API responde 202 e o painel busca o "
        "resultado); distribuir as fatias (por UF ou faixa) entre <i>workers</i> em várias máquinas via fila de "
        "mensagens, com um agregador somando as parciais — o mesmo fatiar-e-agregar, agora distribuído "
        "(map/reduce); replicar a API stateless atrás de um balanceador; e recalcular só o que mudou. Esse salto de "
        "threads para processos em várias máquinas é o gancho da Unidade 2.")
    V = None
    if v and p:
        dif = (v["tempo_resposta_mediana_ms"] / p["tempo_resposta_mediana_ms"] - 1) * 100
        V = (f"Com {tmax} virtual threads e {fmt_n(grande)} requisições o endpoint levou "
             f"{fmt_ms(v['tempo_resposta_mediana_ms'])} ms, contra {fmt_ms(p['tempo_resposta_mediana_ms'])} ms com "
             f"{tmax} threads de plataforma ({'+' if dif >= 0 else ''}{fx(dif, 1)}%) — "
             + ("na prática, empate. " if abs(dif) < 10 else "diferença de agendamento, não de paralelismo extra. ")
             + f"É o esperado: "
             f"virtual threads são executadas sobre um pequeno pool de <i>carrier threads</i> (ForkJoinPool com "
             f"paralelismo = nº de núcleos, aqui {nucleos}). Numa tarefa CPU-bound elas nunca bloqueiam, logo nunca "
             f"liberam o carrier, e o paralelismo real continua limitado pelos núcleos. Virtual threads brilham em "
             f"cargas de E/S (milhares de requisições esperando banco ou rede — o perfil da Mesa DJ e da própria API "
             f"atendendo usuários), onde tornam barato ter uma thread por requisição; para acelerar cálculo, o que "
             f"importa é o número de núcleos, não o tipo de thread.")
    return A, V


# ----------------------------------------------------------------------------- PDF
def estilos():
    ss = getSampleStyleSheet()
    base = ParagraphStyle("base", parent=ss["Normal"], fontName="Helvetica", fontSize=9.6, leading=13.2,
                          alignment=TA_JUSTIFY, spaceAfter=5)
    return {
        "base": base,
        "peq": ParagraphStyle("peq", parent=base, fontSize=8, leading=10.5, alignment=0, spaceAfter=0),
        "cel": ParagraphStyle("cel", parent=base, fontSize=7.8, leading=9.6, alignment=0, spaceAfter=0),
        "tit": ParagraphStyle("tit", parent=base, fontName="Helvetica-Bold", fontSize=17, leading=21, textColor=AZUL,
                              alignment=TA_CENTER, spaceAfter=2),
        "sub": ParagraphStyle("sub", parent=base, fontSize=10.5, alignment=TA_CENTER, textColor=colors.HexColor("#444444")),
        "h1": ParagraphStyle("h1", parent=base, fontName="Helvetica-Bold", fontSize=12.5, leading=16, textColor=AZUL,
                             spaceBefore=8, spaceAfter=4, alignment=0),
        "h2": ParagraphStyle("h2", parent=base, fontName="Helvetica-Bold", fontSize=10.2, leading=13, spaceBefore=4,
                             spaceAfter=2, alignment=0),
        "code": ParagraphStyle("code", parent=base, fontName="Courier", fontSize=7.8, leading=10, alignment=0,
                               backColor=CINZA, borderPadding=4, spaceBefore=2, spaceAfter=6),
        "leg": ParagraphStyle("leg", parent=base, fontSize=8, leading=10, alignment=TA_CENTER,
                              textColor=colors.HexColor("#555555")),
    }


def tabela(dados, larguras, cab_cor=AZUL, zebra=True, alinhar_dir_de=None):
    t = Table(dados, colWidths=larguras, repeatRows=1)
    st = [("BACKGROUND", (0, 0), (-1, 0), cab_cor), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
          ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"), ("FONTSIZE", (0, 0), (-1, -1), 7.8),
          ("VALIGN", (0, 0), (-1, -1), "MIDDLE"), ("GRID", (0, 0), (-1, -1), .3, colors.HexColor("#C8CDD3")),
          ("TOPPADDING", (0, 0), (-1, -1), 2.5), ("BOTTOMPADDING", (0, 0), (-1, -1), 2.5)]
    if zebra:
        for i in range(1, len(dados)):
            if i % 2 == 0:
                st.append(("BACKGROUND", (0, i), (-1, i), CINZA))
    if alinhar_dir_de is not None:
        st.append(("ALIGN", (alinhar_dir_de, 1), (-1, -1), "RIGHT"))
    t.setStyle(TableStyle(st))
    return t


def rodape(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(colors.HexColor("#777777"))
    canvas.drawString(2 * cm, 1.2 * cm, "Rota Vital — Threads II (Projeto Integrador)")
    canvas.drawRightString(A4[0] - 2 * cm, 1.2 * cm, f"página {doc.page}")
    canvas.restoreState()


def gerar_pdf(resumo, meta, g_tempo, g_speed, destino):
    S = estilos()
    info = meta["info"]
    tamanhos = sorted({r["requisicoes"] for r in resumo})
    grande = tamanhos[-1]
    bases = {int(k): v for k, v in meta.get("bases", {}).items()}
    H = meta.get("hospitais", 1000)
    C = meta.get("candidatos_por_insumo", 209.6)
    pares = grande * C
    seq_g = get(resumo, grande, "sequencial", 1)
    P = lambda txt, s="base": Paragraph(txt, S[s])
    st = []

    # ---------------- capa / cabeçalho
    st += [P("Rota Vital — Atividade em Equipe: Threads II", "tit"),
           P("Cruzamento <b>requisições × estoque</b> em escala nacional: versão sequencial vs. versões com threads", "sub"),
           Spacer(1, 4),
           P(f"<b>Equipe:</b> {', '.join(EQUIPE)} &nbsp;|&nbsp; <b>Professor:</b> {PROFESSOR} &nbsp;|&nbsp; "
             f"<b>Data:</b> {date.today().strftime('%d/%m/%Y')}", "sub"),
           P(f"<b>Código:</b> {REPO} (Java 21 + Spring Boot 3)", "sub"), Spacer(1, 6)]

    # ---------------- 1. Justificativa
    st.append(P("1. Justificativa", "h1"))
    st.append(P("1.1 Como encontramos a operação", "h2"))
    st.append(P("Seguimos o roteiro: listamos as operações da camada de aplicação que processam volume e, para cada uma, "
                "perguntamos onde o tempo é gasto na escala nacional, qual a Big-O e se os dados se partem em fatias "
                "independentes."))
    c = lambda x: Paragraph(x, S["cel"])
    cand = [["Operação candidata", "Onde vai o tempo (escala nacional)", "Big-O", "Particionável?", "Serve?"],
            [c("Estatísticas do histórico (totais e médias por UF/mês)"), c("Ler milhões de linhas; a conta é trivial. GROUP BY + índices ou visão materializada no banco resolvem."), c("O(n)"), c("Sim"), c("Não — melhorar o SQL")],
            [c("Detecção de duplicatas"), c("Com hash ou índice único a checagem é O(1) por registro; o custo é E/S."), c("O(n)"), c("Parcialmente"), c("Não — UNIQUE/índice")],
            [c("Ordenação da fila por prioridade"), c("ORDER BY sobre índice, ou heap mantido incrementalmente a cada nova requisição."), c("O(n log n)"), c("Sim"), c("Não — índice/heap")],
            [c("Validações em lote"), c("Regras simples por registro; domina ler e gravar o lote."), c("O(n)"), c("Sim"), c("Ganho pequeno")],
            [c("<b>Cruzamento requisições × estoque</b> (sugerir o melhor fornecedor para cada requisição pendente)"),
             c("<b>Cálculo:</b> cada requisição é comparada com todos os hospitais que têm o insumo — distância geodésica "
               "(senos, raiz, arco-seno) + regras de estoque por par."), c("<b>O(R·H)</b>"),
             c("<b>Sim</b> — requisições independentes"), c("<b>Sim — escolhida</b>")]]
    st.append(tabela(cand, [4.1 * cm, 6.4 * cm, 1.7 * cm, 2.4 * cm, 2.4 * cm]))
    st.append(Spacer(1, 5))

    st.append(P("1.2 A operação escolhida", "h2"))
    st.append(P(
        "O painel nacional do Rota Vital precisa responder, várias vezes ao dia: <i>para cada requisição de insumo "
        "pendente, qual hospital deveria fornecer?</i> Para uma requisição (local, insumo, quantidade q, prioridade), o "
        "serviço percorre os hospitais que têm o insumo, descarta os com estoque &lt; q, calcula a distância "
        "(Haversine) e uma pontuação = distância + penalidade de 80 km se o fornecedor ficaria com menos que 2q, e "
        "escolhe a menor (empate: menor id). Em seguida agrega indicadores: requisições atendíveis, distância média, "
        "% dentro do SLA por prioridade (1 h, 4 h, 12 h, 48 h), entregas interestaduais e os mesmos números por UF."))
    st.append(P("1.3 Big-O da solução sequencial", "h2"))
    st.append(P(
        f"Com R requisições pendentes e C hospitais candidatos por insumo (C proporcional a H, o total de hospitais), "
        f"o laço externo roda R vezes e o interno C vezes: <b>O(R·C) = O(R·H)</b> em tempo, O(R + H·I) em memória "
        f"(I = nº de insumos). Na nossa base, H = {fmt_n(H)} hospitais, I = 30 insumos e C médio = {fx(C, 1)}; "
        f"com R = {fmt_n(grande)} são ~{fx(pares / 1e6, 0)} milhões de pares avaliados, cada um com funções "
        f"trigonométricas. Dobrar R ou H dobra o tempo."))
    st.append(P("1.4 Onde está o gargalo", "h2"))
    # se a base já estava em cache no servidor, o tempo medido (~0 ms) não representa a carga real
    carga = None if bases.get(grande, {}).get("jaEstavaEmCache") else bases.get(grande, {}).get("tempoMs")
    txt_carga = (f"Gerar/carregar o snapshot de {fmt_n(grande)} requisições levou {fmt_ms(carga)} ms, enquanto o "
                 f"cruzamento sequencial levou {fmt_ms(seq_g['tempo_processamento_mediana_ms'])} ms. " if carga else "")
    st.append(P(
        "Os dados de entrada são lidos uma vez (O(R) de E/S, uma consulta simples) e ficam em memória; o tempo depois "
        f"disso é todo de CPU dentro do laço O(R·H). {txt_carga}"
        "Por que não é caso de “melhorar o SQL”: um CROSS JOIN com a fórmula de distância faria os mesmos R·C cálculos "
        "dentro do banco — o recurso mais caro de escalar — e travaria as demais consultas do sistema; índices comuns "
        "não ajudam, porque a pontuação depende do par (requisição, hospital) e do estoque. (Um índice espacial "
        "poderia podar candidatos — otimização algorítmica complementar ao paralelismo, não substituta.)"))
    st.append(P("1.5 Por que os dados são particionáveis", "h2"))
    st.append(P(
        "O cálculo de uma requisição só <b>lê</b> dados imutáveis durante a operação (hospitais e snapshot do estoque) "
        "e só produz o resultado dela mesma: nenhuma requisição depende de outra. A sugestão não reserva estoque — a "
        "reserva acontece depois, numa transação, quando o regulador confirma. Portanto [0, R) pode ser cortado em p "
        "fatias contíguas, cada thread processa a sua com o próprio acumulador, e os resultados parciais são somados "
        "no fim. Como a agregação é soma de inteiros (distâncias em metros inteiros) — associativa e comutativa —, a "
        "ordem em que as fatias terminam não altera nada: a resposta paralela é <b>idêntica</b> à sequencial."))

    # ---------------- 2. Serviço
    st.append(P("2. O serviço (Java 21 + Spring Boot)", "h1"))
    st.append(P(
        "A operação é um endpoint REST real: a requisição HTTP chega ao <i>CruzamentoController</i> (thread do Tomcat), "
        "que chama o <i>CruzamentoService</i>; este obtém o snapshot do <i>RepositorioDeDados</i> e executa o "
        "<i>MotorCruzamento</i> no modo pedido. Nas versões com threads o motor fatia [0, R), submete uma tarefa por "
        "fatia a um <i>ExecutorService</i> (pools fixos de 2, 4 e 8 threads de plataforma, criados uma vez e "
        "reaproveitados; ou um executor de virtual threads), espera todas com <i>invokeAll</i> (join), soma as parciais "
        "e devolve o JSON."))
    st.append(P(
        "POST /api/dados/gerar?requisicoes=1000000 &nbsp;&nbsp;&nbsp;# gera a base (semente fixa = 42)<br/>"
        "GET&nbsp; /api/cruzamento?modo=sequencial&amp;requisicoes=1000000<br/>"
        "GET&nbsp; /api/cruzamento?modo=plataforma&amp;threads=8&amp;requisicoes=1000000<br/>"
        "GET&nbsp; /api/cruzamento?modo=virtual&amp;threads=8&amp;requisicoes=1000000<br/>"
        "GET&nbsp; /api/cruzamento?modo=inseguro&amp;threads=8&amp;requisicoes=100000 &nbsp;# demo de race condition", "code"))
    demo = meta.get("demo_race")
    txt_demo = ""
    if demo:
        div = sum(1 for t in demo["tentativas"] if not t["igual"])
        txt_demo = (f" Na nossa execução, {div} de {len(demo['tentativas'])} tentativas do modo inseguro divergiram "
                    f"da sequencial — e o resultado muda de uma execução para outra, típico de race condition.")
    diverg = meta.get("divergencias", [])
    st.append(P(
        "<b>Mesma resposta em todas as versões.</b> Cada fatia usa um acumulador próprio (confinamento de thread: "
        "nada compartilhado é escrito durante o cálculo, então não há lock nem disputa); só a thread da requisição "
        "faz a agregação. A resposta traz um <i>checksum</i> de todos os pares (requisição, hospital escolhido) e uma "
        "<i>assinatura</i> de todos os números; o benchmark compara a assinatura de cada chamada com a da sequencial "
        + ("e <b>todas foram iguais</b>" if not diverg else f"e encontrou <b>{len(diverg)} divergências</b>")
        + ". Um teste JUnit verifica o mesmo para 2, 3, 4, 8 e 16 threads. Para contraste, o modo <i>inseguro</i> "
          "faz N threads somarem num único acumulador compartilhado sem sincronização (<i>x++</i> = ler, somar, "
          "gravar): incrementos se perdem." + txt_demo))

    # ---------------- 3. Medições
    st.append(P("3. Medições", "h1"))
    srv = info.get("servidor", "")
    st.append(P(
        f"<b>Ambiente:</b> {srv}; {info['nucleosDisponiveis']} núcleo(s) disponível(is) para a JVM; Java {info['java']}; "
        f"{info.get('sistemaOperacional', '')}; heap máx. {info.get('heapMaximoMb', '?')} MB. <b>Método:</b> tempo de "
        f"resposta HTTP medido no cliente (benchmark.py), 1 chamada de aquecimento do JIT por configuração descartada, "
        f"{meta.get('reps', 3)} medições intercaladas por configuração, valor = <b>mediana</b>. "
        f"Speedup = T<sub>seq</sub> / T<sub>p</sub>; eficiência = speedup / nº de threads."))
    if "harness" in srv.lower():
        st.append(P("<i>Observação: estes números foram obtidos no ambiente de desenvolvimento usado para validar o "
                    "código (container com 1 vCPU, sem acesso ao Maven Central, rodando o mesmo controller e serviço "
                    "sobre o servidor HTTP do JDK). Para os números finais, rodar benchmark.py com o Spring Boot numa "
                    "máquina multinúcleo e regenerar este PDF (seção 6).</i>", "peq"))
    lin = [["Requisições", "Versão", "Threads", "Resposta HTTP (ms)", "Processamento (ms)", "Speedup", "Eficiência"]]
    for n in tamanhos:
        for r in [x for x in resumo if x["requisicoes"] == n]:
            lin.append([fmt_n(n), r["modo"], str(r["threads"]), fmt_ms(r["tempo_resposta_mediana_ms"]),
                        fmt_ms(r["tempo_processamento_mediana_ms"]), fx(r["speedup"]) + "×",
                        fx(r["eficiencia"] * 100, 0) + "%"])
    st.append(tabela(lin, [2.2 * cm, 2.3 * cm, 1.5 * cm, 3.1 * cm, 3.6 * cm, 1.8 * cm, 2 * cm], alinhar_dir_de=2))
    st.append(Spacer(1, 6))
    st.append(Image(g_tempo, width=17 * cm, height=17 * cm * 3.6 / 10.4))
    st.append(P("Figura 1 — Tempo de resposta do endpoint por versão e tamanho de entrada.", "leg"))
    st.append(KeepTogether([Image(g_speed, width=12.5 * cm, height=12.5 * cm * 3.8 / 6.4),
                            P("Figura 2 — Speedup × número de threads (linha tracejada = speedup ideal).", "leg")]))

    # ---------------- 4. Análise
    A, V = textos_analise(resumo, meta)
    st.append(P("4. Análise", "h1"))
    st.append(Spacer(1, 1))
    for par in A:
        st.append(P(par))

    # ---------------- 5. Opcional
    if V:
        st.append(KeepTogether([P("5. Opcional — virtual threads (Java 21) × threads de plataforma", "h1"), P(V)]))

    # ---------------- 6. Reproduzir
    st.append(P("6. Como reproduzir", "h1"))
    st.append(P(
        "./mvnw spring-boot:run &nbsp;&nbsp;&nbsp;&nbsp;# ou rodar RotaVitalApplication pela IDE (Java 21)<br/>"
        "python3 scripts/benchmark.py --tamanhos 100000,1000000 --reps " + str(meta.get("reps", 3)) + "<br/>"
        "pip install matplotlib reportlab<br/>"
        "python3 scripts/gerar_relatorio.py --entrada resultados", "code"))
    st.append(P("Os dados são gerados com semente fixa: qualquer máquina produz exatamente as mesmas requisições, "
                "hospitais e resposta (mesma assinatura); só os tempos mudam."))

    doc = SimpleDocTemplate(destino, pagesize=A4, leftMargin=2 * cm, rightMargin=2 * cm, topMargin=1.6 * cm,
                            bottomMargin=1.8 * cm, title="Rota Vital — Threads II", author=", ".join(EQUIPE))
    doc.build(st, onFirstPage=rodape, onLaterPages=rodape)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--entrada", default="resultados")
    ap.add_argument("--pdf", default=None)
    a = ap.parse_args()
    resumo, meta = ler(a.entrada)
    g1, g2 = graficos(resumo, a.entrada)
    destino = a.pdf or os.path.join(a.entrada, "Relatorio_Threads_II_RotaVital.pdf")
    os.makedirs(os.path.dirname(os.path.abspath(destino)), exist_ok=True)
    gerar_pdf(resumo, meta, g1, g2, destino)
    print("PDF gerado:", destino)


if __name__ == "__main__":
    main()
