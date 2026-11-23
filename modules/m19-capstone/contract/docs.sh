# Written-work checks for the capstone (source after testlib.sh). Words are counted outside headings, code
# blocks, table rules and HTML comments, so a template's prompts and headings are not answers.
words() { perl -0pe 's/<!--.*?-->//gs' "$1" 2>/dev/null | awk '/^```/{c=!c; next} !c' | grep -vE '^\s*(#|$)|^\s*\|[-:| ]+\|\s*$' | tr '|' ' ' | wc -w; }

doc_check() { # manifest key, min words, label, [heading regexes...]
  local f; f=$(m ".docs.$1")
  [[ -n $f && -s $LAB_DIR/$f ]] || { fail "$3: capstone.json docs.$1 must point at a file"; return 1; }
  local w; w=$(words "$LAB_DIR/$f")
  (( w >= $2 )) && pass "$f: $w words" || fail "$f: at least $2 words of your own ($w now)"
  local missing=() re
  for re in "${@:4}"; do grep -qiE "^#+ .*($re)" "$LAB_DIR/$f" || missing+=("$re"); done
  (( ${#missing[@]} == 0 )) || fail "$f: a heading for each of: ${missing[*]}"
}

adr_check() {
  local d; d=$(m .docs.adrs)
  [[ -n $d && -d $LAB_DIR/$d ]] || { fail "capstone.json docs.adrs must point at a directory of ADRs"; return; }
  local good=0 f
  for f in "$LAB_DIR/$d"/*.md; do
    [[ -f $f ]] || continue
    grep -qiE '^#+ .*context' "$f" && grep -qiE '^#+ .*decision' "$f" && grep -qiE '^#+ .*consequence' "$f" \
      && grep -qiE '(^#+ .*status|^status:|\*\*status)' "$f" && (( $(words "$f") >= 120 )) && good=$((good + 1))
  done
  (( good >= 3 )) && pass "$d: $good ADRs with status, context, decision and consequences" \
    || fail "$d: at least 3 ADRs, each with Status, Context, Decision and Consequences and 120+ words ($good now)"
}
