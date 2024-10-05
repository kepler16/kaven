default:
    @just --list

build *args:
    clojure -T:build build {{ args }}

release *args:
    clojure -X:build release {{ args }}

test *args:
    clojure -M:test
