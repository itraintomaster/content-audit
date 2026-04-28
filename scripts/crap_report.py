#!/usr/bin/env python3
"""
CRAP report generator for the content-audit Java codebase.

CRAP (Change Risk Anti-Pattern) score per method:
    CRAP(m, cov) = comp(m)^2 * (1 - cov(m))^3 + comp(m)

Without live coverage data we report the worst-case (cov = 0%) which is the
same as: CRAP = CCN^2 + CCN. Methods with CCN <= 5 are considered acceptable
regardless of coverage, so the table highlights the ones that will bite.

Uses lizard (https://github.com/terryyin/lizard) to parse Java source code.
"""
from __future__ import annotations

import os
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import lizard

ROOT = Path(__file__).resolve().parent.parent

# ---------- ANSI helpers ----------
RESET = "\033[0m"
BOLD = "\033[1m"
DIM = "\033[2m"
ITALIC = "\033[3m"

# 24-bit color palette (dark, modern)
def fg(r: int, g: int, b: int) -> str: return f"\033[38;2;{r};{g};{b}m"
def bg(r: int, g: int, b: int) -> str: return f"\033[48;2;{r};{g};{b}m"

COL_TITLE   = fg(180, 220, 255) + BOLD
COL_DIM     = fg(110, 118, 129)
COL_LABEL   = fg(150, 180, 210)
COL_NUM     = fg(220, 220, 220) + BOLD
COL_RULE    = fg(70, 80, 95)
COL_ACCENT  = fg(255, 184, 108)

# Risk tiers (green -> amber -> red)
TIER_GREEN  = fg(80,  200, 130)
TIER_LIME   = fg(190, 220, 90)
TIER_AMBER  = fg(255, 184, 108)
TIER_ORANGE = fg(255, 140, 80)
TIER_RED    = fg(255, 90, 90)
TIER_CRIT   = fg(255, 70, 140) + BOLD

def risk_color(crap: float) -> str:
    if crap <= 2:    return TIER_GREEN
    if crap <= 12:   return TIER_LIME
    if crap <= 30:   return TIER_AMBER
    if crap <= 90:   return TIER_ORANGE
    if crap <= 300:  return TIER_RED
    return TIER_CRIT

def risk_label(crap: float) -> str:
    if crap <= 2:   return "clean"
    if crap <= 12:  return "ok"
    if crap <= 30:  return "watch"
    if crap <= 90:  return "risky"
    if crap <= 300: return "crappy"
    return "crit"


# ---------- Data ----------
@dataclass
class Method:
    module: str
    file: str
    cls: str
    name: str
    ccn: int
    nloc: int
    params: int
    length: int
    start: int

    @property
    def crap(self) -> float:
        # Worst-case (coverage = 0): CCN^2 + CCN.
        return self.ccn * self.ccn + self.ccn

    @property
    def display_name(self) -> str:
        return f"{self.cls}::{self.name}"


def discover_sources() -> list[Path]:
    roots = []
    for mod in sorted(ROOT.iterdir()):
        if not mod.is_dir(): continue
        main_java = mod / "src" / "main" / "java"
        if main_java.exists():
            roots.append(main_java)
    return roots


def parse_method(file_path: Path, fn) -> Method:
    # lizard gives us "ClassName::method" in long_name when using verbose,
    # but in the FunctionInfo we can reconstruct from name which already is
    # qualified for Java.
    raw = fn.name or fn.long_name or "?"
    if "::" in raw:
        cls, name = raw.split("::", 1)
    else:
        cls, name = file_path.stem, raw
    module = file_path.relative_to(ROOT).parts[0]
    return Method(
        module=module,
        file=str(file_path.relative_to(ROOT)),
        cls=cls,
        name=name,
        ccn=fn.cyclomatic_complexity,
        nloc=fn.nloc,
        params=len(fn.parameters),
        length=fn.length,
        start=fn.start_line,
    )


def analyze() -> list[Method]:
    methods: list[Method] = []
    for root in discover_sources():
        for file_analysis in lizard.analyze([str(root)]):
            p = Path(file_analysis.filename)
            for fn in file_analysis.function_list:
                methods.append(parse_method(p, fn))
    return methods


