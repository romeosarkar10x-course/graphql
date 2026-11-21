# Hints, game 6b

1. Run the JVM builds first and make sure lab 6.1 and 6.8 pass: native-image will not fix a broken app,
   it will only take longer to fail.
2. Spring needs its `native` profile (inherited from `spring-boot-starter-parent`): `-Pnative
   native:compile`. Quarkus needs `-Dquarkus.native.enabled=true`; `-Dquarkus.package.jar.enabled=false`
   skips the JVM jar.
3. A native binary that starts but fails a query usually lacks reflection metadata for a class only
   reached by reflection: Spring's `@RegisterReflectionForBinding`, Quarkus's `@RegisterForReflection`.
