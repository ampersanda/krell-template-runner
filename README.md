# Krell Project Generator

Scaffold a [Krell](https://github.com/vouch-opensource/krell) (ClojureScript + React Native) project using [Babashka](https://github.com/borkdude/babashka).

## Prerequisites

- [Node.js](https://nodejs.org/)
- [Clojure CLI](https://clojure.org/guides/install_clojure)
- [Babashka](https://github.com/babashka/babashka#installation) (installed automatically if missing)
- [CocoaPods](https://cocoapods.org/) (required for iOS)

## Quick Start

Replace `AwesomeProject` with your project name (CamelCase):

```sh
curl -s https://raw.githubusercontent.com/ampersanda/krell-template-runner/master/krell.sh | bash -s AwesomeProject
```

## Options

```
-p, --package PACKAGE_NAME   Organization package name (e.g. com.example.krell)
-v, --version VERSION        React Native version (e.g. 0.72.0)
-h, --help                   Show help
```

Example with options:

```sh
curl -s https://raw.githubusercontent.com/ampersanda/krell-template-runner/master/krell.sh | bash -s AwesomeProject --package com.example.krell
```

## What It Does

1. Creates a React Native project via `@react-native-community/cli`
2. Installs Node dependencies
3. Adds Krell and Reagent ClojureScript dependencies (`deps.edn`, `build.edn`)
4. Installs Clojure dependencies
5. Runs `pod install` for iOS (if CocoaPods is available)
6. Generates a starter ClojureScript namespace with a Reagent component

## After Setup

```sh
cd AwesomeProject
clj -M -m krell.main -co build.edn -c -r
```

In separate terminal tabs:

```sh
npx react-native start
```

```sh
# iOS
npx react-native run-ios

# Android
npx react-native run-android
```

Production build:

```sh
clj -M -m krell.main -v -co build.edn -O advanced -c
```

## Documentation

- [Krell Reagent Tutorial](https://github.com/vouch-opensource/krell/wiki/Reagent-Tutorial#using-the-repl)
- [Krell](https://github.com/vouch-opensource/krell)
- [Babashka](https://github.com/babashka/babashka)
