<div align="center">
  <a href="https://code-intelligence.com"><img src="https://www.code-intelligence.com/hubfs/Logos/CI%20Logos/Jazzer_einfach.png" height=150px alt="Jazzer by Code Intelligence">
</a>
  <h1>Jazzer</h1>
  <p>Fuzz Testing for the JVM</p>
  <a href="https://github.com/CodeIntelligenceTesting/jazzer/releases">
    <img src="https://img.shields.io/github/v/release/CodeIntelligenceTesting/jazzer" alt="Releases">
  </a>
  <a href="https://search.maven.org/search?q=g:com.code-intelligence%20a:jazzer">
    <img src="https://img.shields.io/maven-central/v/com.code-intelligence/jazzer" alt="Maven Central">
  </a>
  <a href="https://github.com/CodeIntelligenceTesting/jazzer/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/CodeIntelligenceTesting/jazzer" alt="License">
  </a>

  <br />

<a href="https://www.code-intelligence.com/" target="_blank">Website</a>
|
<a href="https://www.code-intelligence.com/blog" target="_blank">Blog</a>
|
<a href="https://twitter.com/CI_Fuzz" target="_blank">Twitter</a>
</div>

Jazzer is a coverage-guided, in-process fuzzer for the JVM platform developed by [Code Intelligence](https://code-intelligence.com).
It is based on [libFuzzer](https://llvm.org/docs/LibFuzzer.html) and brings many of its instrumentation-powered mutation features to the JVM.

Jazzer currently supports the following platforms:
* Linux x86_64 & arm64 (aarch64)
* macOS 12+ x86_64 & arm64
