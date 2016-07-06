FROM grafana/grafana

COPY grafana.db /var/lib/grafana/
RUN chown -R grafana:grafana /var/lib/grafana/*

ENTRYPOINT ["/usr/sbin/grafana-server", "--homepath=/usr/share/grafana", "--config=/etc/grafana/grafana.ini", "cfg:default.paths.data=/var/lib/grafana", "cfg:default.paths.logs=/var/log/grafana", "cfg:default.paths.plugins=/var/lib/grafana/plugins"]
