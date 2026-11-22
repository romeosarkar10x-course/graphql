# My attacks: functions named attack_M<nn>, each printing a title and returning 0 when blocked (see attacks.sh).
attack_M01() { echo "two readers in one header: X-Shelfie-User: u4, u1"
  mcp_init $MCP "X-Shelfie-User: u4, u1"; refused "$(mcp_call join_club '{"clubId":"c2"}')" && [[ $(members c2) == 4 ]]; }
attack_M02() { echo "a user id smuggled as an extra tool argument (the schema has additionalProperties: false)"
  as u6; refused "$(mcp_call join_club '{"clubId":"c2","userId":"u1"}')" && [[ $(members c2) == 4 ]]; }
