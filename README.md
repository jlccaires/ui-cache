# ui-cache
Android/Kotlin cache for app's ui responisveness

## Requisitos

Versão Android: Android 4.x+;

Incluir corretamente a url do repositório da lib nos repositories do build.gradle, conforme o guia Instalação;

Importar corretamente as dependências no .gradle, conforme o guia Instalação;

## Permissões no AndroidManifest.xml :

AndroidManifest.xml
 Copy
 ```
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```
## Instalação
Via gradle:

build.gradle do projeto
 Copy
```
repositories {

  mavenCentral()

  maven {
    url "https://jitpack.io"
  }

  maven {
    url "https://github.com/mobthink/ui-cache/raw/master/repositorio"
  }

  jcenter()
}
```

```
repositories {

  mavenCentral()

  maven {
    url "https://jitpack.io"
  }

  maven {
    url "https://github.com/mobthink/ui-cache/raw/master/repositorio"
  }

  jcenter()
}
```

build.gradle do modulo
 Copy
 ```
dependencies {
...
  compile 'io.mobthink:ui-cache:0.0.1'
...
}
```
```
dependencies {
...
  compile 'io.mobthink:ui-cache:0.0.1'
...
}
```