# ---------- Presentation ----------
WIDTH = 96

def hr(char: str = "─", color: str = COL_RULE) -> str:
    return color + (char * WIDTH) + RESET

def title_box(text: str) -> str:
    pad = (WIDTH - len(text) - 2) // 2
    left = "─" * pad
    right = "─" * (WIDTH - len(text) - 2 - pad)
    return f"{COL_RULE}╭{left}{RESET} {COL_TITLE}{text}{RESET} {COL_RULE}{right}╮{RESET}"

def print_header(methods: list[Method]) -> None:
    total_methods = len(methods)
    total_nloc = sum(m.nloc for m in methods)
    files = {m.file for m in methods}
    avg_ccn = sum(m.ccn for m in methods) / max(total_methods, 1)
    max_ccn = max((m.ccn for m in methods), default=0)
    avg_crap = sum(m.crap for m in methods) / max(total_methods, 1)
    max_crap = max((m.crap for m in methods), default=0)

    print()
    print(title_box("CRAP REPORT · content-audit"))
    print(f"{COL_RULE}│{RESET}  {COL_LABEL}Methods analyzed{RESET}   {COL_NUM}{total_methods:>6}{RESET}       "
          f"{COL_LABEL}Files{RESET}            {COL_NUM}{len(files):>6}{RESET}       "
          f"{COL_LABEL}NLOC{RESET}        {COL_NUM}{total_nloc:>7}{RESET}   {COL_RULE}│{RESET}")
    print(f"{COL_RULE}│{RESET}  {COL_LABEL}Avg CCN{RESET}            {COL_NUM}{avg_ccn:>6.2f}{RESET}       "
          f"{COL_LABEL}Max CCN{RESET}          {COL_NUM}{max_ccn:>6}{RESET}       "
          f"{COL_LABEL}Avg CRAP{RESET}    {COL_NUM}{avg_crap:>7.1f}{RESET}   {COL_RULE}│{RESET}")
    print(f"{COL_RULE}│{RESET}  {COL_LABEL}Max CRAP{RESET}         {COL_NUM}{max_crap:>8.0f}{RESET}       "
          f"{COL_DIM}CRAP = CCN² + CCN · worst-case (uncovered) formula{RESET}              {COL_RULE}│{RESET}")
    print(f"{COL_RULE}╰{'─'*WIDTH}╯{RESET}")


def print_distribution(methods: list[Method]) -> None:
    buckets = [
        ("clean  (1–2)",     TIER_GREEN,  lambda c: c <= 2),
        ("ok     (3–5)",     TIER_LIME,   lambda c: 3  <= c <= 5),
        ("watch  (6–10)",    TIER_AMBER,  lambda c: 6  <= c <= 10),
        ("risky  (11–15)",   TIER_ORANGE, lambda c: 11 <= c <= 15),
        ("crappy (16–25)",   TIER_RED,    lambda c: 16 <= c <= 25),
        ("crit   (26+)",     TIER_CRIT,   lambda c: c >= 26),
    ]
    total = len(methods)
    print()
    print(f"{COL_TITLE}Complexity Distribution{RESET}  {COL_DIM}(by cyclomatic complexity){RESET}")
    print(hr())
    max_count = max((sum(1 for m in methods if pred(m.ccn)) for _, _, pred in buckets), default=1)
    bar_w = 50
    for label, color, pred in buckets:
        count = sum(1 for m in methods if pred(m.ccn))
        pct = 100.0 * count / max(total, 1)
        fill = int(round(bar_w * count / max(max_count, 1)))
        bar = color + ("█" * fill) + COL_DIM + ("░" * (bar_w - fill)) + RESET
        print(f"  {color}{label:<16}{RESET} {bar} {COL_NUM}{count:>5}{RESET} {COL_DIM}({pct:5.1f}%){RESET}")
    print(hr())


