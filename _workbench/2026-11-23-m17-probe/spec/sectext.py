"""Print the text of one spec section (without its subsections): python3 sectext.py <section number> [chars]"""
import re, html, sys
s = open(__file__.rsplit('/', 1)[0] + '/September2025.html').read()
num, limit = sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 1500
m = re.search(r'<section id="[^"]+"[^>]*secid="' + re.escape(num) + r'"[^>]*>(.*?)(?=<section |</section>)', s, re.S)
t = html.unescape(re.sub(r'<[^>]+>', ' ', m.group(1)))
print(re.sub(r'\s+', ' ', t).strip()[:limit])
