# Lab apps that verify course tokens offline: drop issuer-uri (Boot 4.2 fetches its discovery document even
# with public-key-location) and check "iss" with a JwtIssuerValidator bean instead.
import re, subprocess, sys
apps = [l.rsplit('/src/main/resources/application.yaml', 1)[0] for l in subprocess.run(
    "grep -rl public-key-location --include=application.yaml modules | grep -v target | xargs grep -l 'issuer-uri: http'",
    shell=True, capture_output=True, text=True).stdout.split()]
if len(sys.argv) > 1: apps = [a for a in apps if a in sys.argv[1:]]
for d in apps:
    y = d + '/src/main/resources/application.yaml'; s = open(y).read()
    s = re.sub(r'\n( *)# issuer-uri is still checked against "iss" \(Spring does not fetch it when a key is given\)\.', '', s)
    s, n = re.subn(r'\n( *)issuer-uri: http://keycloak:8180/realms/shelfie', r'\n\1# "iss" is checked by SecurityConfig.courseIssuer(): issuer-uri here would make Spring Boot 4.2\n\1# fetch Keycloak\'s discovery document, even with the public key above.', s)
    assert n == 1, y
    open(y, 'w').write(s)
    j = subprocess.run(f"grep -rl 'class SecurityConfig' {d}/src/main/java", shell=True, capture_output=True, text=True).stdout.split()[0]
    s = open(j).read()
    for imp in ['org.springframework.security.oauth2.core.OAuth2TokenValidator', 'org.springframework.security.oauth2.jwt.Jwt',
                'org.springframework.security.oauth2.jwt.JwtIssuerValidator']:
        if f'import {imp};' not in s:
            s = s.replace('import org.springframework.context.annotation.Configuration;', f'import org.springframework.context.annotation.Configuration;\nimport {imp};', 1)
    assert 'courseIssuer' not in s
    i = s.index('public class SecurityConfig'); k = s.index('{', i) + 1
    s = s[:k] + '''
  /**
   * Course tokens carry Keycloak's issuer. Check it here, offline: with issuer-uri in application.yaml, Spring
   * Boot 4.2 would fetch Keycloak's discovery document even though the public key is given. Boot adds this
   * validator to the decoder it builds from the key, next to the expiry and audience checks.
   */
  @Bean
  OAuth2TokenValidator<Jwt> courseIssuer() {
    return new JwtIssuerValidator("http://keycloak:8180/realms/shelfie");
  }
''' + s[k:]
    open(j, 'w').write(s)
    print('fixed', d)
