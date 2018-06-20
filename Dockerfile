FROM docker-registry.tain.com/azul/zulu-openjdk:8u172
COPY target/memeater.jar /app.jar
COPY docker-entry.sh /docker-entry.sh
RUN chmod +x /docker-entry.sh
ENTRYPOINT ["/docker-entry.sh"]
