#!/usr/bin/env python3
"""Assemble Module 16's lab apps from the reference app (contract/ops-app).

solution = the reference app (without GameDay.java) + the lab's extra files;
starter  = the same, minus the lab's feature (stubs and edits below).
Run from the repo root: python3 _workbench/2026-11-22-m16-build/assemble.py [lab ...]
"""
import os, re, shutil, sys
ROOT = os.getcwd()
REF = 'modules/m16-production/contract/ops-app'
LABS = 'modules/m16-production/labs'
BUILD = '_workbench/2026-11-22-m16-build'
PKG = 'src/main/java/dev/shelfie/ops'

def copy_ref(dst):
    if os.path.exists(dst): shutil.rmtree(dst)
    os.makedirs(dst)
    shutil.copy(f'{REF}/pom.xml', dst)
    shutil.copytree(f'{REF}/src', f'{dst}/src')
    os.remove(f'{dst}/{PKG}/GameDay.java')

def edit(path, old, new, count=1):
    s = open(path).read()
    if old not in s: raise SystemExit(f'{path}: not found: {old[:60]!r}')
    open(path, 'w').write(s.replace(old, new, count))

def drop_block(path, start, end_marker):
    """Remove from the line containing start up to (not including) the line containing end_marker."""
    lines = open(path).read().split('\n'); out = []; skipping = False
    for l in lines:
        if start in l: skipping = True
        elif skipping and end_marker in l: skipping = False
        if not skipping: out.append(l)
    open(path, 'w').write('\n'.join(out))

def drop_dep(pom, artifact):
    s = open(pom).read()
    new = re.sub(r'(\n    <!--[^\n]*-->)?\n    <dependency>\n      <groupId>[^<]+</groupId>\n      <artifactId>' + re.escape(artifact) + r'</artifactId>\n(      <version>[^<]+</version>\n)?    </dependency>', '', s, count=1)
    if new == s: raise SystemExit(f'{pom}: no dependency {artifact}')
    open(pom, 'w').write(new)

def put(path, text):
    os.makedirs(os.path.dirname(path) or '.', exist_ok=True)
    open(path, 'w').write(text)

def extras(lab, side):
    src = f'{BUILD}/extra/{lab}/{side}'
    if os.path.isdir(src): shutil.copytree(src, f'{LABS}/{lab}/{side}', dirs_exist_ok=True)

def lab(name, starter_fn):
    base = f'{LABS}/{name}'
    for side in ('solution', 'starter'):
        copy_ref(f'{base}/{side}')
    starter_fn(f'{base}/starter')
    extras(name, 'solution'); extras(name, 'starter')
    print('assembled', name)

STUBS = f'{BUILD}/stubs'

def s01(d):
    shutil.copy(f'{STUBS}/01/Observability.java', f'{d}/{PKG}/Observability.java')
    for a in ('micrometer-registry-prometheus', 'spring-boot-starter-opentelemetry'): drop_dep(f'{d}/pom.xml', a)
    y = f'{d}/src/main/resources/application.yaml'
    edit(y, '        include: health,prometheus', '        include: health')
    drop_block(y, '  metrics:', 'shelfie:')

def s02(d):
    shutil.copy(f'{STUBS}/02/UsageReporter.java', f'{d}/{PKG}/UsageReporter.java')

def s03(d):
    pass

def s04(d):
    shutil.copy(f'{BUILD}/variants/plain/LiveController.java', f'{d}/{PKG}/LiveController.java')
    os.remove(f'{d}/{PKG}/Shutdown.java')
    y = f'{d}/src/main/resources/application.yaml'
    drop_block(y, '  # Module 16.4: finish in-flight', 'spring:')
    drop_block(y, '  lifecycle:', 'management:')
    drop_block(y, '  endpoint:', '  metrics:')
    drop_block(d + '/pom.xml', '      <!-- Module 16.4: a container image', '    </plugins>')

def s05(d):
    shutil.copy(f'{BUILD}/variants/plain/ReviewsClient.java', f'{d}/{PKG}/ReviewsClient.java')
    p = f'{d}/{PKG}/ErrorAdvice.java'; s = open(p).read()
    i = s.index('    @GraphQlExceptionHandler\n    GraphQLError unavailable'); j = s.index('    @GraphQlExceptionHandler\n    GraphQLError badInput')
    open(p, 'w').write(s[:i] + s[j:])
    for a in ('resilience4j-circuitbreaker', 'resilience4j-bulkhead'): drop_dep(f'{d}/pom.xml', a)

def s06(d):
    shutil.copy(f'{STUBS}/06/CostMetric.java', f'{d}/{PKG}/CostMetric.java')

ALL = {'01-observability': s01, '02-field-usage': s02, '03-governance': s03, '04-deployment': s04,
       '05-reliability': s05, '06-capacity': s06}
for name in (sys.argv[1:] or ALL):
    lab(name, ALL[name])
