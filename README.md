# TerrainMapGenerator
Randomly generates gridded maps.

## Build

The repository uses the checked-in Gradle wrapper and a Java 21 toolchain:

```powershell
.\gradlew.bat testAll
```

Launch the legacy viewer:

```powershell
.\gradlew.bat :render:run --args="show-gui --viewer korge"
```

Launch the Compose Desktop viewer:

```powershell
.\gradlew.bat :render:run --args="show-gui --viewer compose"
```

Build the self-contained portable Windows archive:

```powershell
.\gradlew.bat :render:portableZip
```

The UI and renderer choice is documented in
[ADR 0001](docs/adr/0001-compose-map-viewer.md).
