# imagem com clojure + openjdk
FROM clojure:temurin-21-tools-deps

WORKDIR /app

# Copia só deps primeiro pra cachear dependências
COPY deps.edn /app/deps.edn

# baixa deps (cache)
RUN clojure -P -M:dev || true

# agora copia o código
COPY src /app/src

EXPOSE 3000

# roda sua API
CMD ["clojure", "-M:dev"]
