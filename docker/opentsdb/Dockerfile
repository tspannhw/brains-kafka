# Builds a docker image for OpenTSDB.
#
# Usage:
#   docker build -t jeffsvds/opentsdb -f Dockerfile-opentsdb .
#   docker push jeffsvds/opentsdb

FROM petergrace/opentsdb-docker

COPY opentsdb.conf /etc/
COPY opentsdb.sh /etc/services.d/
RUN chmod +x /etc/services.d/opentsdb.sh

EXPOSE 4242 16010 60000 60010 60030

ENTRYPOINT ["/etc/services.d/opentsdb.sh"]
