# My attacks: functions named attack_M<nn>, each printing a title and returning 0 when blocked, 1 when the attack
# got through. Helpers from contract/redteam/attacks.sh: as <user>, raw <query>, mcp_call, mcp_text, approve <id>,
# refused <result>, members <club>, reviews. Shelfie's data is reset after each attack.
#
# attack_M01() { echo "what it tries"
#   as u4; refused "$(raw '{ ... }')"; }
