#!/bin/sh
# Alternativa sin NetBeans: compila y ejecuta (requiere JDK instalado)
mkdir -p out
# --release 21: el JDK de este equipo compila con 27 y ejecuta con 25;
# sin --release el .class no arranca con el java del sistema.
javac --release 21 -encoding UTF-8 -d out src/edo/*.java && java -cp out edo.Main
