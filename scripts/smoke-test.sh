#!/usr/bin/env bash
# Proba de extremo a extremo da API. Require a aplicación arrancada (./gradlew bootRun), curl e jq.
# Uso: ./scripts/smoke-test.sh [URL_BASE]
set -u
B=${1:-http://localhost:18080}
PASS=0; FAIL=0
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

# check <descrición> <código esperado> <curl args...>  → garda o corpo en $TMP/body e as cabeceiras en $TMP/headers
check() {
  local desc=$1 expected=$2; shift 2
  local code
  code=$(curl -s -o "$TMP/body" -D "$TMP/headers" -w '%{http_code}' "$@")
  if [[ "$code" == "$expected" ]]; then PASS=$((PASS+1)); printf '  ok   %s %s\n' "$code" "$desc"
  else FAIL=$((FAIL+1)); printf '  FAIL %s (esperado %s) %s\n' "$code" "$expected" "$desc"; head -c 400 "$TMP/body"; echo; fi
}
body() { cat "$TMP/body"; }
header() { grep -i "^$1:" "$TMP/headers" | head -1 | cut -d' ' -f2- | tr -d '\r'; }
login() {
  curl -s -D - -o /dev/null -X POST "$B/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" -c "$TMP/cookies-$1" | grep -i '^authorization' | sed 's/.*Bearer //' | tr -d '\r'
}
JSON=(-H 'Content-Type: application/json')
PATCH=(-H 'Content-Type: application/json-patch+json')

echo "== Autenticación"
check "sen token → 401" 401 "$B/benches"
check "credenciais erróneas → 401" 401 -X POST "$B/auth/login" "${JSON[@]}" -d '{"username":"user","password":"mal"}'
NEW="probas$RANDOM"
check "rexistro con datos non válidos → 400" 400 -X POST "$B/auth/register" "${JSON[@]}" -d '{"userID":"x","password":"1"}'
check "rexistro → 201" 201 -X POST "$B/auth/register" "${JSON[@]}" -d "{\"userID\":\"$NEW\",\"name\":\"Probas\",\"password\":\"segredo\"}"
[[ "$(body | jq -r '.password // "oculto"')" == "oculto" ]] && echo "  ok   o rexistro non devolve o contrasinal" || { echo "  FAIL o rexistro devolve o contrasinal"; FAIL=$((FAIL+1)); }
check "rexistro duplicado → 409" 409 -X POST "$B/auth/register" "${JSON[@]}" -d "{\"userID\":\"$NEW\",\"password\":\"segredo\"}"
U=$(login user 1234); A=$(login admin 1234); N=$(login "$NEW" segredo)
[[ -n "$U" && -n "$A" && -n "$N" ]] && echo "  ok   login de user, admin e $NEW" || { echo "  FAIL login"; FAIL=$((FAIL+1)); }
AU=(-H "Authorization: Bearer $U"); AA=(-H "Authorization: Bearer $A"); AN=(-H "Authorization: Bearer $N")
check "refresh coa cookie (sen JWT) → 204" 204 -X POST "$B/auth/refresh" -b "$TMP/cookies-user"
[[ -n "$(header authorization)" ]] && echo "  ok   o refresh devolve un JWT novo" || { echo "  FAIL refresh sen JWT"; FAIL=$((FAIL+1)); }
check "refresh sen cookie → 400" 400 -X POST "$B/auth/refresh"

echo "== Bancos"
check "listar (HAL paxinado)" 200 "${AU[@]}" "$B/benches?size=5&sort=averageMark,desc"
[[ "$(body | jq '.page.totalElements')" -ge 20 && "$(body | jq -r '._links.next.href')" != null ]] && echo "  ok   metadatos e ligazóns de paxinación" || { echo "  FAIL paxinación"; FAIL=$((FAIL+1)); }
check "busca por texto" 200 "${AU[@]}" "$B/benches?q=carballeira"
[[ "$(body | jq '.page.totalElements')" == 2 ]] && echo "  ok   q=carballeira → 2 resultados" || { echo "  FAIL busca texto: $(body | jq '.page.totalElements')"; FAIL=$((FAIL+1)); }
check "busca xeográfica + filtros" 200 "${AU[@]}" "$B/benches?lat=42.8805&lon=-8.5456&radius=0.5&covered=false&minMark=5"
check "busca xeográfica incompleta → 400" 400 "${AU[@]}" "$B/benches?lat=42.88"
check "ordenación por campo inexistente → 400" 400 "${AU[@]}" "$B/benches?sort=foo"
check "minMark fóra de rango → 400" 400 "${AU[@]}" "$B/benches?minMark=20"
check "obter banco" 200 "${AU[@]}" "$B/benches/1"
ETAG=$(header etag)
check "If-None-Match → 304" 304 "${AU[@]}" -H "If-None-Match: $ETAG" "$B/benches/1"
check "banco inexistente → 404" 404 "${AU[@]}" "$B/benches/9999"
check "crear banco sen nome → 400" 400 -X POST "${AU[@]}" "${JSON[@]}" "$B/benches" -d '{"latitude":42.88,"longitude":-8.54,"type":"Wood"}'
check "crear banco → 201" 201 -X POST "${AN[@]}" "${JSON[@]}" "$B/benches" -d '{"name":"Banco de probas","latitude":42.8800,"longitude":-8.5450,"type":"Wooden","covered":false,"averageMark":10,"owner":"admin"}'
BID=$(body | jq '.benchID'); LOC=$(header location)
[[ "$LOC" == *"/benches/$BID" && "$(body | jq -r '.owner')" == "$NEW" && "$(body | jq '.averageMark')" == null ]] && echo "  ok   Location, propietario e campos de só lectura" || { echo "  FAIL creación: $LOC $(body | jq -c '{owner,averageMark}')"; FAIL=$((FAIL+1)); }
check "JSON-Patch do propietario → 200" 200 -X PATCH "${AN[@]}" "${PATCH[@]}" "$B/benches/$BID" -d '[{"op":"replace","path":"/name","value":"Banco renomeado"},{"op":"replace","path":"/covered","value":true}]'
[[ "$(body | jq -r '.name')" == "Banco renomeado" ]] && echo "  ok   patch aplicado" || { echo "  FAIL patch"; FAIL=$((FAIL+1)); }
check "JSON-Patch con valor non válido → 400" 400 -X PATCH "${AN[@]}" "${PATCH[@]}" "$B/benches/$BID" -d '[{"op":"replace","path":"/latitude","value":200}]'
check "JSON-Patch con tipo incorrecto → 422" 422 -X PATCH "${AN[@]}" "${PATCH[@]}" "$B/benches/$BID" -d '[{"op":"replace","path":"/latitude","value":"norte"}]'
check "JSON-Patch con ruta inexistente → 422" 422 -X PATCH "${AN[@]}" "${PATCH[@]}" "$B/benches/$BID" -d '[{"op":"replace","path":"/nonexistent","value":1}]'
check "JSON-Patch con ruta baleira → 422" 422 -X PATCH "${AN[@]}" "${PATCH[@]}" "$B/benches/$BID" -d '[{"op":"remove","path":""}]'
check "JSON-Patch doutro usuario → 403" 403 -X PATCH "${AU[@]}" "${PATCH[@]}" "$B/benches/$BID" -d '[{"op":"replace","path":"/name","value":"x"}]'
check "Content-Type non soportado → 415" 415 -X PATCH "${AN[@]}" "${JSON[@]}" "$B/benches/$BID" -d '{}'

echo "== Sombra (Open-Meteo + posición solar)"
check "predición de sombra" 200 "${AU[@]}" "$B/benches/1/shade?from=6&to=22"
echo "       fonte=$(body | jq -r .source) sombra=$(body | jq .shadedHours)h sol=$(body | jq .sunnyHours)h"
[[ "$(body | jq -r '.hours[0].reason')" == NIGHT ]] && echo "  ok   ás 6:00 en novembro é de noite" || { echo "  FAIL clasificación nocturna"; FAIL=$((FAIL+1)); }
check "banco cuberto → sempre sombra" 200 "${AU[@]}" "$B/benches/4/shade"
[[ "$(body | jq '.sunnyHours')" == 0 ]] && echo "  ok   0 horas de sol" || { echo "  FAIL banco cuberto"; FAIL=$((FAIL+1)); }
check "data lonxana → só astronómico" 200 "${AU[@]}" "$B/benches/1/shade?date=2027-06-21&from=12&to=14"
[[ "$(body | jq -r '.source')" == ASTRONOMICAL ]] && echo "  ok   source=ASTRONOMICAL" || { echo "  FAIL source"; FAIL=$((FAIL+1)); }
check "hora non válida → 400" 400 "${AU[@]}" "$B/benches/1/shade?from=30"
check "intervalo invertido → 400" 400 "${AU[@]}" "$B/benches/1/shade?from=18&to=10"

echo "== Valoracións (eventos asíncronos)"
check "valorar → 201" 201 -X POST "${AN[@]}" "${JSON[@]}" "$B/benches/$BID/reviews" -d '{"mark":7,"note":"Ben","author":"admin"}'
RID=$(body | jq '.reviewID')
[[ "$(body | jq -r '.author')" == "$NEW" ]] && echo "  ok   o autor é o usuario autenticado" || { echo "  FAIL autor"; FAIL=$((FAIL+1)); }
check "valorar de novo → 409 con Location" 409 -X POST "${AN[@]}" "${JSON[@]}" "$B/benches/$BID/reviews" -d '{"mark":3}'
[[ "$(header location)" == *"/reviews/$RID" ]] && echo "  ok   Location apunta á valoración existente" || { echo "  FAIL location 409"; FAIL=$((FAIL+1)); }
check "nota fóra de rango → 400" 400 -X POST "${AU[@]}" "${JSON[@]}" "$B/benches/$BID/reviews" -d '{"mark":11}'
check "valorar outro usuario → 201" 201 -X POST "${AU[@]}" "${JSON[@]}" "$B/benches/$BID/reviews" -d '{"mark":10}'
sleep 1
check "media recalculada" 200 "${AU[@]}" "$B/benches/$BID"
[[ "$(body | jq '.averageMark')" == 8.5 && "$(body | jq '.reviewCount')" == 2 ]] && echo "  ok   media 8.5 con 2 valoracións" || { echo "  FAIL media: $(body | jq -c '{averageMark,reviewCount}')"; FAIL=$((FAIL+1)); }
check "modificar valoración propia" 200 -X PATCH "${AN[@]}" "${PATCH[@]}" "$B/reviews/$RID" -d '[{"op":"replace","path":"/mark","value":9}]'
check "modificar valoración allea → 403" 403 -X PATCH "${AU[@]}" "${PATCH[@]}" "$B/reviews/$RID" -d '[{"op":"replace","path":"/mark","value":0}]'
check "valoracións do banco" 200 "${AU[@]}" "$B/benches/$BID/reviews"
check "valoracións dun usuario" 200 "${AU[@]}" "$B/users/user/reviews"
check "últimas valoracións" 200 "${AU[@]}" "$B/reviews?size=3"

echo "== Comentarios"
check "comentar → 201" 201 -X POST "${AU[@]}" "${JSON[@]}" "$B/benches/$BID/comments" -d '{"text":"Que tal está?"}'
CID=$(body | jq '.commentID')
check "comentario baleiro → 400" 400 -X POST "${AU[@]}" "${JSON[@]}" "$B/benches/$BID/comments" -d '{"text":"  "}'
check "editar comentario propio" 200 -X PATCH "${AU[@]}" "${PATCH[@]}" "$B/comments/$CID" -d '[{"op":"replace","path":"/text","value":"Editado"}]'
check "borrar comentario alleo → 403" 403 -X DELETE "${AN[@]}" "$B/comments/$CID"
check "admin borra comentario → 204" 204 -X DELETE "${AA[@]}" "$B/comments/$CID"
check "comentarios do banco" 200 "${AU[@]}" "$B/benches/1/comments"

echo "== Fotos"
IMG=$(dirname "$0")/../src/main/resources/static/img/img1.jpg
check "subir foto → 201" 201 -X POST "${AN[@]}" -F "file=@$IMG;type=image/jpeg" "$B/benches/$BID/photos"
PID=$(body | jq '.photoID')
python3 -c "import struct,zlib;d=lambda t,b:struct.pack('>I',len(b))+t+b+struct.pack('>I',zlib.crc32(t+b));open('$TMP/bomb.png','wb').write(b'\x89PNG\r\n\x1a\n'+d(b'IHDR',struct.pack('>IIBBBBB',30000,30000,8,2,0,0,0))+d(b'IDAT',zlib.compress(b''))+d(b'IEND',b''))"
check "imaxe con dimensións enormes → 415" 415 -X POST "${AN[@]}" -F "file=@$TMP/bomb.png;type=image/png" "$B/benches/$BID/photos"
check "subir ficheiro que non é imaxe → 415" 415 -X POST "${AN[@]}" -F "file=@$0;type=image/jpeg" "$B/benches/$BID/photos"
check "fotos do banco" 200 "${AU[@]}" "$B/benches/$BID/photos"
check "descargar imaxe sen token (pública)" 200 "$B/photos/$PID/content"
[[ "$(header content-type)" == image/jpeg* && -n "$(header cache-control)" ]] && echo "  ok   Content-Type e Cache-Control" || { echo "  FAIL cabeceiras imaxe"; FAIL=$((FAIL+1)); }
sleep 1
check "miniatura xerada en segundo plano" 200 "${AU[@]}" "$B/photos/$PID"
[[ "$(body | jq '.thumbnailAvailable')" == true ]] && echo "  ok   thumbnailAvailable=true" || { echo "  FAIL miniatura"; FAIL=$((FAIL+1)); }
check "borrar foto allea → 403" 403 -X DELETE "${AU[@]}" "$B/photos/$PID"
check "borrar foto propia → 204" 204 -X DELETE "${AN[@]}" "$B/photos/$PID"

echo "== Usuarios, favoritos e likes"
for url in "$B/users/user" "$B/users" ; do
  curl -s "${AU[@]}" "$url" | grep -q '"password"' && { echo "  FAIL contrasinal visible en $url"; FAIL=$((FAIL+1)); } || echo "  ok   sen contrasinal en $url"
done
check "perfil público" 200 "${AU[@]}" "$B/users/user"
check "usuario inexistente → 404" 404 "${AU[@]}" "$B/users/ninguen"
check "listar usuarios v1 (paxinado)" 200 "${AU[@]}" -H 'API-Version: 1' "$B/users"
check "listar usuarios v0 (lista)" 200 "${AU[@]}" -H 'API-Version: 0' "$B/users"
[[ "$(body | jq 'type')" == '"array"' ]] && echo "  ok   v0 devolve un array" || { echo "  FAIL v0"; FAIL=$((FAIL+1)); }
check "versión inexistente → 400" 400 "${AU[@]}" -H 'API-Version: 7' "$B/users"
check "editar perfil propio (PATCH)" 200 -X PATCH "${AN[@]}" "${PATCH[@]}" "$B/users/$NEW" -d '[{"op":"replace","path":"/location","value":"Lugo"}]'
check "editar perfil alleo → 403" 403 -X PATCH "${AU[@]}" "${PATCH[@]}" "$B/users/admin" -d '[{"op":"replace","path":"/name","value":"x"}]'
check "borrar usuario alleo → 403" 403 -X DELETE "${AU[@]}" "$B/users/admin"
check "gardar favorito → 204" 204 -X PUT "${AN[@]}" "$B/users/$NEW/favorites/1"
check "gardar favorito de novo (idempotente) → 204" 204 -X PUT "${AN[@]}" "$B/users/$NEW/favorites/1"
check "favoritos" 200 "${AN[@]}" "$B/users/$NEW/favorites"
[[ "$(body | jq '.page.totalElements')" == 1 ]] && echo "  ok   1 favorito" || { echo "  FAIL favoritos"; FAIL=$((FAIL+1)); }
check "favorito de banco inexistente → 404" 404 -X PUT "${AN[@]}" "$B/users/$NEW/favorites/9999"
check "gardar favorito noutra conta → 403" 403 -X PUT "${AN[@]}" "$B/users/user/favorites/2"
LIKES=$(curl -s "${AU[@]}" "$B/benches/2" | jq '.likes')
check "like → 204" 204 -X PUT "${AN[@]}" "$B/users/$NEW/likes/2"
check "like repetido → 204" 204 -X PUT "${AN[@]}" "$B/users/$NEW/likes/2"
[[ "$(curl -s "${AU[@]}" "$B/benches/2" | jq '.likes')" == $((LIKES+1)) ]] && echo "  ok   o contador sobe só unha vez (caché invalidada)" || { echo "  FAIL contador likes"; FAIL=$((FAIL+1)); }
[[ "$(curl -s "${AU[@]}" "$B/benches/3" | jq '.likes')" == 0 ]] && echo "  ok   o like non afecta a outros bancos" || { echo "  FAIL likes doutro banco"; FAIL=$((FAIL+1)); }
check "quitar like → 204" 204 -X DELETE "${AN[@]}" "$B/users/$NEW/likes/2"

echo "== Borrados e cascada"
check "borrar banco alleo → 403" 403 -X DELETE "${AU[@]}" "$B/benches/$BID"
check "borrar banco propio → 204" 204 -X DELETE "${AN[@]}" "$B/benches/$BID"
check "valoración borrada en cascada → 404" 404 "${AU[@]}" "$B/reviews/$RID"
check "valorar o banco 3 antes de borrar a conta" 201 -X POST "${AN[@]}" "${JSON[@]}" "$B/benches/3/reviews" -d '{"mark":0}'
sleep 1; COUNT=$(curl -s "${AU[@]}" "$B/benches/3" | jq '.reviewCount')
check "borrar a propia conta → 204" 204 -X DELETE "${AN[@]}" "$B/users/$NEW"
sleep 1
[[ "$(curl -s "${AU[@]}" "$B/benches/3" | jq '.reviewCount')" == $((COUNT-1)) ]] && echo "  ok   media recalculada ao borrar o usuario" || { echo "  FAIL media tras borrar usuario"; FAIL=$((FAIL+1)); }
check "token dun usuario borrado → 401" 401 "${AN[@]}" "$B/benches"
check "logout → 204" 204 -X POST "${AU[@]}" "$B/auth/logout"
check "refresh tras logout → 401" 401 -X POST "$B/auth/refresh" -b "$TMP/cookies-user"
login ana 1234 >/dev/null
check "logout só coa cookie (JWT caducado) → 204" 204 -X POST "$B/auth/logout" -b "$TMP/cookies-ana"
check "refresh tras logout con cookie → 401" 401 -X POST "$B/auth/refresh" -b "$TMP/cookies-ana"

echo "== Documentación"
check "OpenAPI" 200 "$B/docs/oas"
check "Swagger UI" 200 -L "$B/docs/swagger"

echo
echo "Resultado: $PASS correctas, $FAIL fallidas"
[[ $FAIL -eq 0 ]]