def print_top_offenders(methods: list[Method], limit: int = 15) -> None:
    ranked = sorted(methods, key=lambda m: m.crap, reverse=True)[:limit]
    print()
    print(f"{COL_TITLE}Top {len(ranked)} CRAP Offenders{RESET}  {COL_DIM}(highest risk first){RESET}")
    print(hr())
    print(f"  {COL_LABEL}{'#':<3}{'CRAP':>6}  {'CCN':>4} {'NLOC':>5} {'PARM':>4}  "
          f"{'TIER':<8} {'METHOD':<38} {'LOCATION'}{RESET}")
    print(hr("·"))
    for i, m in enumerate(ranked, 1):
        color = risk_color(m.crap)
        tier  = risk_label(m.crap)
        short_name = m.display_name
        if len(short_name) > 37: short_name = short_name[:36] + "…"
        loc = f"{m.module}/…/{Path(m.file).name}:{m.start}"
        if len(loc) > 38: loc = loc[:37] + "…"
        print(f"  {COL_DIM}{i:<3}{RESET}"
              f"{color}{m.crap:>6.0f}{RESET}  "
              f"{color}{m.ccn:>4}{RESET} "
              f"{COL_NUM}{m.nloc:>5}{RESET} "
              f"{COL_NUM}{m.params:>4}{RESET}  "
              f"{color}{tier:<8}{RESET} "
              f"{short_name:<38} "
              f"{COL_DIM}{loc}{RESET}")
    print(hr())


def print_module_breakdown(methods: list[Method]) -> None:
    by_mod: dict[str, list[Method]] = defaultdict(list)
    for m in methods:
        by_mod[m.module].append(m)
    print()
    print(f"{COL_TITLE}Per-Module Breakdown{RESET}  {COL_DIM}(sorted by total CRAP){RESET}")
    print(hr())
    print(f"  {COL_LABEL}{'MODULE':<32} {'METHODS':>8} {'AVG CCN':>9} {'MAX CCN':>9} "
          f"{'TOTAL CRAP':>12} {'HOTSPOTS':>10}{RESET}")
    print(hr("·"))
    rows = []
    for mod, ms in by_mod.items():
        total_crap = sum(m.crap for m in ms)
        max_ccn = max(m.ccn for m in ms)
        avg_ccn = sum(m.ccn for m in ms) / len(ms)
        hotspots = sum(1 for m in ms if m.ccn > 10)
        rows.append((mod, len(ms), avg_ccn, max_ccn, total_crap, hotspots))
    rows.sort(key=lambda r: r[4], reverse=True)
    for mod, n, avg_c, max_c, total_crap, hot in rows:
        color = risk_color(total_crap / max(n, 1))
        hot_color = TIER_RED if hot >= 3 else (TIER_AMBER if hot > 0 else TIER_GREEN)
        print(f"  {COL_LABEL}{mod:<32}{RESET} "
              f"{COL_NUM}{n:>8}{RESET} "
              f"{color}{avg_c:>9.2f}{RESET} "
              f"{color}{max_c:>9}{RESET} "
              f"{color}{total_crap:>12.0f}{RESET} "
              f"{hot_color}{hot:>10}{RESET}")
    print(hr())


def print_footer(methods: list[Method]) -> None:
    risky = [m for m in methods if m.ccn > 10]
    pct = 100.0 * len(risky) / max(len(methods), 1)
    msg = (f"{COL_DIM}Legend:{RESET} "
           f"{TIER_GREEN}clean{RESET} {TIER_LIME}ok{RESET} {TIER_AMBER}watch{RESET} "
           f"{TIER_ORANGE}risky{RESET} {TIER_RED}crappy{RESET} {TIER_CRIT}crit{RESET}   "
           f"{COL_ACCENT}{len(risky)}{RESET} method(s) over CCN>10 "
           f"({COL_ACCENT}{pct:.1f}%{RESET} of codebase)")
    print()
    print(msg)
    print(f"{COL_DIM}{ITALIC}CRAP score is worst-case — adding tests collapses CRAP toward CCN.{RESET}")
    print()


def main() -> int:
    methods = analyze()
    if not methods:
        print("No Java methods found.", file=sys.stderr)
        return 1
    print_header(methods)
    print_distribution(methods)
    print_top_offenders(methods, limit=15)
    print_module_breakdown(methods)
    print_footer(methods)
    return 0


if __name__ == "__main__":
    sys.exit(main())
