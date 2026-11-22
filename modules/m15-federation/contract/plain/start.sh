#!/usr/bin/env bash
# Start the two plain services in the background (inside the course image): catalog on :4101, reviews on :4102.
source /course/tools/testlib.sh
cd "$(dirname "${BASH_SOURCE[0]}")"
java -cp "$(lib_cp)" PlainService.java catalog 4101 > /tmp/plain-catalog.log 2>&1 &
java -cp "$(lib_cp)" PlainService.java reviews 4102 > /tmp/plain-reviews.log 2>&1 &
for p in 4101 4102; do for _ in $(seq 60); do curl -fs -o /dev/null localhost:$p/_stats && break; sleep 0.5; done; done
echo "catalog on :4101, reviews on :4102 (logs in /tmp/plain-*.log)"
