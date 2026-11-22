source /course/tools/testlib.sh; source /course/modules/m06-spring/contract/app-test.sh; source /course/modules/m15-federation/contract/fed.sh
cd /course/_workbench/2026-11-22-m15-probe/lab04-variants/v && mvn -q package -DskipTests 2>&1 | grep ERROR | head -3
export LAB_HOME=/course/modules/m15-federation/labs/04-java-subgraphs LAB_DIR=$PWD
bash $LAB_HOME/tests/run.sh 2>&1 | grep -v "^\s*$" | tail -12
