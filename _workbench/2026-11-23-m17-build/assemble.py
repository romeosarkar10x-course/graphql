#!/usr/bin/env python3
"""Builds Module 17's engine labs from the reference engine (contract/engine) and the stubs in stubs/.

Each lab's starter is the reference engine with that lab's component replaced by its stub; its solution is the
reference engine. Lab 17.3 and mini-project 17.9 also get the fuzz test (with copies of the harness's Gj and Known)
and the pom with test dependencies; the other labs get a pom without them. Run from anywhere:
    python3 _workbench/2026-11-23-m17-build/assemble.py
"""
import os, re, shutil

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
M17 = os.path.join(ROOT, 'modules', 'm17-under-the-hood')
ENGINE = os.path.join(M17, 'contract', 'engine')
HARNESS = os.path.join(M17, 'contract', 'harness', 'dev', 'shelfie', 'harness')
STUBS = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'stubs')
PKG = os.path.join('src', 'main', 'java', 'dev', 'shelfie', 'mini')

LABS = {   # lab dir: (stubbed files in the starter, fuzz test included)
    '02-lexer': (['Lexer.java'], False),
    '03-parser': (['Parser.java'], True),
    '04-validation': (['Validator.java'], False),
    '05-executor': (['Executor.java'], False),
    '06-introspection': (['Introspection.java', 'Printer.java'], False),
    '09-project-engine': (['Lexer.java', 'Parser.java', 'Validator.java', 'Executor.java', 'Introspection.java', 'Printer.java'], True),
}


def plain_pom(pom):
    pom = re.sub(r'\n  <!-- Tests only:.*?</dependencies>\n\n  <build>', '\n  <build>', pom, flags=re.S)
    pom = re.sub(r'\n      <plugin>\n        <groupId>org.apache.maven.plugins</groupId>\n        <artifactId>maven-surefire-plugin</artifactId>.*?</plugin>', '', pom, flags=re.S)
    return pom


def build(lab, stubs, fuzz, variant):
    out = os.path.join(M17, 'labs', lab, variant)
    for keep in ('pom.xml', 'src'):
        p = os.path.join(out, keep)
        if os.path.isdir(p): shutil.rmtree(p)
    os.makedirs(out, exist_ok=True)
    shutil.copytree(os.path.join(ENGINE, 'src', 'main'), os.path.join(out, 'src', 'main'))
    if variant == 'starter':
        for f in stubs: shutil.copy(os.path.join(STUBS, f), os.path.join(out, PKG, f))
    pom = open(os.path.join(ENGINE, 'pom.xml')).read()
    if fuzz:
        shutil.copytree(os.path.join(ENGINE, 'src', 'test'), os.path.join(out, 'src', 'test'))
        for f in ('Gj.java', 'Known.java'):
            shutil.copy(os.path.join(HARNESS, f), os.path.join(out, 'src', 'test', 'java', 'dev', 'shelfie', 'harness', f))
    else:
        pom = plain_pom(pom)
    open(os.path.join(out, 'pom.xml'), 'w').write(pom)


for lab, (stubs, fuzz) in LABS.items():
    for variant in ('starter', 'solution'):
        build(lab, stubs, fuzz, variant)
    print('assembled', lab)
