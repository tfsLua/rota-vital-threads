#!/usr/bin/env python3
"""
Benchmark do endpoint /api/cruzamento do Rota Vital (só biblioteca padrão).

Mede o TEMPO DE RESPOSTA HTTP (cliente) de cada versão — sequencial,
threads de plataforma (2, 4, 8) e virtual threads (2, 4, 8) — para cada
tamanho de entrada, confere se todas devolveram exatamente a mesma resposta
(assinatura) e grava os CSVs usados pelo gerar_relatorio.py.

Uso (com o servidor rodando: ./mvnw spring-boot:run):
    python3 scripts/benchmark.py
    python3 scripts/benchmark.py --tamanhos 100000,1000000 --reps 5
"""
import argparse, csv, json, os, statistics, sys, time, urllib.request, urllib.error
from datetime import datetime


def chamar(url, metodo="GET", timeout=900):
    req = urllib.request.Request(url, method=metodo)
    t0 = time.perf_counter()
    with urllib.request.urlopen(req, timeout=timeout) as r:
        corpo = r.read()
    ms = (time.perf_counter() - t0) * 1000.0
    return json.loads(corpo), ms


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--url", default="http://localhost:8080")
    ap.add_argument("--tamanhos", default="100000,1000000")
    ap.add_argument("--threads", default="2,4,8")
    ap.add_argument("--reps", type=int, default=3, help="medições por configuração (usa a mediana)")
    ap.add_argument("--sem-virtual", action="store_true")
    ap.add_argument("--sem-demo-race", action="store_true")
    ap.add_argument("--saida", default="resultados")
    a = ap.parse_args()

    tamanhos = [int(x) for x in a.tamanhos.split(",")]
    threads = [int(x) for x in a.threads.split(",")]
    configs = [("sequencial", 1)] + [("plataforma", n) for n in threads]
    if not a.sem_virtual:
        configs += [("virtual", n) for n in threads]
    os.makedirs(a.saida, exist_ok=True)

    try:
        info, _ = chamar(f"{a.url}/api/info")
    except urllib.error.URLError as e:
        sys.exit(f"Servidor não respondeu em {a.url} ({e}). Rode antes: ./mvnw spring-boot:run")
    print(f"Servidor: {info['servidor']} | núcleos: {info['nucleosDisponiveis']} | Java {info['java']}")

    brutos, divergencias, bases = [], [], {}
    for n in tamanhos:
        g, _ = chamar(f"{a.url}/api/dados/gerar?requisicoes={n}", "POST")
        bases[n] = g
        print(f"\n== {n:,} requisições | {g['hospitais']} hospitais | "
              f"{g['mediaHospitaisCandidatosPorInsumo']} candidatos/insumo (média) ==")
        # aquecimento do JIT: uma chamada de cada configuração, descartada
        for modo, t in configs:
            chamar(f"{a.url}/api/cruzamento?modo={modo}&threads={t}&requisicoes={n}")
        ref = None
        for rep in range(1, a.reps + 1):
            for modo, t in configs:  # ordem intercalada: reduz efeito de "deriva" da máquina
                corpo, ms = chamar(f"{a.url}/api/cruzamento?modo={modo}&threads={t}&requisicoes={n}")
                ref = ref or corpo["assinatura"]
                igual = corpo["assinatura"] == ref
                if not igual:
                    divergencias.append((n, modo, t, rep))
                brutos.append({"requisicoes": n, "modo": modo, "threads": t, "rep": rep,
                               "tempo_resposta_ms": round(ms, 2),
                               "tempo_processamento_ms": round(corpo["tempoProcessamentoMs"], 2),
                               "assinatura": corpo["assinatura"], "igual_sequencial": igual,
                               "atendiveis": corpo["atendiveis"],
                               "distancia_media_km": corpo["distanciaMediaKm"]})
                print(f"  rep {rep} {modo:<10} {t:>2} thr  {ms:>10.1f} ms  {'OK' if igual else 'DIVERGIU!'}")

    # ---- resumo (mediana) + speedup ----
    resumo = []
    for n in tamanhos:
        base = statistics.median(r["tempo_resposta_ms"] for r in brutos
                                 if r["requisicoes"] == n and r["modo"] == "sequencial")
        for modo, t in configs:
            v = [r["tempo_resposta_ms"] for r in brutos if r["requisicoes"] == n and r["modo"] == modo and r["threads"] == t]
            p = [r["tempo_processamento_ms"] for r in brutos if r["requisicoes"] == n and r["modo"] == modo and r["threads"] == t]
            med = statistics.median(v)
            resumo.append({"requisicoes": n, "modo": modo, "threads": t,
                           "tempo_resposta_mediana_ms": round(med, 1),
                           "tempo_processamento_mediana_ms": round(statistics.median(p), 1),
                           "speedup": round(base / med, 2),
                           "eficiencia": round(base / med / t, 2)})

    demo = None
    if not a.sem_demo_race:
        n = min(tamanhos)
        seq, _ = chamar(f"{a.url}/api/cruzamento?modo=sequencial&requisicoes={n}")
        tentativas = []
        for _ in range(3):
            r, _ = chamar(f"{a.url}/api/cruzamento?modo=inseguro&threads=8&requisicoes={n}")
            tentativas.append({"atendiveis": r["atendiveis"], "igual": r["assinatura"] == seq["assinatura"]})
        demo = {"requisicoes": n, "atendiveis_sequencial": seq["atendiveis"], "tentativas": tentativas}
        print(f"\nDemo race condition (8 threads sem sincronização, {n:,}): "
              + ", ".join(f"{t['atendiveis']} ({'igual' if t['igual'] else 'DIVERGIU'})" for t in tentativas)
              + f"  | sequencial = {seq['atendiveis']}")

    with open(os.path.join(a.saida, "medicoes_brutas.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(brutos[0].keys())); w.writeheader(); w.writerows(brutos)
    with open(os.path.join(a.saida, "resumo.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(resumo[0].keys())); w.writeheader(); w.writerows(resumo)
    with open(os.path.join(a.saida, "meta.json"), "w") as f:
        json.dump({"info": info, "data": datetime.now().isoformat(timespec="seconds"), "reps": a.reps,
                   "hospitais": g["hospitais"], "candidatos_por_insumo": g["mediaHospitaisCandidatosPorInsumo"],
                   "bases": bases, "divergencias": divergencias, "demo_race": demo}, f, ensure_ascii=False, indent=2)

    print("\nResumo (mediana do tempo de resposta do endpoint):")
    print(f"{'requisições':>12} {'modo':<11}{'thr':>4} {'tempo (ms)':>11} {'speedup':>8}")
    for r in resumo:
        print(f"{r['requisicoes']:>12,} {r['modo']:<11}{r['threads']:>4} {r['tempo_resposta_mediana_ms']:>11.1f} {r['speedup']:>8.2f}")
    print("\nTodas as versões devolveram a MESMA resposta." if not divergencias
          else f"\nATENÇÃO: {len(divergencias)} divergência(s) — race condition!")
    print(f"Arquivos em {a.saida}/  ->  agora: python3 scripts/gerar_relatorio.py --entrada {a.saida}")


if __name__ == "__main__":
    main()
