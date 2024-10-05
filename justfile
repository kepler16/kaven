default:
    @just --list

build *args:
    clojure -X:build build {{ args }}

release *args:
    clojure -X:build release {{ args }}

test *args:
    clojure -M:test
