# Lab 10.8: graphql-cop 1.15's four HIGH findings, the way graphql-cop decides them
# (lib/tests/*.py in its repository), so the test needs no scanner image.

# K01 Alias Overloading: 101 aliases of __typename must not be answered
POST {{url}}
Content-Type: application/json
{"query": "query cop { alias0:__typename alias1:__typename alias2:__typename alias3:__typename alias4:__typename alias5:__typename alias6:__typename alias7:__typename alias8:__typename alias9:__typename alias10:__typename alias11:__typename alias12:__typename alias13:__typename alias14:__typename alias15:__typename alias16:__typename alias17:__typename alias18:__typename alias19:__typename alias20:__typename alias21:__typename alias22:__typename alias23:__typename alias24:__typename alias25:__typename alias26:__typename alias27:__typename alias28:__typename alias29:__typename alias30:__typename alias31:__typename alias32:__typename alias33:__typename alias34:__typename alias35:__typename alias36:__typename alias37:__typename alias38:__typename alias39:__typename alias40:__typename alias41:__typename alias42:__typename alias43:__typename alias44:__typename alias45:__typename alias46:__typename alias47:__typename alias48:__typename alias49:__typename alias50:__typename alias51:__typename alias52:__typename alias53:__typename alias54:__typename alias55:__typename alias56:__typename alias57:__typename alias58:__typename alias59:__typename alias60:__typename alias61:__typename alias62:__typename alias63:__typename alias64:__typename alias65:__typename alias66:__typename alias67:__typename alias68:__typename alias69:__typename alias70:__typename alias71:__typename alias72:__typename alias73:__typename alias74:__typename alias75:__typename alias76:__typename alias77:__typename alias78:__typename alias79:__typename alias80:__typename alias81:__typename alias82:__typename alias83:__typename alias84:__typename alias85:__typename alias86:__typename alias87:__typename alias88:__typename alias89:__typename alias90:__typename alias91:__typename alias92:__typename alias93:__typename alias94:__typename alias95:__typename alias96:__typename alias97:__typename alias98:__typename alias99:__typename alias100:__typename }"}
HTTP *
[Asserts]
jsonpath "$.data.alias100" not exists

# K02 Directive Overloading: ten duplicated directives must not produce ten errors
POST {{url}}
Content-Type: application/json
{"query": "query cop { __typename @aa@aa@aa@aa@aa@aa@aa@aa@aa@aa }"}
HTTP *
[Asserts]
jsonpath "$.errors" count < 10

# K03 Field Duplication: 500 copies of __typename must not be answered
POST {{url}}
Content-Type: application/json
file,dup500.json;
HTTP *
[Asserts]
jsonpath "$.data.__typename" not exists

# K04 Introspection
POST {{url}}
Content-Type: application/json
{"query": "query cop { __schema { types { name fields { name } } } }"}
HTTP *
[Asserts]
jsonpath "$.data.__schema" not exists
